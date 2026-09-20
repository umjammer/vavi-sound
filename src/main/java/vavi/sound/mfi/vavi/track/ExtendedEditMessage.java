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
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * ExtendedEditMessage.
 * <pre>
 *  0xff, 0xf1
 *  length 2 bytes
 *  data ... 0x01, part, switch
 * </pre>
 * <p>
 * TODO only MFi1
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020704 nsano initial version <br>
 *          0.01 2026-09-20 nsano read the length <br>
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
     * @param dis actual data (without header, data2 ~)
     */
    @Override
    public ExtendedEditMessage init(int delta, int status, int data1, DataInputStream dis)
        throws IOException {

        int length = dis.readUnsignedShort();

        byte[] data = new byte[5 + length];

        data[0] = (byte) (delta & 0xff);
        data[1] = (byte) (status & 0xff);           // normal 0xff
        data[2] = (byte) (data1 & 0xff);            // extended edit 0xf1
        data[3] = (byte) ((length / 0x100) & 0xff); // length MSB
        data[4] = (byte) ((length % 0x100) & 0xff); // length LSB

        dis.readFully(data, 5, length);

logger.log(Level.DEBUG, "ExtendedEdit: Δ: %02x, len: %d%n%s".formatted(data[0], length, StringUtil.getDump(data, 5, length)));
        super.init(data);
        return this;
    }

    /** the part it edits, -1 when the data is too short */
    public int getPart() {
        return length >= 7 ? data[6] & 0xff : -1;
    }

    /** the switch, -1 when the data is too short */
    public int getSwitch() {
        return length >= 8 ? data[7] & 0xff : -1;
    }

    @Override
    public String toString() {
        return "ExtendedEdit: part=%d switch=%d".formatted(getPart(), getSwitch());
    }
}
