/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.midi.MidiConstants;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.Track;
import vavi.sound.smaf.chunk.ChannelStatus;
import vavi.sound.smaf.chunk.ScoreTrackChunk;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;
import vavi.util.Debug;


/**
 * MIDI Context for a converter.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class MidiContext {

    /** Max MIDI channels */
    public static final int MAX_MIDI_CHANNELS = 16;

    /** チャンネルコンフィギュレーション */
    public enum ChannelConfiguration {
        /** リズム */
        PERCUSSION,
        /** その他 */
        SOUND_SET,
        /** 未使用 */
        UNUSED
    };

    /** channel 9 はデフォルトでリズム */
    public static final int CHANNEL_DRUM = 9;

    /** */
    public MidiContext() {
        drums[CHANNEL_DRUM] = ChannelConfiguration.PERCUSSION;
    }

    /** */
    private FormatType formatType;

    /** */
    public FormatType getFormatType() {
        return formatType;
    }

    /**
     * must {@link #setSmafTrackNumber(int)} before use this method
     * @see #formatType
     * @see #setDrum(int, vavi.sound.smaf.message.MidiContext.ChannelConfiguration)
     */
    public void setTrack(Track smafTrack) {
        for (int j = 0; j < smafTrack.size(); j++) {
            SmafEvent event = smafTrack.get(j);
            SmafMessage message = event.getMessage();
            if (message instanceof vavi.sound.smaf.MetaMessage) {
                vavi.sound.smaf.MetaMessage metaMessage = (vavi.sound.smaf.MetaMessage) message;
                if (metaMessage.getType() == MidiConstants.META_MACHINE_DEPEND) {
                    //
                    this.formatType = (FormatType) metaMessage.getData().get("formatType"); // [ms]
Debug.println("formatType: " + formatType);
                    for (int i = 0; i < MAX_MIDI_CHANNELS; i++) {
                        if (formatType == FormatType.HandyPhoneStandard) {
                            velocities[i] = 0x7f;
                        } else {
                            velocities[i] = 64;
                        }
                    }
                    //
                    ChannelStatus[] channelStatuses = (ChannelStatus[]) metaMessage.getData().get("channelStatuses");
Debug.println("channelStatuses: " + (channelStatuses != null ? channelStatuses.length : null));
                    if (channelStatuses != null) {
                        for (int i = 0; i < channelStatuses.length; i++) {
                            setDrum(i, toChannelConfiguration(getMidiChannel(i), channelStatuses[i].getType()));
//if (getMidiChannel(i) == CHANNEL_DRUM) {
// setDrum(i, ChannelConfiguration.PERCUSSION);
//} else {
// setDrum(i, ChannelConfiguration.SOUND_SET);
//}
                        }
                    }
                }
            }
        }
    }

    /** @param midiTrack */
    public void setMidiTrack(javax.sound.midi.Track midiTrack) {
        this.midiTrack = midiTrack;
    }

    /** */
    private javax.sound.midi.Track midiTrack;

    /** @return midi Track */
    public javax.sound.midi.Track getMidiTrack() {
        return midiTrack;
    }

    /** convert types */
    private ChannelConfiguration toChannelConfiguration(int midiChannel, ChannelStatus.Type type) {
        switch (type) {
        case Melody:
            return ChannelConfiguration.SOUND_SET;
        case NoCare:
            return midiChannel == CHANNEL_DRUM ? ChannelConfiguration.PERCUSSION : ChannelConfiguration.SOUND_SET;
        case NoMelody:
        default:
            return ChannelConfiguration.UNUSED;
        case Rhythm:
            return ChannelConfiguration.PERCUSSION;
        }
    }

    /** 現在のトラック No. */
    private int smafTrackNumber;

    /** */
    public int getSmafTrackNumber() {
        return smafTrackNumber;
    }

    /** */
    public void setSmafTrackNumber(int smafTrackNumber) {
        this.smafTrackNumber = smafTrackNumber;
    }

    /** 現在の ticks, index is smafTrackNumber */
    private long[] currentTicks = new long[4];

    /**
     * smafTrackNumber must be set
     * @return ticks 
     */
    public long getCurrentTick() {
        return currentTicks[smafTrackNumber];
    }

    /**
     * smafTrackNumber must be set
     * @param ticks ticks 
     */
    public void setCurrentTick(long ticks) {
        this.currentTicks[smafTrackNumber] = ticks;
    }

    /**
     * smafTrackNumber must be set
     * @param ticks ticks 
     */
    public void addCurrentTick(long ticks) {
        this.currentTicks[smafTrackNumber] += ticks;
    }

    /** channel がリズムかどうか, index is pseudo MIDI channel */
    private ChannelConfiguration[] drums = new ChannelConfiguration[MAX_MIDI_CHANNELS];

    /** index is pseudo MIDI channel */
    private int[] velocities = new int[MAX_MIDI_CHANNELS];

    /* init */ {
        for (int i = 0; i < MAX_MIDI_CHANNELS; i++) {
            drums[i] = ChannelConfiguration.UNUSED;
        }
    }

    /** */
    private static final int CHANNEL_UNUSED = -1;

    /** DRUM_CHANNEL がリズムでない場合の交換先チャンネル */
    private int drumSwapChannel = CHANNEL_UNUSED;

    /**
     * @param smafChannel SMAF channel
     */
    public ChannelConfiguration getDrum(int smafChannel) {
        int midiChannel = getMidiChannel(smafChannel);
        return drums[midiChannel];
    }

    /**
     * @param smafChannel SMAF channel
     * @param value drum or sound
     */
    public void setDrum(int smafChannel, ChannelConfiguration value) {
        int midiChannel = getMidiChannel(smafChannel);

        if (drumSwapChannel != CHANNEL_UNUSED && midiChannel == drumSwapChannel) {
Debug.println("already swapped: " + midiChannel + ", " + value);
        } else {
            drums[midiChannel] = value;
//Debug.println("temporary: " + midiChannel + ", " + value);
        }

        // DRUM_CHANNEL がリズムでなければ空いてる channel と交換
        if (midiChannel == CHANNEL_DRUM && drums[CHANNEL_DRUM] == ChannelConfiguration.SOUND_SET && drumSwapChannel == CHANNEL_UNUSED) {
            for (int k = MAX_MIDI_CHANNELS - 1; k >= 0; k--) {
                if (k != CHANNEL_DRUM && drums[k] == ChannelConfiguration.UNUSED) {
                    drumSwapChannel = k;
Debug.println("channel 9 -> " + k);
                    break;
                }
            }
if (drumSwapChannel == CHANNEL_UNUSED) {
 Debug.println("cannot swap: " + midiChannel + ", " + value);
}
Debug.println("channel configuration: " + midiChannel + "ch, " + drums[midiChannel]);
        }
if (value != ChannelConfiguration.UNUSED) {
StringBuilder sb1 = new StringBuilder(16);
StringBuilder sb2 = new StringBuilder(16);
StringBuilder sb3 = new StringBuilder(16);
for (int i = 0; i < drums.length; i++) {
 sb1.append(drumSwapChannel != CHANNEL_UNUSED && drumSwapChannel == i ? "@" : String.format("%1x", i));
 sb2.append(midiChannel == i ? "*" : " ");
 sb3.append(drums[i].name().charAt(0));
}
Debug.println("drums: " + midiChannel + "ch, " + value + "\n" + sb1 + "\n" + sb2 + "\n" + sb3);
}
    }

    /** channel に割り当てられた program no, index is pseudo MIDI channel */
    private int[] programs = new int[MAX_MIDI_CHANNELS];

    /** 
     * @param smafChannel SMAF channel 
     * @return pseudo MIDI channel 
     */
    private int getMidiChannel(int smafChannel) {
        if (formatType == ScoreTrackChunk.FormatType.HandyPhoneStandard) {
            return smafTrackNumber * 4 + smafChannel;
        } else {
if (smafTrackNumber > 0) {
 Debug.println("track > 0: " + smafTrackNumber);
}
            return smafTrackNumber * 16 + smafChannel;
        }
    }

    /**
     * @param smafChannel SMAF channel
     * @return ドラム置き換え後のチャンネル (real MIDI channel)
     */
    public int setProgram(int smafChannel, int program) {
        int midiChannel = getMidiChannel(smafChannel);

        if (formatType != FormatType.HandyPhoneStandard) {
            if (midiChannel != drumSwapChannel && drums[midiChannel] == ChannelConfiguration.PERCUSSION) {
Debug.println("drum always zero:[" + midiChannel + "]: " + program);
                program = 0;
            }
        }

        programs[midiChannel] = program;

        midiChannel = retrieveChannel(smafChannel);

        return midiChannel;
    }

    /**
     * @param smafChannel SMAF channel
     */
    public int getProgram(int smafChannel) {
        int midiChannel = getMidiChannel(smafChannel);
        return programs[midiChannel];
    }

    //---- note

    /**
     * @param smafChannel SMAF channel
     * @return ドラム置き換え後のチャンネル (real MIDI channel)
     * @see #drums
     */
    public int retrieveChannel(int smafChannel) {
        int midiChannel = getMidiChannel(smafChannel);

//        if (midiChannel == drumSwapChannel) {
//Debug.println("used swapped channel: " + midiChannel);
//        }

        // ドラムのチャンネルがサウンドとして使用されている
        if (midiChannel == CHANNEL_DRUM && drums[CHANNEL_DRUM] == ChannelConfiguration.SOUND_SET) {
            midiChannel = drumSwapChannel;
        }

        // パーカッション指定はすべて MIDI ドラムチャネルに
        if (drums[midiChannel] == ChannelConfiguration.PERCUSSION) {
            midiChannel = CHANNEL_DRUM;
        }

        return midiChannel;
    }

    /**
     * HandyPhoneStandard: sound +36
     * @param smafChannel SMAF channel
     * @param pitch SMAF pitch
     * @return MIDI pitch
     * @see #programs
     */
    public int retrievePitch(int smafChannel, int pitch) {
        if (formatType == ScoreTrackChunk.FormatType.HandyPhoneStandard) {
            int midiChannel = getMidiChannel(smafChannel);
            if (drums[midiChannel] == ChannelConfiguration.PERCUSSION) {
//Debug.println("drum pitch: " + (programs[midiChannel] & 0x7f) + ", " + pitch);
                return programs[midiChannel] & 0x7f;
            } else {
                pitch += 36;
                switch (octaveShifts[midiChannel]) {
                default:
                case 0:
                    return pitch;
                case 1:
                    return pitch + 12;
                case 2:
                    return pitch + 24;
                case 3:
                    return pitch + 36;
                case 4:
                    return pitch + 48;
                case 0x81:
                    return pitch - 12;
                case 0x82:
                    return pitch - 24;
                case 0x83:
                    return pitch - 36;
                case 0x84:
                    return pitch - 48;
                }
            }
        } else {
            return pitch;
        }
    }

    /**
     * HandyPhoneStandard only.
     * index is pseudo MIDI channel
     * <pre>
     *  Value        | Description
     * --------------+-----------------
     *  0x00         | No Shift(Original)
     *  0x01         | +1 Octave
     *  0x02         | +2 Octave
     *  0x03         | +3 Octave
     *  0x04         | +4 Octave
     *  0x05 ～ 0x80 | Reserved
     *  0x81         | -1 Octave
     *  0x82         | -2 Octave
     *  0x83         | -3 Octave
     *  0x84         | -4 Octave
     *  0x85 ～ 0xFF | Reserved
     * </pre>
     * @see OctaveShiftMessage
     */
    private int[] octaveShifts = new int[MAX_MIDI_CHANNELS];

    /**
     * @param smafChannel
     * @param octaveShift
     * @see OctaveShiftMessage
     * @see #octaveShifts
     */
    public void setOctaveShift(int smafChannel, int octaveShift) {
        int midiChannel = getMidiChannel(smafChannel);
        octaveShifts[midiChannel] = octaveShift;
//Debug.println("octaveShifts[" + midiChannel + "]: " + octaveShift);
    }

    /**
     * @param smafChannel
     * @param velocity
     * @see NoteMessage
     * @see #velocities
     */
    public int setVelocity(int smafChannel, int velocity) {
        int midiChannel = getMidiChannel(smafChannel);
        velocities[midiChannel] = velocity;
//Debug.println("velocities[" + mididChannel + "]: " + octaveShift);
        return velocity; // TODO う～ん
    }

    /**
     * @param smafChannel
     * @see NoteMessage
     * @see #velocities
     */
    public int getVelocity(int smafChannel) {
        int midiChannel = getMidiChannel(smafChannel);
        return velocities[midiChannel];
    }

    //----

    /**
     * index is MA1レジスタ値
     * TODO どうつかうの？
     * @see "SscMA1_Gl110-j.pdf"
     */
    private static final int[] tempoTable = {
        // 0
        -1, -1, -1, -1, 437, 364, 312, 273,
        // 8
        243, 218, 199, 182, 168, 156, 146, 137,
        // 16
        129, 121, 115, 109, 104, 99, 95, 91,
        // 24
        87, 84, 81, 78, 75, 73, 70, 68,
        // 32
        66, 64, 62, 61, 59, 57, 56, 55,
        // 40
        53, 52, 51, 50, 49, 47, 46, -1,
        // 48
        45, 44, 43, 42, 41, -1, 40, 39,
        // 56
        38, -1, 37, -1, 36, 35, -1, 34,
        // 64
        -1, 33, -1, 32, -1, 31, -1, -1,
        // 72
        30, -1, 29, -1, -1, 28, -1, -1,
        // 80
        27, -1, -1, 26, -1, -1, 25, -1,
        // 88
        -1, -1, 24, -1, -1, -1, 23, -1,
        // 96
        -1, -1, 22, -1, -1, -1, -1, 21,
        // 104
        -1, -1, -1, -1, 20, -1, -1, -1,
        // 112
        -1, -1, 19, -1, -1, -1, -1, -1,
        // 120
        18, -1, -1, -1, -1, -1, -1, -1,
        // 128
        17, -1, -1, -1, -1, -1, -1, -1,
        // 136
        16, -1, -1, -1, -1, -1, -1, -1,
        // 144
        -1, 15, -1, -1, -1, -1, -1, -1,
        // 152
        -1, -1, -1, 14, -1, -1, -1, -1,
        // 160
        -1, -1, -1, -1, -1, -1, -1, 13 /* 167 */,
        // 168
        -1, -1, -1, -1, -1, -1, -1, -1,
        // 176
        -1, -1, -1, -1, -1, 12 /* 181 */, -1, -1,
        // 184
        -1, -1, -1, -1, -1, -1, -1, -1,
        // 192
        -1, -1, -1, -1, -1, -1, 11 /* 198 */, -1,
        // 200
        -1, -1, -1, -1, -1, -1, -1, -1,
        // 208
        -1, -1, -1, -1, -1, -1, -1, -1,
        // 216
        -1, 10 /* 217 */, -1, -1, -1, -1, -1, -1,
        // 224
        -1, -1, -1, -1, -1, -1, -1, -1,
        // 232
        -1, -1, -1, -1, -1, -1, -1, -1,
        // 240
        -1, -1, 9 /* 242 */, -1, -1, -1, -1, -1,
        // 248
        -1, -1, -1, -1, -1, -1, -1, -1
    };

    /** */
    static {
Debug.println("tempoTable: " + tempoTable.length);    
    }

    /** テンポの指定がない場合、SSDは 4 分音符 = 120 として扱います */
    private static final int tempo = 120;

    /** */
    private int timeBase = 2;

    /**
     * @return Returns the ticks.
     * @see #timeBase
     */
    public long getTicksOf(long gateTime) {
        return gateTime * timeBase;
    }

    /**
     * @see #tempo
     * @see #timeBase
     */
    public MidiEvent getTempoEvent() throws InvalidMidiDataException {
        int l = tempo * timeBase * 1000;
//      int l = (int) Math.round(60d * 1000000d / tempo);
        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(
            0x51,
            new byte[] {
                (byte)  ((l / 0x10000) & 0xff),
                (byte) (((l % 0x10000) / 0x100) & 0xff),
                (byte)  ((l % 0x100)   & 0xff) },
            3);
        return new MidiEvent(metaMessage, currentTicks[0]);
    }

    /**
     * @param smafTracks smaf tracks
     * @return resolution for MIDI
     * @see #tempo
     * @see #timeBase
     */
    public int getResolution(Track[] smafTracks)
        throws InvalidSmafDataException {

        if (smafTracks.length == 0) {
            throw new InvalidSmafDataException("no tracks");
        }

int t = 0;
        for (Track track : smafTracks) {
            for (int i = 0; i < track.size(); i++) {
                SmafEvent event = track.get(i);
                SmafMessage message = event.getMessage();
                if (message instanceof vavi.sound.smaf.MetaMessage) {
                    vavi.sound.smaf.MetaMessage metaMessage = (vavi.sound.smaf.MetaMessage) message;
                    if (metaMessage.getType() == MidiConstants.META_MACHINE_DEPEND) {
                        this.timeBase = (Integer) metaMessage.getData().get("durationTimeBase"); // [ms]
Debug.println("timebase: " + timeBase + ", (" + t + ":" + i + ")");
                        return tempo * timeBase;
                    }
                }
            }
t++;
        }

Debug.println("no tempo message in track 0");
        return 120;
    }
}

/* */
