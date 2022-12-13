/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

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

    /** SMAF のトラック数の最大値 */
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

    /** TODO 今のところ sequence#resolution */
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

    /** index は SMAF Track No., 使用されていれば true */
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
     * tick の倍率
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

    /** 直前の tick, index は SMAF Track No. */
    private long[] beforeTicks = new long[MAX_SMAF_TRACKS];

    /* init */ {
        for (int i = 0; i < MAX_SMAF_TRACKS; i++) {
            beforeTicks[i] = 0;
        }
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

    /** @return 補正あり Δタイム */
    public int retrieveAdjustedDelta(int smafTrackNumber, long currentTick) {
        return getAdjustedDelta(smafTrackNumber, (currentTick - beforeTicks[smafTrackNumber]) / scale);
    }

    /**
     * @return 補正なし Δタイム
     * TODO 何でこれでうまくいくの？
     */
    private int retrieveDelta(int smafTrackNumber, long currentTick) {
        return (int) Math.round((currentTick - beforeTicks[smafTrackNumber]) / scale);
    }

    /** Math#round() で丸められた誤差 */
    private double[] roundedSum = new double[MAX_SMAF_TRACKS];

    /** Math#round() で丸められた誤差が整数値より大きくなった場合の補正 */
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
     */
    public SmafEvent[] getIntervalSmafEvents() {

        int interval;
        int track;
        MidiEvent midiEvent = midiTrack.get(midiEventIndex);

        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage) {
            // note
            ShortMessage shortMessage = (ShortMessage) midiMessage;
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
 // ありえないはず
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
            // 255 Δ 分後ろにずらしていく
            incrementBeforeTick(track, 255);
        }

//Debug.println(nopLength + " nops inserted");
        return smafEvents;
    }

    /**
     * 前のデータ(MIDI NoteOn)が実行されてからのΔ(時間)を取得します。
     * 必ず事前に #getIntervalSmafEvents() を実行してΔを 255 以下を
     * 返すようにしておいて下さい。
     */
    public int getDuration() {

        int delta = 0;
        MidiEvent midiEvent = midiTrack.get(midiEventIndex);

        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage) {
            // note
            ShortMessage shortMessage = (ShortMessage) midiMessage;
            int channel = shortMessage.getChannel();

            delta = retrieveAdjustedDelta(retrieveSmafTrack(channel), midiEvent.getTick());
        } else if (midiMessage instanceof MetaMessage && ((MetaMessage) midiMessage).getType() == 81) {
            // tempo
            delta = retrieveAdjustedDelta(smafTrackNumber, midiEvent.getTick()); // TODO smafTrackNumber でいいのか？
Debug.println(Level.FINE, "delta for tempo[" + smafTrackNumber + "]: " + delta);
        } else {
Debug.println(Level.FINE, "no delta defined for: " + midiMessage);
        }

if (delta > 255) {
 // getIntervalSmafEvents で処理されているはずなのでありえない
 Debug.println(Level.WARNING, "Δ: " + delta + ", " + (delta % 256));
}
        return delta % 256;
    }

    //----

    /** 補正された SMAF Pitch を取得します。 sound -45, percussion -35 */
    public int retrievePitch(int channel, int pitch) {
        return pitch - 45 + (channel == MidiContext.CHANNEL_DRUM ? 10 : 0);
    }

    /**
     * SMAF Voice No. を取得します。
     * @param channel MIDI channel
     */
    public int retrieveVoice(int channel) {
        return channel % 4;
    }

    /**
     * MIDI Channel を取得します。
     * @param voice SMAF channel
     */
    public int retrieveChannel(int voice) {
    return smafTrackNumber * 4 + voice;
    }

    /**
     * SMAF Track を取得します。
     * @param channel MIDI channel
     */
    public int retrieveSmafTrack(int channel) {
        return channel / 4;
    }

    //----

    /** 現在の SMAF のトラック No. */
    private int smafTrackNumber;

    /** 現在の SMAF トラック No. を設定します。 */
    public void setSmafTrackNumber(int smafTrackNumber) {
        this.smafTrackNumber = smafTrackNumber;
    }

    /** 現在の SMAF トラック No. を取得します。 */
    public int getSmafTrackNumber() {
        return smafTrackNumber;
    }

    /** 現在の MIDI トラック */
    private Track midiTrack;

    /** 現在の MIDI トラックを設定します。 */
    public void setMidiTrack(Track midiTrack) {
        this.midiTrack = midiTrack;
        this.noteOffEventUsed = new BitSet(midiTrack.size());
    }

    /** 現在の MIDI イベントのインデックス値 */
    private int midiEventIndex;

    /** 現在の MIDI イベントのインデックス値を設定します。 */
    public void setMidiEventIndex(int midiEventIndex) {
        this.midiEventIndex = midiEventIndex;
    }

    /** 現在の MIDI イベントのインデックス値を取得します。 */
    int getMidiEventIndex() {
        return midiEventIndex;
    }

    /**
     * 同じ channel で次の ShortMessage である MIDI イベントを取得します。
     *
     * @throws NoSuchElementException 次の MIDI イベントがない
     * @throws IllegalStateException 現在のイベントは ShortMessage ではない
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
     * 現在選択中の NoteOn イベントと対の NoteOff イベントを取得します。
     * IllegalStateException はバグトラップのためだけに使用してください。
     * @see vavi.sound.smaf.message.NoteMessage
     *
     * @throws NoSuchElementException 対の NoteOff イベントがない
     * @throws IllegalStateException 現在のイベントは ShortMessage ではない
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

                    noteOffEventUsed.set(i);    // 消費フラグ on
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

    // SmafConvertible ---------------------------------------------------------

    /** BANK LSB */
    private int[] bankLSB = new int[MidiContext.MAX_MIDI_CHANNELS];
    /** BANK MSB */
    private int[] bankMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

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
    private int[] rpnLSB = new int[MidiContext.MAX_MIDI_CHANNELS];
    /** RPN MSB */
    private int[] rpnMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** NRPN LSB */
    private int[] nrpnLSB = new int[MidiContext.MAX_MIDI_CHANNELS];
    /** NRPN MSB */
    private int[] nrpnMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** bank, rpn, nrpn */
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context)
        throws InvalidSmafDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
//        int command = shortMessage.getCommand();
        int data1 = shortMessage.getData1();
        int data2 = shortMessage.getData2();

        switch (data1) {
        case 0:        // バンクセレクト MSB
            bankMSB[channel] = data2;
            break;
        case 32:    // バンクセレクト LSB
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

/* */
