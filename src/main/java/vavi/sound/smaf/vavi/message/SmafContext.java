/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;

import static java.lang.System.getLogger;


/**
 * SMAF context for the converter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 *          0.01 260912 nsano midi to smaf <br>
 */
public class SmafContext implements SmafConvertible {

    private static final Logger logger = getLogger(SmafContext.class.getName());

    /** max SMAF track number */
    public static final int MAX_SMAF_TRACKS = 4;

    /** SMAF channels of one {@link vavi.sound.smaf.vavi.chunk.TrackChunk.FormatType#HandyPhoneStandard} track */
    public static final int MAX_SMAF_CHANNELS = 4;

    /**
     * Timebase_D and Timebase_G of the tracks written, in [msec].
     * 4 msec is what the Handy Phone Standard files out there use.
     * @see vavi.sound.smaf.vavi.chunk.TrackChunk#setDurationTimeBase(int)
     */
    public static final int TIME_BASE = 4;

    /** no tempo in a MIDI sequence means a quarter note = 120 */
    private static final int DEFAULT_TEMPO = 500_000;

    // ----

    /** @see MidiFileFormat#getType() 0: SMF Format 0, 1: SMF Format 1 */
    private int type;

    /** */
    public int getType() {
        return type;
    }

    /** */
    public void setType(int type) {
        this.type = type;
    }

    /** the resolution of the MIDI sequence, in ticks per quarter note */
    private int timeBase;

    /** */
    public int getTimeBase() {
        return timeBase;
    }

    /** */
    public void setTimeBase(int timeBase) {
        this.timeBase = timeBase;
    }

    // ----

    /** index is SMAF Track No., true if used */
    private final boolean[] trackUsed = new boolean[MAX_SMAF_TRACKS];

    /**
     * @param smafTrackNumber smaf track number
     */
    public void setTrackUsed(int smafTrackNumber, boolean trackUsed) {
        this.trackUsed[smafTrackNumber] = trackUsed;
    }

    /**
     * @param smafTrackNumber smaf track number
     */
    public boolean isTrackUsed(int smafTrackNumber) {
        return trackUsed[smafTrackNumber];
    }

    // ----

    /**
     * The tempo map of the MIDI sequence, the ticks the tempo changes at (ascending, the
     * first one is 0), what it changes to in [μsec/beat] and the time each change is at.
     */
    private long[] tempoTicks = {0};
    /** @see #tempoTicks */
    private int[] tempoValues = {DEFAULT_TEMPO};
    /** @see #tempoTicks */
    private long[] tempoMicroseconds = {0};

    /**
     * The SMAF duration step a MIDI tick is at, counted from the beginning of the sequence.
     * <p>
     * A Δ of HandyPhoneStandard is a real time, a multiple of Timebase_D, and there is no
     * tempo message, so the tempo map of the MIDI sequence has to be baked into the Δs. Every
     * Δ being the difference of two of these, a rounding error never accumulates.
     * </p>
     *
     * @see #TIME_BASE
     */
    public long retrieveSteps(long tick) {
        int i = Arrays.binarySearch(tempoTicks, tick);
        if (i < 0) {
            i = Math.max(-i - 2, 0); // the tempo before the tick
        }
        long microseconds = tempoMicroseconds[i] + (tick - tempoTicks[i]) * tempoValues[i] / timeBase;
        return Math.round((double) microseconds / (TIME_BASE * 1000));
    }

    /** the step the previous message of the track is at, index is SMAF Track No. */
    private final long[] beforeSteps = new long[MAX_SMAF_TRACKS];

    /* init */ {
        Arrays.fill(beforeSteps, 0);
    }

    /**
     * Remembers where the previous message of the track is.
     *
     * @param smafTrackNumber smaf track number
     * @param tick the MIDI tick of the message
     */
    public void setBeforeTick(int smafTrackNumber, long tick) {
        this.beforeSteps[smafTrackNumber] = retrieveSteps(tick);
    }

    /**
     * Shifts the previous message of the track forward, for the Δ a Nop message has eaten.
     *
     * @param smafTrackNumber smaf track number
     */
    public void incrementBeforeStep(int smafTrackNumber, long steps) {
        this.beforeSteps[smafTrackNumber] += steps;
    }

    /**
     * @param smafTrackNumber smaf track number
     * @return the Δ between the previous message of the track and {@code tick}
     */
    public int retrieveDelta(int smafTrackNumber, long tick) {
        return (int) (retrieveSteps(tick) - beforeSteps[smafTrackNumber]);
    }

    // ----

    /**
     * Finds how many Δs of the largest size a message can tell fit in the time since the previous
     * message of the track, and returns an array of NopMessages to be inserted for that number.
     * <pre>
     *     event    index    process
     *   |
     * --+- NoteOn    -2    -> brforeTick
     * ↑ |
     * ｜|
     * Δ |- NoteOff    -1    -> noteOffEventUsed[-1] = true
     * ｜|
     * ↓ |
     * --+-
     *   |
     *  -O- NoteOn    midiEventIndex
     *   |
     *   |
     *   |
     * --+-
     * </pre>
     * in the above figure, one NopMessage is inserted.
     *
     * @param smafTrackNumber the SMAF track the current MIDI event is going to
     * @return null when nothing is to be inserted
     */
    public SmafEvent[] getIntervalSmafEvents(int smafTrackNumber) {
        return getIntervalSmafEvents(smafTrackNumber, midiEvents.get(midiEventIndex).getTick());
    }

    /**
     * Nop messages for the Δ which does not fit in one message, as
     * {@link #getIntervalSmafEvents(int)} but for any tick.
     *
     * @param smafTrackNumber the SMAF track the message is going to
     * @param currentTick the MIDI tick the message is at
     * @return null when nothing is to be inserted
     */
    public SmafEvent[] getIntervalSmafEvents(int smafTrackNumber, long currentTick) {

        int interval = retrieveDelta(smafTrackNumber, currentTick);
if (interval < 0) {
 // it shouldn't be possible
 logger.log(Level.WARNING, "interval: " + interval + "[" + smafTrackNumber + "] @" + currentTick);
 interval = 0;
}
        int nopLength = interval / HandyPhoneStandard.maxSteps;
        if (nopLength == 0) {
            return null;
        }
        SmafEvent[] smafEvents = new SmafEvent[nopLength];
        for (int i = 0; i < nopLength; i++) {
            NopMessage smafMessage = new NopMessage(HandyPhoneStandard.maxSteps);
            smafEvents[i] = new SmafEvent(smafMessage, 0L);    // TODO 0l
            // shift forward by the Δ the Nop has eaten
            incrementBeforeStep(smafTrackNumber, HandyPhoneStandard.maxSteps);
        }

logger.log(Level.DEBUG, nopLength + " nops inserted[" + smafTrackNumber + "]");
        return smafEvents;
    }

    /**
     * Gets the Δ (time) since the previous data (MIDI NoteOn) was executed.
     * Be sure to execute #getIntervalSmafEvents(int) in advance to return Δ less than
     * {@link HandyPhoneStandard#maxSteps}.
     *
     * @param smafTrackNumber the SMAF track the current MIDI event is going to
     */
    public int getDuration(int smafTrackNumber) {

        MidiEvent midiEvent = midiEvents.get(midiEventIndex);
        int delta = retrieveDelta(smafTrackNumber, midiEvent.getTick());

if (delta > HandyPhoneStandard.maxSteps) {
 // this is impossible because it should be handled by getIntervalSmafEvents
 logger.log(Level.WARNING, "Δ: " + delta + ", " + (delta % (HandyPhoneStandard.maxSteps + 1)));
}
        return delta % (HandyPhoneStandard.maxSteps + 1);
    }

    // ----

    /**
     * Gets the corrected SMAF Pitch.
     * <p>
     * A HandyPhoneStandard note is an octave 0 ~ 3 and a note 1 ~ 12 of it, which
     * {@link MidiContext#retrievePitch(int, int)} plays as the MIDI pitch 37 ~ 84.
     * </p>
     *
     * @param channel MIDI channel
     * @param pitch MIDI pitch
     * @return SMAF pitch 1 ~ 48
     */
    public int retrievePitch(int channel, int pitch) {
        int smafPitch = pitch - 36;
        if (smafPitch < 1) {
            // out of range, the lowest octave of the same note
            smafPitch = Math.floorMod(smafPitch - 1, 12) + 1;
        } else if (smafPitch > 48) {
            // out of range, the highest octave of the same note
            smafPitch = Math.floorMod(smafPitch - 1, 12) + 37;
        }
        return smafPitch;
    }

    /**
     * Gets SMAF Voice No.
     * @param channel MIDI channel
     */
    public int retrieveVoice(int channel) {
        return channel % MAX_SMAF_CHANNELS;
    }

    /**
     * Gets MIDI Channel.
     * @param voice SMAF channel
     */
    public int retrieveChannel(int voice) {
        return smafTrackNumber * MAX_SMAF_CHANNELS + voice;
    }

    /**
     * Gets SMAF Track.
     * @param channel MIDI channel
     */
    public int retrieveSmafTrack(int channel) {
        return channel / MAX_SMAF_CHANNELS;
    }

    /**
     * Whether the channel plays rhythm, which is the MIDI drum channel only.
     * @param channel MIDI channel
     * @see MidiContext#CHANNEL_DRUM
     */
    public boolean isPercussion(int channel) {
        return channel == MidiContext.CHANNEL_DRUM;
    }

    // ----

    /** current SMAF track No. */
    private int smafTrackNumber;

    /** Sets current SMAF track No. */
    public void setSmafTrackNumber(int smafTrackNumber) {
        this.smafTrackNumber = smafTrackNumber;
    }

    /** Gets current SMAF strack No. */
    public int getSmafTrackNumber() {
        return smafTrackNumber;
    }

    /** the MIDI events of all the MIDI tracks, ordered by tick */
    private final List<MidiEvent> midiEvents = new ArrayList<>();

    /**
     * Flattens the MIDI sequence into one event sequence, ordered by tick, and takes its
     * resolution and its tempo map.
     *
     * @see #retrieveSteps(long)
     */
    public void setMidiSequence(Sequence midiSequence) {

        this.timeBase = midiSequence.getResolution();
if (midiSequence.getDivisionType() != Sequence.PPQ) {
 logger.log(Level.WARNING, "not PPQ, the resolution is not ticks per beat: " + midiSequence.getDivisionType());
}

        SortedMap<Long, Integer> tempos = new TreeMap<>();
        for (Track midiTrack : midiSequence.getTracks()) {
            for (int i = 0; i < midiTrack.size(); i++) {
                MidiEvent midiEvent = midiTrack.get(i);
                MidiMessage midiMessage = midiEvent.getMessage();
                if (midiMessage instanceof MetaMessage metaMessage &&
                    metaMessage.getType() == MetaEvent.META_TEMPO.number()) {

                    byte[] data = metaMessage.getData();
                    tempos.put(midiEvent.getTick(),
                            (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | data[2] & 0xff);
                }
                midiEvents.add(midiEvent);
            }
        }
        setTempos(tempos);

        midiEvents.sort(Comparator
                .comparingLong(MidiEvent::getTick)
                .thenComparingInt(SmafContext::getChannel));

        this.noteOffEventUsed = new BitSet(midiEvents.size());
    }

    /**
     * @param tempos tick to [μsec/beat], the tempo before the first one is a quarter note = 120
     * @see #tempoTicks
     */
    private void setTempos(SortedMap<Long, Integer> tempos) {

        if (tempos.isEmpty() || tempos.firstKey() != 0) {
            tempos.put(0L, DEFAULT_TEMPO);
        }
logger.log(Level.DEBUG, "resolution: " + timeBase + ", tempos: " + tempos.size() + ", first: " +
        Math.round(60_000_000d / tempos.get(tempos.firstKey())) + " bpm");

        tempoTicks = new long[tempos.size()];
        tempoValues = new int[tempos.size()];
        tempoMicroseconds = new long[tempos.size()];

        int i = 0;
        for (Map.Entry<Long, Integer> tempo : tempos.entrySet()) {
            tempoTicks[i] = tempo.getKey();
            tempoValues[i] = tempo.getValue();
            if (i > 0) {
                tempoMicroseconds[i] = tempoMicroseconds[i - 1] +
                        (tempoTicks[i] - tempoTicks[i - 1]) * tempoValues[i - 1] / timeBase;
            }
            i++;
        }
    }

    /** @return -1 when the event is not a {@link ShortMessage} */
    private static int getChannel(MidiEvent midiEvent) {
        MidiMessage midiMessage = midiEvent.getMessage();
        return midiMessage instanceof ShortMessage shortMessage ? shortMessage.getChannel() : -1;
    }

    /** @return the number of the MIDI events of the whole sequence */
    public int getSequenceSize() {
        return midiEvents.size();
    }

    /**
     * @param midiEventIndex index into the flattened MIDI sequence
     * @return MIDI event, it becomes the current one
     */
    public MidiEvent getMidiEvent(int midiEventIndex) {
        this.midiEventIndex = midiEventIndex;
        return midiEvents.get(midiEventIndex);
    }

    /** index value of the current MIDI event */
    private int midiEventIndex;

    /** Sets the index value of the current MIDI event. */
    public void setMidiEventIndex(int midiEventIndex) {
        this.midiEventIndex = midiEventIndex;
    }

    /** Gets the index value of the current MIDI event. */
    int getMidiEventIndex() {
        return midiEventIndex;
    }

    /**
     * Gets the currently selected NoteOn event and its counterpart NoteOff event.
     * Use IllegalStateException only for bug traps.
     * @see NoteMessage
     *
     * @throws NoSuchElementException no paired NoteOff event
     * @throws IllegalStateException current event is not a ShortMessage
     */
    public MidiEvent getNoteOffMidiEvent() throws NoSuchElementException {

        ShortMessage shortMessage;

        MidiEvent midiEvent = midiEvents.get(midiEventIndex);
        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage) {
            shortMessage = (ShortMessage) midiMessage;
        } else {
            throw new IllegalStateException("current is not ShortMessage");
        }

        int channel = shortMessage.getChannel();
        int data1 = shortMessage.getData1();

        for (int i = midiEventIndex + 1; i < midiEvents.size(); i++) {
            midiEvent = midiEvents.get(i);
            midiMessage = midiEvent.getMessage();
            if (midiMessage instanceof ShortMessage) {
                shortMessage = (ShortMessage) midiMessage;
                if (shortMessage.getChannel() == channel &&
                    shortMessage.getData1() == data1 &&
                    isNoteOff(shortMessage) &&
                    !noteOffEventUsed.get(i)) {

                    noteOffEventUsed.set(i);    // consumption flag on
                    return midiEvent;
                }
            }
        }

        throw new NoSuchElementException(channel + "ch, " + data1);
    }

    /** A NoteOn with the velocity 0 is a NoteOff too. */
    static boolean isNoteOff(ShortMessage shortMessage) {
        return shortMessage.getCommand() == ShortMessage.NOTE_OFF ||
               (shortMessage.getCommand() == ShortMessage.NOTE_ON && shortMessage.getData2() == 0);
    }

    /** whether it has already been consumed */
    private BitSet noteOffEventUsed;

    /** Gets whether it has already been consumed. */
    public boolean isNoteOffEventUsed() {
        return noteOffEventUsed.get(midiEventIndex);
    }

    // SmafConvertible ----

    /** BANK LSB */
    private final int[] bankLSB = new int[MidiContext.MAX_MIDI_CHANNELS];
    /** BANK MSB */
    private final int[] bankMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** */
    public static final int RPN_PITCH_BEND_SENSITIVITY = 0x0000;
    /** */
    public static final int RPN_FINE_TUNING = 0x0001;
    /** */
    public static final int RPN_COURCE_TUNING = 0x0002;
    /** */
    public static final int RPN_TUNING_PROGRAM_SELECT = 0x0003;
    /** */
    public static final int RPN_TUNING_BANK_SELECT = 0x0004;
    /** */
    public static final int RPN_NULL = 0x7f7f;

    /** RPN LSB */
    private final int[] rpnLSB = new int[MidiContext.MAX_MIDI_CHANNELS];
    /** RPN MSB */
    private final int[] rpnMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** NRPN LSB */
    private final int[] nrpnLSB = new int[MidiContext.MAX_MIDI_CHANNELS];
    /** NRPN MSB */
    private final int[] nrpnMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** the control changes which are remembered here instead of being converted */
    private static final List<String> keys = List.of(
            "short.176.32", "short.176.98", "short.176.99", "short.176.100", "short.176.101");

    @Override
    public boolean accept(String key) {
        return keys.contains(key);
    }

    /** bank, rpn, nrpn */
    @Override
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context)
        throws InvalidSmafDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
//        int command = shortMessage.getCommand();
        int data1 = shortMessage.getData1();
        int data2 = shortMessage.getData2();

        switch (data1) {
        case 0:     // bank select MSB
            bankMSB[channel] = data2;
            break;
        case 32:    // bank select LSB
            bankLSB[channel] = data2;
            break;
        case 98:    // NRPN LSB
            nrpnLSB[channel] = data2;
            break;
        case 99:    // NRPN MSB
            nrpnMSB[channel] = data2;
            break;
        case 100:    // RPN LSB
            rpnLSB[channel] = data2;
            break;
        case 101:    // RPN MSB
            rpnMSB[channel] = data2;
            break;
        default:
//logger.log(Level.TRACE, "not implemented: " + data1);
            break;
        }

        return null;
    }
}
