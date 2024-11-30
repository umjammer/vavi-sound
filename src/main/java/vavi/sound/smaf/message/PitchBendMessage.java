/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;


/**
 * PitchBendMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class PitchBendMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible, SmafConvertible {

    /** smaf channel */
    private int channel;

    /** 14 Bit */
    private int pitchBend;

    /**
     * Creates PitchBendMessage.
     * @param duration
     * @param channel smaf channel
     * @param value MIDI pitchBend 14 Bit
     */
    public PitchBendMessage(int duration, int channel, int value) {
        this.duration = duration;
        this.channel = channel;
        this.pitchBend = value;
//logger.log(Level.TRACE, "pitchBend: " + value);
    }

    /** for SmafConvertible */
    protected PitchBendMessage() {
    }

    /** */
    public int getPitchBend() {
        return pitchBend;
    }

    /** @param pitchBend 14 bit */
    public void setPitchBend(int pitchBend) {
        this.pitchBend = pitchBend & 0x3fff;
    }

    /** */
    public int getChannel() {
        return channel;
    }

    /** @param channel 0x00 ~ 0x03 */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public String toString() {
    return "PitchBend:" +
        " duration=" + duration +
        " channel=" + channel +
        " pitchBend=" + pitchBend;
    }

    // ----

    /* */
    @Override
    public byte[] getMessage() {
        return null; // TODO
    }

    /* */
    @Override
    public int getLength() {
        return 0;   // TODO
    }

    /**
     * because PsmPlayer converted it like this.
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int midiChannel = context.retrieveChannel(this.channel);

        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.PITCH_BEND,
                                midiChannel,
                                pitchBend & 0x7f,   // LSB
                                pitchBend >> 7);    // MSB
        return new MidiEvent[] {
            new MidiEvent(shortMessage, context.getCurrentTick())
        };
    }

    @Override
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context)
        throws InvalidSmafDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int data2 = shortMessage.getData2();

        int track = context.retrieveSmafTrack(channel);
        int voice = context.retrieveVoice(channel);

        PitchBendMessage smafMessage = new PitchBendMessage();
        smafMessage.setDuration(context.getDuration());
        smafMessage.setChannel(voice);
        smafMessage.setPitchBend(data2);

        context.setBeforeTick(track, midiEvent.getTick());

        return new SmafEvent[] {
            new SmafEvent(smafMessage, midiEvent.getTick())
        };
    }
}
