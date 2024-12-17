/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.Track;
import vavi.sound.smaf.chunk.ChannelStatus;
import vavi.sound.smaf.chunk.ScoreTrackChunk;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;

import static java.lang.System.getLogger;


/**
 * MIDI Context for a converter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class MidiContext {

    private static final Logger logger = getLogger(MidiContext.class.getName());

    /** Max MIDI channels */
    public static final int MAX_MIDI_CHANNELS = 16;

    /** channel configuration */
    public enum ChannelConfiguration {
        /** rhythm */
        PERCUSSION,
        /** others */
        SOUND_SET,
        /** unused */
        UNUSED
    }

    /** channel 9 defaults to rhythm */
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
            if (message instanceof vavi.sound.smaf.MetaMessage metaMessage) {
                if (metaMessage.getType() == MetaEvent.META_MACHINE_DEPEND.number()) {
                    //
                    this.formatType = (FormatType) metaMessage.getData().get("formatType"); // [ms]
logger.log(Level.DEBUG, "formatType: " + formatType);
                    for (int i = 0; i < MAX_MIDI_CHANNELS; i++) {
                        if (formatType == FormatType.HandyPhoneStandard) {
                            velocities[i] = 0x7f;
                        } else {
                            velocities[i] = 64;
                        }
                    }
                    //
                    ChannelStatus[] channelStatuses = (ChannelStatus[]) metaMessage.getData().get("channelStatuses");
logger.log(Level.DEBUG, "channelStatuses: " + (channelStatuses != null ? channelStatuses.length : null));
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
    private static ChannelConfiguration toChannelConfiguration(int midiChannel, ChannelStatus.Type type) {
        return switch (type) {
            case Melody -> ChannelConfiguration.SOUND_SET;
            case NoCare -> midiChannel == CHANNEL_DRUM ? ChannelConfiguration.PERCUSSION : ChannelConfiguration.SOUND_SET;
            case Rhythm -> ChannelConfiguration.PERCUSSION;
            default -> ChannelConfiguration.UNUSED;
        };
    }

    /** current track no. */
    private int smafTrackNumber;

    /** */
    public int getSmafTrackNumber() {
        return smafTrackNumber;
    }

    /** */
    public void setSmafTrackNumber(int smafTrackNumber) {
        this.smafTrackNumber = smafTrackNumber;
    }

    /** current ticks, index is smafTrackNumber */
    private final long[] currentTicks = new long[4];

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

    /** whether channel is for rhythm, index is pseudo MIDI channel */
    private final ChannelConfiguration[] drums = new ChannelConfiguration[MAX_MIDI_CHANNELS];

    /** index is pseudo MIDI channel */
    private final int[] velocities = new int[MAX_MIDI_CHANNELS];

    /* init */ {
        Arrays.fill(drums, ChannelConfiguration.UNUSED);
    }

    /** */
    private static final int CHANNEL_UNUSED = -1;

    /** replacement channel if DRUM_CHANNEL is not a rhythm */
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
logger.log(Level.DEBUG, "already swapped: " + midiChannel + ", " + value);
        } else {
            drums[midiChannel] = value;
//logger.log(Level.TRACE, "temporary: " + midiChannel + ", " + value);
        }

        // if DRUM_CHANNEL is not a rhythm, replace it with an empty channel.
        if (midiChannel == CHANNEL_DRUM && drums[CHANNEL_DRUM] == ChannelConfiguration.SOUND_SET && drumSwapChannel == CHANNEL_UNUSED) {
            for (int k = MAX_MIDI_CHANNELS - 1; k >= 0; k--) {
                if (k != CHANNEL_DRUM && drums[k] == ChannelConfiguration.UNUSED) {
                    drumSwapChannel = k;
logger.log(Level.DEBUG, "channel 9 -> " + k);
                    break;
                }
            }
if (drumSwapChannel == CHANNEL_UNUSED) {
 logger.log(Level.DEBUG, "cannot swap: " + midiChannel + ", " + value);
}
logger.log(Level.DEBUG, "channel configuration: " + midiChannel + "ch, " + drums[midiChannel]);
        }
if (value != ChannelConfiguration.UNUSED) {
StringBuilder sb1 = new StringBuilder(16);
StringBuilder sb2 = new StringBuilder(16);
StringBuilder sb3 = new StringBuilder(16);
for (int i = 0; i < drums.length; i++) {
 sb1.append(drumSwapChannel != CHANNEL_UNUSED && drumSwapChannel == i ? "@" : "%1x".formatted(i));
 sb2.append(midiChannel == i ? "*" : " ");
 sb3.append(drums[i].name().charAt(0));
}
logger.log(Level.DEBUG, "drums: " + midiChannel + "ch, " + value + "\n" + sb1 + "\n" + sb2 + "\n" + sb3);
}
    }

    /** program no, index is pseudo MIDI channel assigned to channel */
    private final int[] programs = new int[MAX_MIDI_CHANNELS];

    /**
     * @param smafChannel SMAF channel
     * @return pseudo MIDI channel
     */
    private int getMidiChannel(int smafChannel) {
        if (formatType == ScoreTrackChunk.FormatType.HandyPhoneStandard) {
            return smafTrackNumber * 4 + smafChannel;
        } else {
if (smafTrackNumber > 0 || smafChannel > 16) {
 logger.log(Level.DEBUG, "track > 0: " + smafTrackNumber + ", or smafChannel > 16: " + smafChannel);
}
            return smafTrackNumber * 16 + smafChannel % 16; // TODO smafChannel > 16
        }
    }

    /**
     * @param smafChannel SMAF channel
     * @return channel after drum replacement (real MIDI channel)
     */
    public int setProgram(int smafChannel, int program) {
        int midiChannel = getMidiChannel(smafChannel);

        if (formatType != FormatType.HandyPhoneStandard) {
            if (midiChannel != drumSwapChannel && drums[midiChannel] == ChannelConfiguration.PERCUSSION) {
logger.log(Level.DEBUG, "drum always zero:[" + midiChannel + "]: " + program);
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

    // ---- note

    /**
     * @param smafChannel SMAF channel
     * @return channel after drum replacement (real MIDI channel)
     * @see #drums
     */
    public int retrieveChannel(int smafChannel) {
        int midiChannel = getMidiChannel(smafChannel);

//        if (midiChannel == drumSwapChannel) {
//logger.log(Level.TRACE, "used swapped channel: " + midiChannel);
//        }

        // drum channel is used as sound
        if (midiChannel == CHANNEL_DRUM && drums[CHANNEL_DRUM] == ChannelConfiguration.SOUND_SET) {
            midiChannel = drumSwapChannel;
        }

        // all percussion specifications go to MIDI drum channels
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
//logger.log(Level.TRACE, "drum pitch: " + (programs[midiChannel] & 0x7f) + ", " + pitch);
                return programs[midiChannel] & 0x7f;
            } else {
                pitch += 36;
                return switch (octaveShifts[midiChannel]) {
                    default -> pitch;
                    case 1 -> pitch + 12;
                    case 2 -> pitch + 24;
                    case 3 -> pitch + 36;
                    case 4 -> pitch + 48;
                    case 0x81 -> pitch - 12;
                    case 0x82 -> pitch - 24;
                    case 0x83 -> pitch - 36;
                    case 0x84 -> pitch - 48;
                };
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
     *  0x05  ~  0x80 | Reserved
     *  0x81         | -1 Octave
     *  0x82         | -2 Octave
     *  0x83         | -3 Octave
     *  0x84         | -4 Octave
     *  0x85  ~  0xFF | Reserved
     * </pre>
     * @see OctaveShiftMessage
     */
    private final int[] octaveShifts = new int[MAX_MIDI_CHANNELS];

    /**
     * @param smafChannel channel
     * @param octaveShift octave shift
     * @see OctaveShiftMessage
     * @see #octaveShifts
     */
    public void setOctaveShift(int smafChannel, int octaveShift) {
        int midiChannel = getMidiChannel(smafChannel);
        octaveShifts[midiChannel] = octaveShift;
//logger.log(Level.TRACE, "octaveShifts[" + midiChannel + "]: " + octaveShift);
    }

    /**
     * @param smafChannel channel
     * @param velocity velocity
     * @see NoteMessage
     * @see #velocities
     */
    public int setVelocity(int smafChannel, int velocity) {
        int midiChannel = getMidiChannel(smafChannel);
        velocities[midiChannel] = velocity;
//logger.log(Level.TRACE, "velocities[" + mididChannel + "]: " + octaveShift);
        return velocity; // TODO mhh...
    }

    /**
     * @param smafChannel channel
     * @see NoteMessage
     * @see #velocities
     */
    public int getVelocity(int smafChannel) {
        int midiChannel = getMidiChannel(smafChannel);
        return velocities[midiChannel];
    }

    // ----

    /**
     * index is MA1 register value
     * TODO how to use it?
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

    /* */
    static {
logger.log(Level.TRACE, "tempoTable: " + tempoTable.length);
    }

    /** if no tempo is specified, SSD will treat it as a quarter note = 120 */
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
logger.log(Level.INFO, "tempo: " + l);
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
                if (message instanceof vavi.sound.smaf.MetaMessage metaMessage) {
                    if (metaMessage.getType() == MetaEvent.META_MACHINE_DEPEND.number()) {
                        this.timeBase = (Integer) metaMessage.getData().get("durationTimeBase"); // [ms]
logger.log(Level.DEBUG, "timebase: " + timeBase + ", (" + t + ":" + i + ")");
                        return tempo * timeBase;
                    }
                }
            }
t++;
        }

logger.log(Level.DEBUG, "no tempo message in track 0");
        return 120;
    }
}
