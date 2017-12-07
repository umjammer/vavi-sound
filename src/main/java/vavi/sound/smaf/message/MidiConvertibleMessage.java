/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;


/**
 * MidiConvertibleMessage.
 * <p>
 * CONTROL_CHANGE をすべて扱う
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050501 nsano initial version <br>
 */
public class MidiConvertibleMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible {

    /** smaf channel */
    private int channel;

    /** */
    private int command;

    /** */
    private int value;

    /**
     * @param duration
     * @param command
     * @param channel smaf channel
     * @param value
     */
    public MidiConvertibleMessage(int duration, int command, int channel, int value) {
        this.duration = duration;
        this.command = command;
        this.channel = channel;
        this.value = value;
    }

    /**
     * @return Returns the command.
     */
    public int getCommand() {
        return command;
    }

    /**
     * @return Returns the channel.
     */
    public int getChannel() {
        return channel;
    }

    /**
     * @return Returns the value.
     */
    public int getValue() {
        return value;
    }

    /** */
    public String toString() {
        return "MidiConvertible:" +
            " duration=" + duration +
            " command=" + command +
            " channel=" + channel +
            " value=" + value;
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
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
        int midiChannel = context.retrieveChannel(this.channel);

        MidiEvent[] events = new MidiEvent[1];
        ShortMessage shortMessage = new ShortMessage();
//Debug.println("(" + StringUtil.toHex2(command) + "): " + channel + "ch, " + StringUtil.toHex2(value));
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                midiChannel,
                                command,
                                value);
        events[0] = new MidiEvent(shortMessage, context.getCurrentTick());
        return events;
    }
}

/* */
