/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.util.Arrays;
import java.util.BitSet;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.util.Debug;


/**
 * SMAF context for the converter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class SmafContext implements SmafConvertible {

    /** max SMAF track number */
    public static final int MAX_SMAF_TRACKS = 4;

    //----

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

    /** TODO currently sequence#resolution */
    private int timeBase;

    /** */
    public int getTimeBase() {
        return timeBase;
    }

    /** */
    public void setTimeBase(int timeBase) {
        this.timeBase = timeBase;
    }

    //----

    /** index is SMAF Track No., true if used */
    private boolean[] trackUsed = new boolean[MAX_SMAF_TRACKS];

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

    //----

    /**
     * tick magnification
     */
    private double scale = 1.0d;

    /** */
    public double getScale() {
        return scale;
    }

    /** */
    public void setScale(float scale) {
Debug.println(Level.FINE, "scale: " + scale);
        this.scale = scale;
    }

    //----

    /** the previous tick, index is SMAF Track No. */
    private long[] beforeTicks = new long[MAX_SMAF_TRACKS];

    /* init */ {
        Arrays.fill(beforeTicks, 0);
    }

    /**
     * @param smafTrackNumber midi channel
     */
    public long getBeforeTick(int smafTrackNumber) {
        return beforeTicks[smafTrackNumber];
    }

    /**
     * @param smafTrackNumber midi track number
     */
    public void setBeforeTick(int smafTrackNumber, long beforeTick) {
        this.beforeTicks[smafTrackNumber] = beforeTick;
    }

    /**
     * @param smafTrackNumber midi track number
     */
    public void incrementBeforeTick(int smafTrackNumber, long delta) {
        this.beforeTicks[smafTrackNumber] += getAdjustedDelta(smafTrackNumber, delta * scale);
    }

    /** @return with correction Δ time */
    public int retrieveAdjustedDelta(int smafTrackNumber, long currentTick) {
        return getAdjustedDelta(smafTrackNumber, (currentTick - beforeTicks[smafTrackNumber]) / scale);
    }

    /**
     * @return no correction Δ time
     * TODO why does this work?
     */
    private int retrieveDelta(int smafTrackNumber, long currentTick) {
        return (int) Math.round((currentTick - beforeTicks[smafTrackNumber]) / scale);
    }

    /** error rounded with Math#round() */
    private double[] roundedSum = new double[MAX_SMAF_TRACKS];

    /** correction when the sum of rounding errors with Math#round() is larger than 1 */
    private int getAdjustedDelta(int smafTrackNumber, double floatDelta) {
        int delta = (int) Math.round(floatDelta);
        double rounded = floatDelta - delta;
        roundedSum[smafTrackNumber] += rounded;
        if (roundedSum[smafTrackNumber] >= 1f) {
Debug.println(Level.FINE, "rounded over 1, plus 1: " + roundedSum[smafTrackNumber] + "[" + smafTrackNumber + "]");
            delta += 1;
            roundedSum[smafTrackNumber] -= 1;
        } else if (roundedSum[smafTrackNumber] <= -1f) {
Debug.println(Level.FINE, "rounded under -1, minus 1: " + roundedSum[smafTrackNumber] + "[" + smafTrackNumber + "]");
            delta -= 1;
            roundedSum[smafTrackNumber] += 1;
        }
        return delta;
    }

    //----

    /**
     * Finds how many Δs(integer value, truncating too much) can be included in the time since the previous
     * NoteOn (currentTick - beforeTicks[track]) and returns an array of NopMessages to be inserted for that number.
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
     */
    public SmafEvent[] getIntervalSmafEvents() {

        int interval;
        int track;
        MidiEvent midiEvent = midiTrack.get(midiEventIndex);

        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage shortMessage) {
            // note
            int channel = shortMessage.getChannel();

            track = retrieveSmafTrack(channel);
            interval = retrieveDelta(track, midiEvent.getTick());
        } else if (midiMessage instanceof MetaMessage && ((MetaMessage) midiMessage).getType() == 81) {
            // tempo
            track = smafTrackNumber;
            interval = retrieveDelta(track, midiEvent.getTick());
Debug.println(Level.FINE, "interval for tempo[" + smafTrackNumber + "]: " + interval);
        } else if (midiMessage instanceof MetaMessage && ((MetaMessage) midiMessage).getType() == 47) {
            // eot
            track = smafTrackNumber;
            interval = retrieveDelta(track, midiEvent.getTick());
Debug.println(Level.FINE, "interval for EOT[" + smafTrackNumber + "]: " + interval);
        } else if (midiMessage instanceof SysexMessage) {
            return null;
        } else {
Debug.println(Level.WARNING, "not supported message: " + midiMessage);
            return null;
        }
//if (interval > 255) {
// Debug.println("interval: " + interval + ", " + (interval - 256));
//}
if (interval < 0) {
 // it shouldn't be possible
 Debug.println(Level.WARNING, "interval: " + interval);
 interval = 0;
}
        int nopLength = interval / 255;
        if (nopLength == 0) {
            return null;
        }
        SmafEvent[] smafEvents = new SmafEvent[nopLength];
        for (int i = 0; i < nopLength; i++) {
            NopMessage smafMessage = new NopMessage(255);
            smafEvents[i] = new SmafEvent(smafMessage, 0L);    // TODO 0l
            // shift backward by 255 Δ minutes
            incrementBeforeTick(track, 255);
        }

//Debug.println(nopLength + " nops inserted");
        return smafEvents;
    }

    /**
     * Gets the Δ (time) since the previous data (MIDI NoteOn) was executed.
     * Be sure to execute #getIntervalSmafEvents() in advance to return Δ less than 255.
     */
    public int getDuration() {

        int delta = 0;
        MidiEvent midiEvent = midiTrack.get(midiEventIndex);

        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage shortMessage) {
            // note
            int channel = shortMessage.getChannel();

            delta = retrieveAdjustedDelta(retrieveSmafTrack(channel), midiEvent.getTick());
        } else if (midiMessage instanceof MetaMessage && ((MetaMessage) midiMessage).getType() == 81) {
            // tempo
            delta = retrieveAdjustedDelta(smafTrackNumber, midiEvent.getTick()); // TODO is smafTrackNumber ok?
Debug.println(Level.FINE, "delta for tempo[" + smafTrackNumber + "]: " + delta);
        } else {
Debug.println(Level.FINE, "no delta defined for: " + midiMessage);
        }

if (delta > 255) {
 // this is impossible because it should be handled by getIntervalSmafEvents
 Debug.println(Level.WARNING, "Δ: " + delta + ", " + (delta % 256));
}
        return delta % 256;
    }

    //----

    /** Gets the corrected SMAF Pitch. sound -45, percussion -35 */
    public int retrievePitch(int channel, int pitch) {
        return pitch - 45 + (channel == MidiContext.CHANNEL_DRUM ? 10 : 0);
    }

    /**
     * Gets SMAF Voice No.
     * @param channel MIDI channel
     */
    public int retrieveVoice(int channel) {
        return channel % 4;
    }

    /**
     * Gets MIDI Channel.
     * @param voice SMAF channel
     */
    public int retrieveChannel(int voice) {
    return smafTrackNumber * 4 + voice;
    }

    /**
     * Gets SMAF Track.
     * @param channel MIDI channel
     */
    public int retrieveSmafTrack(int channel) {
        return channel / 4;
    }

    //----

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

    /** current MIDI track */
    private Track midiTrack;

    /** Sets current MIDI track. */
    public void setMidiTrack(Track midiTrack) {
        this.midiTrack = midiTrack;
        this.noteOffEventUsed = new BitSet(midiTrack.size());
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
     * Gets the next ShortMessage MIDI event on the same channel.
     *
     * @throws NoSuchElementException no next MIDI event
     * @throws IllegalStateException current event is not a ShortMessage
     */
    public MidiEvent getNextMidiEvent() throws NoSuchElementException {

        ShortMessage shortMessage;

        MidiEvent midiEvent = midiTrack.get(midiEventIndex);
        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage) {
            shortMessage = (ShortMessage) midiMessage;
        } else {
            throw new IllegalStateException("current is not ShortMessage");
        }

        int channel = shortMessage.getChannel();
        int data1 = shortMessage.getData1();

        for (int i = midiEventIndex + 1; i < midiTrack.size(); i++) {
            midiEvent = midiTrack.get(i);
            midiMessage = midiEvent.getMessage();
            if (midiMessage instanceof ShortMessage) {
                shortMessage = (ShortMessage) midiMessage;
                if (shortMessage.getChannel() == channel &&
                    shortMessage.getCommand() == ShortMessage.NOTE_ON &&
                    shortMessage.getData1() != data1) {
Debug.println(Level.FINE, "next: " + shortMessage.getChannel() + "ch, " + shortMessage.getData1());
                    return midiEvent;
                }
            }
        }

        throw new NoSuchElementException("no next event of channel: " + channel);
    }

    /**
     * Gets the currently selected NoteOn event and its counterpart NoteOff event.
     * Use IllegalStateException only for bug traps.
     * @see vavi.sound.smaf.message.NoteMessage
     *
     * @throws NoSuchElementException no paired NoteOff event
     * @throws IllegalStateException current event is not a ShortMessage
     */
    public MidiEvent getNoteOffMidiEvent() throws NoSuchElementException {

        ShortMessage shortMessage;

        MidiEvent midiEvent = midiTrack.get(midiEventIndex);
        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage) {
            shortMessage = (ShortMessage) midiMessage;
        } else {
            throw new IllegalStateException("current is not ShortMessage");
        }

        int channel = shortMessage.getChannel();
        int data1 = shortMessage.getData1();

        for (int i = midiEventIndex + 1; i < midiTrack.size(); i++) {
            midiEvent = midiTrack.get(i);
            midiMessage = midiEvent.getMessage();
            if (midiMessage instanceof ShortMessage) {
                shortMessage = (ShortMessage) midiMessage;
                if (shortMessage.getChannel() == channel &&
                    shortMessage.getData1() == data1) {

                    noteOffEventUsed.set(i);    // consumption flag on
                    return midiEvent;
                }
            }
        }

        throw new NoSuchElementException(channel + "ch, " + data1);
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
//Debug.println("not implemented: " + data1);
            break;
        }

        return null;
    }
}
