/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.File;
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
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.vavi.track.NopMessage;
import vavi.sound.mfi.vavi.track.TempoMessage;
import vavi.sound.midi.MidiConstants;
import vavi.sound.midi.MidiUtil;
import vavi.util.Debug;


/**
 * mfi context for the converter.
 * <li>TODO {@link MfiConvertible} �����ł����̂��H
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030905 nsano initial version <br>
 *          0.01 030907 nsano add {@link MfiConvertible} handler <br>
 *          0.02 031211 nsano scale, be float <br>
 *          0.03 031212 nsano format 1 compliant <br>
 */
public class MfiContext {

    /** MFi �̃g���b�N���̍ő�l */
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

    /** TODO ���̂Ƃ��� {@link Sequence#getResolution()} */
    private int timeBase;

    /** */
    public int getTimeBase() {
        return timeBase;
    }

    //----

    /** index �� MFi Track No., �g�p����Ă���� true */
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
     * tick �̔{��
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

    /** ���O�� tick, index �� MFi Track No. */
    private long[] previousTicks = new long[MAX_MFI_TRACKS];

    /* init */ {
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

    /** @return �␳���� ���^�C�� */
    public int retrieveAdjustedDelta(int mfiTrackNumber, long currentTick) {
        return getAdjustedDelta(mfiTrackNumber, (currentTick - previousTicks[mfiTrackNumber]) / scale);
    }

    /** Math#round() �Ŋۂ߂�ꂽ�덷 */
    private double[] roundedSum = new double[MAX_MFI_TRACKS];
    
    /** Math#round() �Ŋۂ߂�ꂽ�덷�������l���傫���Ȃ����ꍇ�̕␳ */
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
     * ��O�� NoteOn ����̎��� (currentTick - beforeTicks[track]) ��
     * �����������邩(�����l�A���܂�؂�̂�)�����߁A���̌����}������
     * NopMessage �̔z���Ԃ��܂��B
     * <pre>
     *     event    index    process
     *   |
     * --+- NoteOn    -2    -> brforeTick
     * ��|
     * �b|
     * ��|- NoteOff    -1    -> noteOffEventUsed[-1] = true
     * �b|
     * ��|
     * --+-
     *   |
     *  -O- NoteOn    midiEventIndex
     *   |
     *   |
     *   |
     * --+-
     * </pre>
     * ��L�}���� 1 �� NopMessage ���}�������B
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
 // ���肦�Ȃ��͂�
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
            mfiEvents[i] = new MfiEvent(mfiMessage, 0l);    // TODO 0l
            // 255 �� �����ɂ��炵�Ă���
            incrementPreviousTick(mfiTrackNumber, Math.round(255 * scale));
        };

//Debug.println(nopLength + " nops inserted");
        return mfiEvents;
    }

    /**
     * �O�̃f�[�^(MIDI NoteOn)�����s����Ă���̃�(����)���擾���܂��B
     * �K�����O�� #getIntervalMfiEvents() �����s���ă��� 255 �ȉ���
     * �Ԃ��悤�ɂ��Ă����ĉ������B
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
            delta = retrieveAdjustedDelta(mfiTrackNumber, midiEvent.getTick()); // TODO 0 �ł����̂��H
//Debug.println("[" + midiEventIndex + "] delta for tempo, " + mfiTrackNumber + "ch: " + delta);
        } else {
Debug.println(Level.WARNING, "no delta defined for: " + MidiUtil.paramString(midiMessage));
        }

if (delta > 255) {
 // getIntervalMfiEvents �ŏ�������Ă���͂��Ȃ̂ł��肦�Ȃ�
 Debug.println(Level.SEVERE, "��: " + delta + ", " + (delta % 256));
}
        return delta % 256;
    }

    //----

    /** �␳���ꂽ MFi Pitch ���擾���܂��B sound -45, percussion -35 */
    public int retrievePitch(int channel, int pitch) {
        return pitch - 45 + (channel == MidiContext.CHANNEL_DRUM ? 10 : 0);
    }

    /**
     * MFi Voice No. ���擾���܂��B
     * @param channel MIDI channel
     */
    public int retrieveVoice(int channel) {
        return channel % 4;
    }

    /**
     * MFi Track ���擾���܂��B
     * @param channel MIDI channel
     */
    public int retrieveMfiTrack(int channel) {
        return channel / 4;
    }

    //----

    /** MIDI �C�x���g�̒P��V�[�P���X */
    private List<MidiEvent> midiEvents = new ArrayList<MidiEvent>();
    
    /** MIDI �C�x���g�̒P��V�[�P���X��ݒ肵�܂��B */
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

    /** ���݂� MIDI �C�x���g�̃C���f�b�N�X�l */
    private int midiEventIndex;

    /** ���݂� MIDI �C�x���g�̃C���f�b�N�X�l���擾���܂��B */
    int getMidiEventIndex() {
        return midiEventIndex;
    }

    /**
     * ���� channel �Ŏ��� {@link ShortMessage} �ł��� MIDI �C�x���g���擾���܂��B
     *
     * @throws NoSuchElementException ���� MIDI �C�x���g���Ȃ�
     * @throws IllegalStateException ���݂̃C�x���g�� {@link ShortMessage} �ł͂Ȃ�
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

    /** ���łɏ���ꂽ���ǂ��� */
    private BitSet noteOffEventUsed;

    /** ���łɏ���ꂽ���ǂ������擾���܂��B */
    public boolean isNoteOffEventUsed() {
        return noteOffEventUsed.get(midiEventIndex);
    }

    //-------------------------------------------------------------------------

    /**
     * Converts the midi file to a mfi file.
     * <pre>
     * usage:
     *  % java MfiContext in_midi_file out_mld_file
     * </pre>
     */
    public static void main(String[] args) throws Exception {

Debug.println("midi in: " + args[0]);
Debug.println("mfi out: " + args[1]);

        File file = new File(args[0]);
        javax.sound.midi.Sequence midiSequence = MidiSystem.getSequence(file);
        MidiFileFormat midiFileFormat = MidiSystem.getMidiFileFormat(file);
        int type = midiFileFormat.getType();
Debug.println("type: " + type);
        vavi.sound.mfi.Sequence mfiSequence = MfiSystem.toMfiSequence(midiSequence, type);
        file = new File(args[1]);
        int r = MfiSystem.write(mfiSequence, VaviMfiFileFormat.FILE_TYPE, file);
Debug.println("write: " + r);

        System.exit(0);
    }
}

/* */
