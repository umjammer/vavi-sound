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
 * ExpressionMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class ExpressionMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible, SmafConvertible {

    /** smaf channel */
    private int channel;

    /** 0 ~ 127 */
    private int volume;

    /**
     *
     * @param duration
     * @param channel smaf channel
     * @param value expression 0 ~ 127
     */
    public ExpressionMessage(int duration, int channel, int value) {
        this.duration = duration;
        this.channel = channel;
        this.volume  =  value & 0x7f;
    }

    /** for SmafConvertible */
    protected ExpressionMessage() {
    }

    /** */
    public int getVolume() {
        return volume;
    }

    /** */
    public void setVolume(int volume) {
        this.volume = volume & 0x7f;
    }

    /** */
    public int getChannel() {
        return channel;
    }

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public String toString() {
        return "Expression:" +
            " duration=" + duration +
            " channel=" + channel +
            " volume="  + volume;
    }

    //----

    /* */
    public byte[] getMessage() {
        return null; // TODO
    }

    /* */
    public int getLength() {
        return 0;   // TODO
    }

    /** */
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int midiChannel = context.retrieveChannel(this.channel);

        MidiEvent[] events = new MidiEvent[1];
        ShortMessage shortMessage = new ShortMessage();
//Debug.println("Expression: [" + duration + "] " + channel + "ch, " + volume);
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                midiChannel,
                                11,    // エクスプレッション MSB
                                volume);
        events[0] = new MidiEvent(shortMessage, context.getCurrentTick());
        return events;
    }

    /** */
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context)
        throws InvalidSmafDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();

        int track = context.retrieveSmafTrack(channel);
        int voice = context.retrieveVoice(channel);

        ExpressionMessage smafMessage = new ExpressionMessage();
        smafMessage.setDuration(context.getDuration());
        smafMessage.setChannel(voice);
        smafMessage.setVolume(shortMessage.getData2());

        context.setBeforeTick(track, midiEvent.getTick());

        return new SmafEvent[] {
            new SmafEvent(smafMessage, midiEvent.getTick())
        };
    }
}

/* */
