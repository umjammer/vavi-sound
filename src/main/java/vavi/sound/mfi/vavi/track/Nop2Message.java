/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;

import static java.lang.System.getLogger;


/**
 * Nop2Message.
 * <pre>
 *  0xff, 0xd# Play Control Information
 *  channel false
 *  delta   true
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051116 nsano initial version <br>
 * @since MFi 3.0
 */
public class Nop2Message extends ShortMessage
    implements MidiConvertible {

    private static final Logger logger = getLogger(Nop2Message.class.getName());

    public static final int maxDelta = 0xff * 0x100 + 0xff;

    /**
     * 0xdc
     * <pre>
     *  // usage
     *  int delta = ...
     *  MfiMessage message = new Nop2Message(delta % 0x100, delta / 0x100);
     * </pre>
     * @param delta delta time
     * @param data2 0 ~ 255 (* 0xff)
     */
    public Nop2Message(int delta, int data2) {
        super(delta, 0xff, 0xdc, data2);
logger.log(Level.DEBUG, "NOP2: delta: " + delta);
    }

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 always 0xdc
     * @param data2 0 ~ 255 (* 0xff)
     */
    public Nop2Message(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xdc, data2);
logger.log(Level.DEBUG, "NOP2: delta: " + delta);
    }

    /** */
    public String toString() {
        return "Nop2:" + (data[3] & 0xff);
    }

    //----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) {
//logger.log(Level.DEBUG, "ignore: " + this);
        return null;
    }
}
