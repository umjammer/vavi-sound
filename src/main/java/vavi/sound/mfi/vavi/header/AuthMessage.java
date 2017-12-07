/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import java.io.UnsupportedEncodingException;

import vavi.sound.mfi.vavi.SubMessage;


/**
 * 著作権管理情報 MFi Header Sub Chunk.
 * <li> TODO use {@link CodeMessage} 
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class AuthMessage extends SubMessage {

    /** */
    public static final String TYPE = "auth";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public AuthMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /** */
    public AuthMessage(String data) {
        super(TYPE, data);
    }

    /** */
    public String toString() {
        try {
            int length = getDataLength();
            byte[] data = getData();

            String string = new String(data, readingEncoding);
            return "auth: " + length + ": \"" + string + "\"";
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}

/* */
