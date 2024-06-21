/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.vavi.track.NopMessage;
import vavi.sound.mfi.vavi.track.TempoMessage;
import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.midi.MidiUtil;

import static java.lang.System.getLogger;


/**
 * mfi context for the converter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030905 nsano initial version <br>
 *          0.01 030907 nsano add {@link MfiConvertible} handler <br>
 *          0.02 031211 nsano scale, be float <br>
 *          0.03 031212 nsano format 1 compliant <br>
 */
public class MfiContext {

    private static final Logger logger = getLogger(MfiContext.class.getName());

    /** max MFi track number */
    public static final int MAX_MFI_TRACKS = 4;

    //----

    /** @see MidiFileFormat#getType() 0: SMF Format 0, 1: SMF Format 1 */
    private int type;

    /** MIDI format type */
    public int getType() {
        return type;
    }

    /** MIDI format type */
    public void setType(int type) {
logger.log(Level.DEBUG, "type: " + type);
        this.type = type;
    }

    /** TODO currently {@link Sequence#getResolution()} */
    private int timeBase;

    /** */
    public int getTimeBase() {
        return timeBase;
    }

    //----

    /** index is MFi Track No., when that's used this returns true */
    private final boolean[] trackUsed = new boolean[MAX_MFI_TRACKS];

    /**
     * @param mfiTrackNumber mfi track number
     */
    public void setTrackUsed(int mfiTrackNumber, boolean trackUsed) {
        this.trackUsed[mfiTrackNumber] = trackUsed;
    }

    /**
     * @param mfiTrackNumber mfi track number
     */
    public boolean isTrackUsed(int mfiTrackNumber) {
        return trackUsed[mfiTrackNumber];
    }

    /** is EOF set to the track ? */
    private final boolean[] eofSet = new boolean[MAX_MFI_TRACKS];

    /**
     * @param mfiTrackNumber
     * @param flag
     */
    public void setEofSet(int mfiTrackNumber, boolean flag) {
        eofSet[mfiTrackNumber] = flag;
    }

    /**
     * @param mfiTrackNumber
     */
    public boolean isEofSet(int mfiTrackNumber) {
        return eofSet[mfiTrackNumber];
    }

    //----

    /**
     * magnification of tick
     * @see vavi.sound.mfi.vavi.track.TempoMessage
     */
    private double scale = 1.0d;

    /** */
    public double getScale() {
        return scale;
    }

    /** is {@link #scale} changed ? */
    private boolean scaleChanged = false;

    /**
     * @return Returns the scaleChanged.
     */
    public boolean isScaleChanged() {
        return scaleChanged;
    }

    //----

    /** previous tick, index is MFi Track No. */
    private final long[] previousTicks = new long[MAX_MFI_TRACKS];

    /* initializing */ {
        Arrays.fill(previousTicks, 0);
    }

    /**
     * @param mfiTrackNumber midi channel
     */
    public long getPreviousTick(int mfiTrackNumber) {
        return previousTicks[mfiTrackNumber];
    }

    /**
     * @param mfiTrackNumber midi track number
     */
    public void setPreviousTick(int mfiTrackNumber, long tick) {
        this.previousTicks[mfiTrackNumber] = tick;
    }

    /**
     * @param mfiTrackNumber midi track number
     */
    public void incrementPreviousTick(int mfiTrackNumber, long tick) {
        this.previousTicks[mfiTrackNumber] += tick;
    }

    /** @return with correction Δ time */
    public int retrieveAdjustedDelta(int mfiTrackNumber, long currentTick) {
        return getAdjustedDelta(mfiTrackNumber, (currentTick - previousTicks[mfiTrackNumber]) / scale);
    }

    /** errors rounded by Math#round() */
    private final double[] roundedSum = new double[MAX_MFI_TRACKS];

    /** Gets adjustment when sum of errors rounded by Math#round() become larger than 1 */
    private int getAdjustedDelta(int mfiTrackNumber, double doubleDelta) {
        int delta = (int) Math.round(doubleDelta);
        double rounded = doubleDelta - delta;
        roundedSum[mfiTrackNumber] += rounded;
        if (roundedSum[mfiTrackNumber] >= 1d) {
//logger.log(Level.DEBUG, "rounded over 1, plus 1: " + roundedSum[mfiTrackNumber] + "[" + mfiTrackNumber + "]");
            delta += 1;
            roundedSum[mfiTrackNumber] -= 1;
        } else if (roundedSum[mfiTrackNumber] <= -1d) {
//logger.log(Level.DEBUG, "rounded under -1, minus 1: " + roundedSum[mfiTrackNumber] + "[" + mfiTrackNumber + "]");
            delta -= 1;
            roundedSum[mfiTrackNumber] += 1;
        }
        return delta;
    }

    //----

    /**
     * Finds how many Δs(integer value, truncating too much) can be included in the time since the previous NoteOn
     * (currentTick - beforeTicks[track]) and returns an array of NopMessages to be inserted for that number.
     * <pre>
     *     event    index    process
     *   |
     * --+- NoteOn    -2    -> brforeTick
     * ↑|
     * ｜|
     * Δ|- NoteOff    -1    -> noteOffEventUsed[-1] = true
     * ｜|
     * ↓|
     * --+-
     *   |
     *  -O- NoteOn    midiEventIndex
     *   |
     *   |
     *   |
     * --+-
     * </pre>
     * case of above, one NopMessage will be inserted
     *
     * @return null current event is MetaMessage or SysexMessage or delta not supported.
     */
    public MfiEvent[] getIntervalMfiEvents(int mfiTrackNumber) {

        int interval;
        MidiEvent midiEvent = midiEvents.get(midiEventIndex);

        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage) {
            // note
            interval = retrieveAdjustedDelta(mfiTrackNumber, midiEvent.getTick());
        } else if (midiMessage instanceof MetaMessage && ((MetaMessage) midiMessage).getType() == 81) {
            // tempo
            interval = retrieveAdjustedDelta(mfiTrackNumber, midiEvent.getTick());
logger.log(Level.DEBUG, "interval for tempo[" + mfiTrackNumber + "]: " + interval);
        } else if (midiMessage instanceof MetaMessage && ((MetaMessage) midiMessage).getType() == 47) {
            // eot
            interval = retrieveAdjustedDelta(mfiTrackNumber, midiEvent.getTick());
//logger.log(Level.DEBUG, "interval for EOT[" + mfiTrackNumber + "]: " + interval);
        } else if (midiMessage instanceof MetaMessage) {
            return null;
        } else if (midiMessage instanceof SysexMessage) {
            return null;
        } else {
logger.log(Level.WARNING, "not supported for delta: " + midiEventIndex + ", " + MidiUtil.paramString(midiMessage));
            return null;
        }
if (interval < 0) {
 // it shouldn't be possible
 logger.log(Level.WARNING, "interval: " + interval + ", " + midiEventIndex + ", " + MidiUtil.paramString(midiMessage));
 interval = 0;
}
        int nopLength = interval / 255;
        if (nopLength == 0) {
            return null;
        }
        MfiEvent[] mfiEvents = new MfiEvent[nopLength];
        for (int i = 0; i < nopLength; i++) {
            NopMessage mfiMessage = new NopMessage(255, 0);
            mfiEvents[i] = new MfiEvent(mfiMessage, 0L); // TODO 0l
            // shift backward by 255Δ
            incrementPreviousTick(mfiTrackNumber, Math.round(255 * scale));
        }

//logger.log(Level.DEBUG, nopLength + " nops inserted");
        return mfiEvents;
    }

    /**
     * Gets the Δ(time) since the previous data (MIDI NoteOn) was executed.
     * Be sure to execute {@link #getIntervalMfiEvents(int)} in advance to return Δ less than 255.
     */
    public int getDelta(int mfiTrackNumber) {

        int delta = 0;
        MidiEvent midiEvent = midiEvents.get(midiEventIndex);

        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage) {
            // note
            delta = retrieveAdjustedDelta(mfiTrackNumber, midiEvent.getTick());
        } else if (midiMessage instanceof MetaMessage && ((MetaMessage) midiMessage).getType() == 81) {
            // tempo
            delta = retrieveAdjustedDelta(mfiTrackNumber, midiEvent.getTick()); // TODO is 0 ok?
//logger.log(Level.DEBUG, "[" + midiEventIndex + "] delta for tempo, " + mfiTrackNumber + "ch: " + delta);
        } else {
logger.log(Level.WARNING, "no delta defined for: " + MidiUtil.paramString(midiMessage));
        }

if (delta > 255) {
 // this is impossible because it should be handled by getIntervalMfiEvents
 logger.log(Level.ERROR, "Δ: " + delta + ", " + (delta % 256));
}
        return delta % 256;
    }

    //----

    /**
     * Gets the corrected MFi pitch.
     * <p>
     * sound -45, percussion -35
     * </p>
     */
    public int retrievePitch(int channel, int pitch) {
        return pitch - 45 + (channel == MidiContext.CHANNEL_DRUM ? 10 : 0);
    }

    /**
     * Gets the MFi Voice No.
     * @param channel MIDI channel
     */
    public int retrieveVoice(int channel) {
        return channel % 4;
    }

    /**
     * Gets the MFi Track.
     * @param channel MIDI channel
     */
    public int retrieveMfiTrack(int channel) {
        return channel / 4;
    }

    //----

    /** a single sequence of MIDI events */
    private final List<MidiEvent> midiEvents = new ArrayList<>();

    /** Sets a single sequence of MIDI events. */
    public void setMidiSequence(Sequence midiSequence) {

        this.timeBase = midiSequence.getResolution();

        Track[] midiTracks = midiSequence.getTracks();
        for (Track track : midiTracks) {
            for (int i = 0; i < track.size(); i++) {
                MidiMessage midiMessage = track.get(i).getMessage();
                if (midiMessage instanceof MetaMessage metaMessage &&
                        ((MetaMessage) midiMessage).getType() == MetaEvent.META_TEMPO.number()) {

                    byte[] data = metaMessage.getData();

                    int timeBase = TempoMessage.getNearestTimeBase(this.timeBase);
                    int l = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | data[2] & 0xff;
                    int tempo = (int) Math.round(60d * 1000000d / ((48d / timeBase) * l));

                    for (int divider = (tempo + 254) / 255; tempo > 255; divider *= 2) {
                        timeBase = TempoMessage.getNearestTimeBase(timeBase / divider);
                        tempo = (int) Math.round(60d * 1000000d / ((48d / timeBase) * l));
                        double scale = (double) this.timeBase / timeBase;
                        if (scale > this.scale) {
                            this.scale = scale;
                            scaleChanged = true;
                        }
                    }
                }
            }
        }
        this.scale = Math.ceil(this.scale);
logger.log(Level.DEBUG, "(SCALE) final scale: " + scale + ", " + scaleChanged);

        for (Track midiTrack : midiTracks) {
            for (int i = 0; i < midiTrack.size(); i++) {
                this.midiEvents.add(midiTrack.get(i));
            }
        }
        midiEvents.sort(new Comparator<MidiEvent>() {
            @Override
            public int compare(MidiEvent e1, MidiEvent e2) {
                long t1 = e1.getTick();
                long t2 = e2.getTick();
                if (t1 - t2 != 0) {
                    return (int) (t1 - t2);
                } else {
                    int c1 = getChannel(e1);
                    int c2 = getChannel(e2);
                    return c1 - c2;
                }
            }

            /** */
            int getChannel(MidiEvent e) {
                MidiMessage m = e.getMessage();
                if (m instanceof ShortMessage) {
                    return ((ShortMessage) m).getChannel();
                } else {
                    return -1;
                }
            }
        });

        this.noteOffEventUsed = new BitSet(midiEvents.size());
    }

    /**
     * @return midi events size
     */
    public int getSequenceSize() {
        return midiEvents.size();
    }

    /**
     * @param midiEventIndex
     * @return MIDI event
     */
    public MidiEvent getMidiEvent(int midiEventIndex) {
        this.midiEventIndex = midiEventIndex;
        return midiEvents.get(midiEventIndex);
    }

    /** current MIDI event index */
    private int midiEventIndex;

    /** Gets current MIDI event index. */
    int getMidiEventIndex() {
        return midiEventIndex;
    }

    /**
     * Get the next {@link ShortMessage} MIDI event on the same channel.
     *
     * @throws NoSuchElementException when no next MIDI event
     * @throws IllegalStateException current event is not {@link ShortMessage}
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
                    shortMessage.getData1() != data1 &&
                    shortMessage.getData2() == 0 &&
                    !noteOffEventUsed.get(i)) {
//logger.log(Level.DEBUG, "next: " + shortMessage.getChannel() + "ch, " + shortMessage.getData1());
                    noteOffEventUsed.set(i);
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
}
