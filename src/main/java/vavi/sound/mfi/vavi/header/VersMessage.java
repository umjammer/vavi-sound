/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import java.io.UnsupportedEncodingException;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.SubMessage;


/**
 * バージョン情報 MFi Header Sub Chunk.
 *
 * <pre>
 *  &quot;vers&quot; 4 bytes: mld version
 *  format mmnn (ex. 0100)
 * </pre>
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class VersMessage extends SubMessage {

    /** */
    public static final String TYPE = "vers";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * <li>TODO length 4 check
     * @param type ignored
     */
    public VersMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /**
     * TODO length 4 check
     * @param data 4 bytes number (ex. { '0', '3', '0', '1' })
     */
    public VersMessage(String data) {
        super(TYPE, data);
    }

    /** */
    public String getVersion() {
        try {
            return new String(getData(), readingEncoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * TODO length 4 check
     * @param version 4 bytes number as string (ex. "0301")
     */
    public void setVersion(String version)
        throws InvalidMfiDataException {

        try {
            setData(version.getBytes(readingEncoding));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    public String toString() {
        return "vers: " + getDataLength() +
               ": " + getVersion();
    }
}

/* */
