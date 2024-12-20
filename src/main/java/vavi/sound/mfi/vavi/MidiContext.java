/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.Track;
import vavi.sound.mfi.vavi.track.TempoMessage;

import static java.lang.System.getLogger;


/**
 * MIDI Context for the converter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030821 nsano initial version <br>
 *          0.01 020826 nsano add pitch bend related <br>
 *          0.02 031214 nsano add resolution related <br>
 */
public class MidiContext {

    private static final Logger logger = getLogger(MidiContext.class.getName());

    /** */
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

//    /** TODO for saving when there are multiple rhythm channels */
//    drumProgram = 0;

    /** current track number */
    private int mfiTrackNumber;

    /** */
    public int getMfiTrackNumber() {
        return mfiTrackNumber;
    }

    /** */
    public void setMfiTrackNumber(int trackNumber) {
        this.mfiTrackNumber = trackNumber;
    }

    /** current Δ time */
    private final long[] currents = new long[4];

    /** mfiTrackNumber must be set */
    public long getCurrent() {
        return currents[mfiTrackNumber];
    }

    /** mfiTrackNumber must be set */
    public void setCurrent(long current) {
        this.currents[mfiTrackNumber] = current;
    }

    /** mfiTrackNumber must be set */
    public void addCurrent(long value) {
        this.currents[mfiTrackNumber] += value;
    }

    /** whether channel is a rhythm, index is pseudo MIDI channel */
    private final ChannelConfiguration[] drums = new ChannelConfiguration[MAX_MIDI_CHANNELS];

    /* initializing */ {
        Arrays.fill(drums, ChannelConfiguration.UNUSED);
    }

    /** */
    private static final int CHANNEL_UNUSED = -1;

    /** destination channel if DRUM_CHANNEL is not rhythm */
    private int drumSwapChannel = CHANNEL_UNUSED;

    /**
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     */
    public void setDrum(int channel, ChannelConfiguration value) {
        if (drumSwapChannel != CHANNEL_UNUSED && channel == drumSwapChannel) {
logger.log(Level.DEBUG, "already swapped: " + channel + ", " + value);
        } else {
            drums[channel] = value;
        }

        // ff DRUM_CHANNEL is not a rhythm, replace it with an empty channel
        if (channel == CHANNEL_DRUM && drums[CHANNEL_DRUM] == ChannelConfiguration.SOUND_SET && drumSwapChannel == CHANNEL_UNUSED) {
            for (int k = MAX_MIDI_CHANNELS - 1; k >= 0; k--) {
                if (k != CHANNEL_DRUM && drums[k] == ChannelConfiguration.UNUSED) {
                    drumSwapChannel = k; // TODO support multiple？
logger.log(Level.DEBUG, "channel 9 -> " + k);
                    break;
                }
            }
if (drumSwapChannel == CHANNEL_UNUSED) {
logger.log(Level.DEBUG, "cannot swap: " + channel + ", " + value);
}
        }
    }

    /** volumes assigned to channel, index is pseudo MIDI channel */
    private final int[] volumes = new int[MAX_MIDI_CHANNELS];

    /**
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     */
    public void addVolume(int channel, int value) {
        volumes[channel] += value;
    }

    /**
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     */
    public void setVolume(int channel, int value) {
        volumes[channel] = value;
    }

    /*
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     */
    public int getVolume(int channel) {
        return volumes[channel];
    }

    /** program numbers assigned to channel, index is real MIDI channel */
    private final int[] programs = new int[MAX_MIDI_CHANNELS];

    /**
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     * @return channel after drum replacement (real MIDI channel)
     */
    public int setProgram(int channel, int program) {
        if (channel != drumSwapChannel && drums[channel] == ChannelConfiguration.PERCUSSION) {
logger.log(Level.DEBUG, "drum always zero:[" + channel + "]: " + program);
            program = 0;
        }

        channel = retrieveChannel(channel);

        programs[channel] = (programs[channel] & 0x40) | program;

        return channel;
    }

    /**
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     * @return channel after drum replacement (real MIDI channel)
     */
    public int setBank(int channel, int bank) {
        if (channel != drumSwapChannel && drums[channel] == ChannelConfiguration.PERCUSSION) {
logger.log(Level.DEBUG, "drum always zero:[" + channel + "]: " + bank);
            bank = 0;
        }

        channel = retrieveChannel(channel);

        bank = (bank & 0x01) << 6;

        programs[channel] = (programs[channel] & 0x3f) | bank;

        return channel;
    }

    /**
     * @param channel real MIDI channel
     */
    public int getProgram(int channel) {
//logger.log(Level.TRACE, "program[" + channel + "]: " + programs[channel] + " (0x" + StringUtil.toHex2(programs[channel]) + ")");
        return programs[channel];
    }

    // ---- note

    /**
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     * @return channel after drum replacement (real MIDI channel)
     */
    public int retrieveChannel(int channel) {

        if (channel == CHANNEL_DRUM && drums[CHANNEL_DRUM] == ChannelConfiguration.SOUND_SET) {
            channel = drumSwapChannel;
        }

        if (drums[channel] == ChannelConfiguration.PERCUSSION) {
            channel = CHANNEL_DRUM;
        }

        return channel;
    }

    /**
     * sound +45, percussion +35
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     * @param pitch MFi pitch
     * @return MIDI pitch
     */
    public int retrievePitch(int channel, int pitch) {
        if (drums[channel] == ChannelConfiguration.PERCUSSION) {
            pitch -= 10; // TODO spec
        }

        return pitch + 45;
    }

    // ----

    /** -32 ~ 31, index is pseudo MIDI channel */
    private final int[] pitchBends = new int[MAX_MIDI_CHANNELS];
    /** -32 ~ 31, index is pseudo MIDI channel */
    private final int[] finePitchBends = new int[MAX_MIDI_CHANNELS];
    /** 0 ~ 24, index is pseudo MIDI channel */
    private final int[] pitchBendRanges = new int[MAX_MIDI_CHANNELS];

    /* initializing */ {
        for (int i = 0; i < MAX_MIDI_CHANNELS; i++) {
            pitchBends[i] = 16;
            finePitchBends[i] = 16;
            pitchBendRanges[i] = 2;
        }
    }

    /** */
    public void setPitchBend(int channel, int pitchBend) {
        pitchBends[channel] = pitchBend;
    }

    /** */
    public void setFinePitchBend(int channel, int finePitchBend) {
        finePitchBends[channel] = finePitchBend;
    }

    /** */
    public void setPitchBendRange(int channel, int pitchBendRange) {
        pitchBendRanges[channel] = pitchBendRange;
    }

    /** TODO unused */
    public int retrieveRealPitch(int channel) {
        int pb = pitchBends[channel];
        int fpb = finePitchBends[channel];
        int rg = pitchBendRanges[channel];

        int pitch =
//          (int) ((pb * rgb * 100f / 32f) + ((fpb * rgb * 100f) / (32f * 32f)));
            (pb * rg * 100 / 32) + ((fpb * rg * 100) / (32 * 32)) / 20;
logger.log(Level.DEBUG, "pitch[" + channel + "]: " + pitch);
        return pitch;
    }

    // ----

    /**
     * use with PPQ
     * @param mfiTracks mfi tracks
     * @return time base
     */
    public int getResolution(Track[] mfiTracks)
        throws InvalidMfiDataException {

        if (mfiTracks.length == 0) {
            throw new InvalidMfiDataException("no tracks");
        }

        Track track = mfiTracks[0];
        for (int j = 0; j < track.size(); j++) {
            MfiEvent event = track.get(j);
            MfiMessage message = event.getMessage();

            if (message instanceof TempoMessage) {
                return ((TempoMessage) message).getTimeBase();
            }
        }

logger.log(Level.INFO, "no tempo message in track 0");
        return 48; // MFi default time base
    }
}
