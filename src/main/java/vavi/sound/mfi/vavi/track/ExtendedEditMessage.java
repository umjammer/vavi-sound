/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.SysexMessage;

import static java.lang.System.getLogger;


/**
 * ExtendedEditMessage.
 * <pre>
 *  0xff, 0xf1
 * </pre>
 * <p>
 * TODO only MFi1
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020704 nsano initial version <br>
 */
public class ExtendedEditMessage extends SysexMessage {

    private static final Logger logger = getLogger(ExtendedEditMessage.class.getName());

    /** */
    protected ExtendedEditMessage(byte[] message) {
        super(message);
    }

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param is actual data (without header)
     */
    public static ExtendedEditMessage readFrom(int delta, int status, int data1, InputStream is)
        throws InvalidMfiDataException,
               IOException {

        DataInputStream dis = new DataInputStream(is);

        int dummy  = dis.read();    // 0x00
        dummy      = dis.read();    // 0x03
        dummy      = dis.read();    // 0x01
        int part   = dis.read();
        int zwitch = dis.read();
logger.log(Level.DEBUG, "dummy " + dummy + ", part " + part + ", switch " + zwitch);

        throw new InvalidMfiDataException("unsupported: " + 0xf2);
    }

    @Override
    public String toString() {
        return "ExtendedEdit:";
    }
}
