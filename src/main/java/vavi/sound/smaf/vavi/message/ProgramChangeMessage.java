/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.mobile.YamahaExclusive;
import vavi.sound.smaf.vavi.message.MidiContext.ChannelConfiguration;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.vavi.chunk.TrackChunk.FormatType;


/**
 * ProgramChangeMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class ProgramChangeMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible, SmafConvertible {

    /** smaf channel */
    private int channel;

    /** GM? */
    private int program;

    /**
     * Creates ProgramChangeMessage.
     * @param duration
     * @param channel smaf channel
     * @param value program
     */
    public ProgramChangeMessage(int duration, int channel, int value) {
        this.duration = duration;
        this.channel = channel;
        this.program = value & 0x7f;
    }

    /** for SmafConvertible */
    public ProgramChangeMessage() {
    }

    /** */
    public int getProgram() {
        return program;
    }

    /** */
    public void setProgram(int program) {
        this.program = program & 0x7f;
    }

    /** */
    public int getChannel() {
        return channel;
    }

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    @Override
    public String toString() {
        return "ProgramChange:" +
            " duration=" + duration +
            " channel=" + channel +
            " program=" + program;
    }

    // ----

    @Override
    public byte[] getMessage() {
        return HandyPhoneStandard.control(duration, channel, 0x00, program);
    }

    @Override
    public int getLength() {
        return getMessage().length;
    }

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int midiChannel = context.setProgram(this.channel, this.program);
        // for a synthesizer which plays the file's own voices the program of a drum channel is
        // the drum kit (Bank_Program3 of the MA-3 driver), see StreamExclusive#isEnabled
        int program = midiChannel == MidiContext.CHANNEL_DRUM && !YamahaExclusive.isEnabled() ? 0 : context.getProgram(this.channel);

//logger.log(Level.TRACE, "ProgramChange: [" + duration + "] " + channel + "ch, " + context.getProgram(channel));
        if (context.getFormatType() == FormatType.HandyPhoneStandard &&
            context.getDrum(this.channel) == ChannelConfiguration.PERCUSSION &&
            context.getSmafTrackNumber() * 4 + this.channel != MidiContext.CHANNEL_DRUM) {
            return null;
        } else {
            ShortMessage shortMessage = new ShortMessage();
            shortMessage.setMessage(ShortMessage.PROGRAM_CHANGE,
                                    midiChannel,
                                    program,
                                    0);
            return new MidiEvent[] {
                new MidiEvent(shortMessage, context.getCurrentTick())
            };
        }
    }

    @Override
    public boolean accept(String key) {
        return "short.192".equals(key);
    }

    /**
     * A percussion channel has no program of its own, the program selects the drum sound of
     * each single note there.
     * @see NoteMessage#getSmafEvents
     */
    @Override
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context)
        throws InvalidSmafDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int data1 = shortMessage.getData1();

        int track = context.retrieveSmafTrack(channel);
        int voice = context.retrieveVoice(channel);

        if (context.isPercussion(channel)) {
            return null;
        }

        ProgramChangeMessage changeVoiceMessage = new ProgramChangeMessage();
        changeVoiceMessage.setDuration(context.getDuration(track));
        changeVoiceMessage.setChannel(voice);
        changeVoiceMessage.setProgram(data1);

        context.setBeforeTick(track, midiEvent.getTick());

        return new SmafEvent[] {
            new SmafEvent(changeVoiceMessage, midiEvent.getTick())
        };
    }
}
