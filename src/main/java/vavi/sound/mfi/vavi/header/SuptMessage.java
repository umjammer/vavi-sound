/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import java.io.UnsupportedEncodingException;

import vavi.sound.mfi.vavi.SubMessage;


/**
 * サポート情報 MFi Header Sub Chunk.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class SuptMessage extends SubMessage {

    /** */
    public static final String TYPE = "supt";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public SuptMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /** */
    public SuptMessage(String data) {
        super(TYPE, data);
    }

    /** */
    public String toString() {
        try {
            int length = getDataLength();
            byte[] data = getData();

            String string = new String(data, readingEncoding);
            return "supt: " + length + ": \"" + string + "\"";
        } catch (UnsupportedEncodingException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
    }
}

/* */
