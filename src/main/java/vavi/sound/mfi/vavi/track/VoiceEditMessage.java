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


/**
 * VoiceEditMessage.
 * <pre>
 *  0xff, 0xf0
 * </pre>
 * <p>
 * TODO only MFi1
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 */
public class VoiceEditMessage extends SysexMessage implements SysexTrackMessage {

    private static final Logger logger = System.getLogger(VoiceEditMessage.class.getName());

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
     * @param dis actual data (without header)
     */
    @Override
    public VoiceEditMessage init(int delta, int status, int data1, DataInputStream dis) throws IOException {
        logger.log(Level.WARNING, "unsupported: " + 0xf0);
byte[] data = new byte[length];
dis.readFully(data);
        return this;
    }

    @Override
    public String toString() {
        return "VoiceEdit:";
    }
}
