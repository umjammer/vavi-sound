/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.mfi.SysexMessage;
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage.SysexTrackMessage;

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
public class ExtendedEditMessage extends SysexMessage implements SysexTrackMessage {

    private static final Logger logger = getLogger(ExtendedEditMessage.class.getName());

    @Override
    public boolean accept(String key) {
        return "255.e.241".equals(key);
    }

    @Override
    public ExtendedEditMessage init(byte[] message) {
        return (ExtendedEditMessage) super.init(message);
    }

    /**
     * for {@link TrackChunk}
     *
     * @param dis actual data (without header)
     */
    @Override
    public ExtendedEditMessage init(int delta, int status, int data1, DataInputStream dis)
        throws IOException {

        int dummy  = dis.read();    // 0x00
        dummy      = dis.read();    // 0x03
        dummy      = dis.read();    // 0x01
        int part   = dis.read();
        int zwitch = dis.read();
logger.log(Level.DEBUG, "dummy " + dummy + ", part " + part + ", switch " + zwitch);

        logger.log(Level.WARNING, "unsupported: " + 0xf2);
        return this;
    }

    @Override
    public String toString() {
        return "ExtendedEdit:";
    }
}
