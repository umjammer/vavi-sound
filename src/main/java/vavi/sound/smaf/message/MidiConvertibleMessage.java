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
 * handle all CONTROL_CHANGE
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050501 nsano initial version <br>
 */
public class MidiConvertibleMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible {

    /** smaf channel */
    private final int channel;

    /** */
    private final int command;

    /** */
    private final int value;

    /**
     * @param duration duration
     * @param command command
     * @param channel smaf channel
     * @param value value
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

    @Override
    public String toString() {
        return "MidiConvertible:" +
            " duration=" + duration +
            " command=" + command +
            " channel=" + channel +
            " value=" + value;
    }

    // ----

    @Override
    public byte[] getMessage() {
        return null; // TODO
    }

    @Override
    public int getLength() {
        return 0;   // TODO
    }

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
        int midiChannel = context.retrieveChannel(this.channel);

        MidiEvent[] events = new MidiEvent[1];
        ShortMessage shortMessage = new ShortMessage();
//logger.log(Level.TRACE, "(" + StringUtil.toHex2(command) + "): " + channel + "ch, " + StringUtil.toHex2(value));
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                midiChannel,
                                command,
                                value);
        events[0] = new MidiEvent(shortMessage, context.getCurrentTick());
        return events;
    }
}
