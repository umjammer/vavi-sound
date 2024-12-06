/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * UndefinedMessage
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030821 nsano initial version <br>
 *          0.01 030920 nsano repackage <br>
 */
public class UndefinedMessage extends MfiMessage
    implements MidiConvertible {

    private static final Logger logger = getLogger(UndefinedMessage.class.getName());

    /**
     *
     * @param delta delta time
     * @param status
     * @param data1 extended status
     * @param data2 data
     */
    public UndefinedMessage(int delta, int status, int data1, int data2) {
        this(delta, status, data1, new byte[] { (byte) data2 });
    }

    /**
     *
     * @param delta delta time
     * @param status
     * @param data1 extended status
     * @param data2 data
     */
    public UndefinedMessage(int delta, int status, int data1, byte[] data2) {
        super(new byte[3 + data2.length]);

        data[0] = (byte) (delta & 0xff);
        data[1] = (byte) (status & 0xff);
        data[2] = (byte) (data1 & 0xff);
        System.arraycopy(data2, 0, data, 3, data2.length);
    }

    @Override
    public String toString() {
        return "Undefined: %02x, %02x, %02x\n%s".formatted(data[0], data[1], data[2], StringUtil.getDump(data, 3, length - 3));
    }

    // ----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) {
logger.log(Level.DEBUG, this);
        return null;
    }
}
