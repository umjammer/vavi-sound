/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message;

import javax.sound.midi.MidiEvent;

import vavi.sound.smaf.ShortMessage;


/**
 * NopMessage.
 * <pre>
 *  duration    1or2
 *              0xff
 *              0x00
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class NopMessage extends ShortMessage
    implements MidiConvertible {

    /** the largest duration of one message */
    public static final int maxSteps = HandyPhoneStandard.maxSteps;

    /**
     * @param duration
     */
    public NopMessage(int duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "Nop:" +
            " duration=" + duration +
            " (%04x".formatted(duration) + ")";
    }

    // ----

    @Override
    public byte[] getMessage() {
        return HandyPhoneStandard.message(duration, 0xff, 0x00);
    }

    @Override
    public int getLength() {
        return getMessage().length;
    }

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) {
        return null;
    }
}
