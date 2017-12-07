/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.io.IOException;
import java.io.InputStream;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.SysexMessage;


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
public class VoiceEditMessage extends SysexMessage {

    /** */
    protected VoiceEditMessage(byte[] message) {
        super(message);
    }

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param is 実際のデータ (ヘッダ無し)
     */
    public static VoiceEditMessage readFrom(int delta, int status, int data1, InputStream is)
        throws InvalidMfiDataException,
               IOException {

        throw new InvalidMfiDataException("unsupported: " + 0xf1);
    }

    /** */
    public String toString() {
        return "VoiceEdit:";
    }
}

/* */
