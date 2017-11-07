/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;

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
import vavi.sound.midi.MidiConstants;
import vavi.sound.midi.MidiUtil;
import vavi.util.Debug;


/**
 * mfi context for the converter.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030905 nsano initial version <br>
 *          0.01 030907 nsano add {@link MfiConvertible} handler <br>
 *          0.02 031211 nsano scale, be float <br>
 *          0.03 031212 nsano format 1 compliant <br>
 */
public class MfiContext {

    /** MFi のトラック数の最大値 */
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
Debug.println("type: " + type);
        this.type = type;
    }

    /** TODO 今のところ {@link Sequence#getResolution()} */
    private int timeBase;

    /** */
    public int getTimeBase() {
        return timeBase;
    }

    //----

    /** index は MFi Track No., 使用されていれば true */
    private boolean[] trackUsed = new boolean[MAX_MFI_TRACKS];

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
    private boolean[] eofSet = new boolean[MAX_MFI_TRACKS];

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
     * tick の倍率
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

    /** 直前の tick, index は MFi Track No. */
    private long[] previousTicks = new long[MAX_MFI_TRACKS];

    /* initializing */ {
        for (int i = 0; i < MAX_MFI_TRACKS; i++) {
            previousTicks[i] = 0;
        }
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

    /** @return 補正あり Δタイム */
    public int retrieveAdjustedDelta(int mfiTrackNumber, long currentTick) {
        return getAdjustedDelta(mfiTrackNumber, (currentTick - previousTicks[mfiTrackNumber]) / scale);
    }

    /** Math#round() で丸められた誤差 */
    private double[] roundedSum = new double[MAX_MFI_TRACKS];

    /** Math#round() で丸められた誤差が整数値より大きくなった場合の補正 */
    private int getAdjustedDelta(int mfiTrackNumber, double doubleDelta) {
        int delta = (int) Math.round(doubleDelta);
        double rounded = doubleDelta - delta;
        roundedSum[mfiTrackNumber] += rounded;
        if (roundedSum[mfiTrackNumber] >= 1d) {
//Debug.println("rounded over 1, plus 1: " + roundedSum[mfiTrackNumber] + "[" + mfiTrackNumber + "]");
            delta += 1;
            roundedSum[mfiTrackNumber] -= 1;
        } else if (roundedSum[mfiTrackNumber] <= -1d) {
//Debug.println("rounded under -1, minus 1: " + roundedSum[mfiTrackNumber] + "[" + mfiTrackNumber + "]");
            delta -= 1;
            roundedSum[mfiTrackNumber] += 1;
        }
        return delta;
    }

    //----

    /**
     * 一つ前の NoteOn からの時間 (currentTick - beforeTicks[track]) に
     * いくつΔが入るか(整数値、あまり切り捨て)を求め、その個数分挿入する
     * NopMessage の配列を返します。
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
     * 上記図だと 1 つの NopMessage が挿入される。
     *
     * @return null current event is MetaMessage or SysexMessage or delta not supported.
     */
    public MfiEvent[] getIntervalMfiEvents(int mfiTrackNumber) {

        int interval = 0;
        MidiEvent midiEvent = midiEvents.get(midiEventIndex);

        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage) {
            // note
            interval = retrieveAdjustedDelta(mfiTrackNumber, midiEvent.getTick());
        } else if (midiMessage instanceof MetaMessage && ((MetaMessage) midiMessage).getType() == 81) {
            // tempo
            interval = retrieveAdjustedDelta(mfiTrackNumber, midiEvent.getTick());
Debug.println("interval for tempo[" + mfiTrackNumber + "]: " + interval);
        } else if (midiMessage instanceof MetaMessage && ((MetaMessage) midiMessage).getType() == 47) {
            // eot
            interval = retrieveAdjustedDelta(mfiTrackNumber, midiEvent.getTick());
//Debug.println("interval for EOT[" + mfiTrackNumber + "]: " + interval);
        } else if (midiMessage instanceof MetaMessage) {
            return null;
        } else if (midiMessage instanceof SysexMessage) {
            return null;
        } else {
Debug.println(Level.WARNING, "not supported for delta: " + midiEventIndex + ", " + MidiUtil.paramString(midiMessage));
            return null;
        }
if (interval < 0) {
 // ありえないはず
 Debug.println(Level.WARNING, "interval: " + interval + ", " + midiEventIndex + ", " + MidiUtil.paramString(midiMessage));
 interval = 0;
}
        int nopLength = interval / 255;
        if (nopLength == 0) {
            return null;
        }
        MfiEvent[] mfiEvents = new MfiEvent[nopLength];
        for (int i = 0; i < nopLength; i++) {
            NopMessage mfiMessage = new NopMessage(255, 0);
            mfiEvents[i] = new MfiEvent(mfiMessage, 0l); // TODO 0l
            // 255 Δ 分後ろにずらしていく
            incrementPreviousTick(mfiTrackNumber, Math.round(255 * scale));
        };

//Debug.println(nopLength + " nops inserted");
        return mfiEvents;
    }

    /**
     * 前のデータ(MIDI NoteOn)が実行されてからのΔ(時間)を取得します。
     * 必ず事前に {@link #getIntervalMfiEvents(int)} を実行してΔを 255 以下を
     * 返すようにしておいて下さい。
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
            delta = retrieveAdjustedDelta(mfiTrackNumber, midiEvent.getTick()); // TODO 0 でいいのか？
//Debug.println("[" + midiEventIndex + "] delta for tempo, " + mfiTrackNumber + "ch: " + delta);
        } else {
Debug.println(Level.WARNING, "no delta defined for: " + MidiUtil.paramString(midiMessage));
        }

if (delta > 255) {
 // getIntervalMfiEvents で処理されているはずなのでありえない
 Debug.println(Level.SEVERE, "Δ: " + delta + ", " + (delta % 256));
}
        return delta % 256;
    }

    //----

    /**
     * 補正された MFi Pitch を取得します。
     * <p>
     * sound -45, percussion -35
     * </p>
     */
    public int retrievePitch(int channel, int pitch) {
        return pitch - 45 + (channel == MidiContext.CHANNEL_DRUM ? 10 : 0);
    }

    /**
     * MFi Voice No. を取得します。
     * @param channel MIDI channel
     */
    public int retrieveVoice(int channel) {
        return channel % 4;
    }

    /**
     * MFi Track を取得します。
     * @param channel MIDI channel
     */
    public int retrieveMfiTrack(int channel) {
        return channel / 4;
    }

    //----

    /** MIDI イベントの単一シーケンス */
    private List<MidiEvent> midiEvents = new ArrayList<>();

    /** MIDI イベントの単一シーケンスを設定します。 */
    public void setMidiSequence(Sequence midiSequence) {

        this.timeBase = midiSequence.getResolution();

        Track midiTracks[] = midiSequence.getTracks();
        for (int t = 0; t < midiTracks.length; t++) {
            for (int i = 0; i < midiTracks[t].size(); i++) {
                javax.sound.midi.MidiMessage midiMessage = midiTracks[t].get(i).getMessage();
                if (midiMessage instanceof MetaMessage &&
                    ((MetaMessage) midiMessage).getType() == MidiConstants.META_TEMPO) {

                    MetaMessage metaMessage = (MetaMessage) midiMessage;
                    byte data[] = metaMessage.getData();

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
Debug.println("(SCALE) final scale: " + scale + ", " + scaleChanged);

        for (int t = 0; t < midiTracks.length; t++) {
            for (int i = 0; i < midiTracks[t].size(); i++) {
                this.midiEvents.add(midiTracks[t].get(i));
            }
        }
        Collections.sort(midiEvents,
            new Comparator<MidiEvent>() {
                /** */
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

    /** 現在の MIDI イベントのインデックス値 */
    private int midiEventIndex;

    /** 現在の MIDI イベントのインデックス値を取得します。 */
    int getMidiEventIndex() {
        return midiEventIndex;
    }

    /**
     * 同じ channel で次の {@link ShortMessage} である MIDI イベントを取得します。
     *
     * @throws NoSuchElementException 次の MIDI イベントがない
     * @throws IllegalStateException 現在のイベントは {@link ShortMessage} ではない
     */
    public MidiEvent getNoteOffMidiEvent() throws NoSuchElementException {

        ShortMessage shortMessage = null;

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
//Debug.println("next: " + shortMessage.getChannel() + "ch, " + shortMessage.getData1());
                    noteOffEventUsed.set(i);
                    return midiEvent;
                }
            }
        }

        throw new NoSuchElementException(channel + "ch, " + data1);
    }

    /** すでに消費されたかどうか */
    private BitSet noteOffEventUsed;

    /** すでに消費されたかどうかを取得します。 */
    public boolean isNoteOffEventUsed() {
        return noteOffEventUsed.get(midiEventIndex);
    }
}

/* */
