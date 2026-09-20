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
 * VoiceEditMessage.
 * <pre>
 *  0xff, 0xf0
 *  length 2 bytes
 *  data ...
 * </pre>
 * <p>
 * The voice data is not applied to any sound source yet, it is only kept as it came, so that
 * the message is skipped by the right number of bytes and what follows it in the track still
 * parses (and so that writing the sequence out again gives the same bytes).
 * </p>
 * <p>
 * TODO only MFi1
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 2026-09-20 nsano read the length <br>
 */
public class VoiceEditMessage extends SysexMessage implements SysexTrackMessage {

    private static final Logger logger = getLogger(VoiceEditMessage.class.getName());

    @Override
    public boolean accept(String key) {
        return "255.e.240".equals(key);
    }

    @Override
    public VoiceEditMessage init(byte[] message) {
        return (VoiceEditMessage) super.init(message);
    }

    /**
     * for {@link TrackChunk}
     *
     * @param dis actual data (without header, data2 ~)
     */
    @Override
    public VoiceEditMessage init(int delta, int status, int data1, DataInputStream dis) throws IOException {

        int length = dis.readUnsignedShort();

        byte[] data = new byte[5 + length];

        data[0] = (byte) (delta & 0xff);
        data[1] = (byte) (status & 0xff);           // normal 0xff
        data[2] = (byte) (data1 & 0xff);            // voice edit 0xf0
        data[3] = (byte) ((length / 0x100) & 0xff); // length MSB
        data[4] = (byte) ((length % 0x100) & 0xff); // length LSB

        dis.readFully(data, 5, length);

logger.log(Level.DEBUG, "VoiceEdit: Δ: %02x, len: %d%n%s".formatted(data[0], length, StringUtil.getDump(data, 5, length)));
        super.init(data);
        return this;
    }

    @Override
    public String toString() {
        return "VoiceEdit: %d bytes".formatted(length - 5);
    }
}
