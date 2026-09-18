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
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.ChannelMessage;
import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.Track;
import vavi.sound.mfi.vavi.sequencer.MfiValueExclusive;
import vavi.sound.mfi.vavi.track.TempoMessage;

import static java.lang.System.getLogger;


/**
 * MIDI Context for the converter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030821 nsano initial version <br>
 *          0.01 020826 nsano add pitch bend related <br>
 *          0.02 031214 nsano add resolution related <br>
 *          0.03 260911 nsano add {@link #toProgram(int, int)} <br>
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

    /**
     * the MIDI channel a melody on {@link #CHANNEL_DRUM} goes to, chosen when first needed,
     * {@link #CHANNEL_UNUSED} until then
     */
    private int drumSwapChannel = CHANNEL_UNUSED;

    /** whether a channel message of the sequence is on the channel, index is pseudo MIDI channel */
    private final boolean[] usedChannels = new boolean[MAX_MIDI_CHANNELS];

    /**
     * Finds the channels the sequence uses, which a melody on {@link #CHANNEL_DRUM} must not be
     * moved to. An audio channel is not a MIDI one.
     */
    public void setTracks(Track[] mfiTracks) {
        for (int t = 0; t < mfiTracks.length; t++) {
            for (int i = 0; i < mfiTracks[t].size(); i++) {
                MfiMessage message = mfiTracks[t].get(i).getMessage();
                if (message instanceof ChannelMessage channelMessage && !message.getClass().getSimpleName().startsWith("Audio")) {
                    int channel = channelMessage.getVoice() + 4 * t;
                    if (channel < MAX_MIDI_CHANNELS) {
                        usedChannels[channel] = true;
                    }
                }
            }
        }
    }

    /**
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     */
    public void setDrum(int channel, ChannelConfiguration value) {
        drums[channel] = value;
    }

    /**
     * The MIDI channel a melody on {@link #CHANNEL_DRUM} goes to: one the sequence has no
     * channel message on, or else a percussion one, whose own MIDI channel is left as its
     * messages all go to {@link #CHANNEL_DRUM}.
     */
    private int drumSwapChannel() {
        if (drumSwapChannel == CHANNEL_UNUSED) {
            for (int k = MAX_MIDI_CHANNELS - 1; k >= 0 && drumSwapChannel == CHANNEL_UNUSED; k--) {
                if (k != CHANNEL_DRUM && !usedChannels[k]) {
                    drumSwapChannel = k;
                }
            }
            for (int k = MAX_MIDI_CHANNELS - 1; k >= 0 && drumSwapChannel == CHANNEL_UNUSED; k--) {
                if (k != CHANNEL_DRUM && drums[k] == ChannelConfiguration.PERCUSSION) {
                    drumSwapChannel = k;
                }
            }
            if (drumSwapChannel == CHANNEL_UNUSED) {
logger.log(Level.DEBUG, "cannot swap, the melody stays on " + CHANNEL_DRUM);
                return CHANNEL_DRUM;
            }
logger.log(Level.DEBUG, "channel " + CHANNEL_DRUM + " -> " + drumSwapChannel);
        }
        return drumSwapChannel;
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
        if (drums[channel] == ChannelConfiguration.PERCUSSION) {
logger.log(Level.DEBUG, "drum always zero:[" + channel + "]: " + program);
            program = 0;
        }

        channel = retrieveChannel(channel);

        programs[channel] = (programs[channel] & 0x40) | program;

        return channel;
    }

    /**
     * The MIDI program an MFi bank / program pair ends up as.
     * <p>
     * {@link #setBank(int, int)} and {@link #setProgram(int, int)} write the two
     * halves of the same 7 bit program number, so a voice registered for an MFi
     * (bank, program) has to be registered at this MIDI program to be found
     * again. Note that only bit 0 of the 6 bit MFi bank survives, so two banks
     * whose numbers differ above bit 0 collide.
     * </p>
     */
    public static int toProgram(int bank, int program) {
        return ((bank & 0x01) << 6) | (program & 0x3f);
    }

    /**
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     * @return channel after drum replacement (real MIDI channel)
     */
    public int setBank(int channel, int bank) {
        if (drums[channel] == ChannelConfiguration.PERCUSSION) {
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
     * Where every channel message of the channel goes: {@link #CHANNEL_DRUM} is kept for the
     * percussion ones, which are all gathered there, a melody one on it goes elsewhere.
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     * @return channel after drum replacement (real MIDI channel)
     */
    public int retrieveChannel(int channel) {
        if (drums[channel] == ChannelConfiguration.PERCUSSION) {
            return CHANNEL_DRUM;
        }
        if (channel == CHANNEL_DRUM) {
            return drumSwapChannel();
        }
        return channel;
    }

    /**
     * Where a message {@link #retrieveChannel} moved came from, for a synthesizer of an mfi
     * sound source, which plays the channels of the file as they are.
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     * @param tick the tick of the message moved
     * @return the {@link MfiValueExclusive#CHANNEL} exclusive to go right before the message,
     *         nothing if the channel is not moved
     */
    public MidiEvent[] origin(int channel, long tick) throws InvalidMidiDataException {
        int midiChannel = retrieveChannel(channel);
        if (midiChannel == channel) {
            return new MidiEvent[0];
        }
        return new MidiEvent[] {
            new MidiEvent(MfiValueExclusive.message(MfiValueExclusive.CHANNEL, midiChannel, channel), tick)
        };
    }

    /**
     * The events of the channel's messages, which are on {@link #retrieveChannel} of it
     * already, each with {@link #origin} right before it when the channel is moved.
     * @param channel pseudo MIDI channel (mfiTrackNumber * 4 + voice)
     */
    public MidiEvent[] withOrigins(int channel, MidiEvent... events) throws InvalidMidiDataException {
        if (retrieveChannel(channel) == channel) {
            return events;
        }
        List<MidiEvent> result = new ArrayList<>();
        for (MidiEvent event : events) {
            result.addAll(Arrays.asList(origin(channel, event.getTick())));
            result.add(event);
        }
        return result.toArray(MidiEvent[]::new);
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
                resolution = ((TempoMessage) message).getTimeBase();
                return resolution;
            }
        }

logger.log(Level.INFO, "no tempo message in track 0");
        resolution = 48; // MFi default time base
        return resolution;
    }

    /** the midi resolution {@link #getResolution(Track[])} decided, 0 before it */
    private int resolution;

    /** @return the midi resolution, 0 if not decided yet */
    public int getResolution() {
        return resolution;
    }
}
