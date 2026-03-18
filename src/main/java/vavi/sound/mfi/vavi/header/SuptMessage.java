/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import java.io.UnsupportedEncodingException;

import vavi.sound.mfi.vavi.SubMessage;


/**
 * MFi Header Sub Chunk for support information.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class SuptMessage extends SubMessage {

    /** */
    public static final String TYPE = "supt";

    @Override
    public boolean accept(String key) {
        return TYPE.equals(key);
    }

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     *
     * @param type ignored
     * @return this
     */
    @Override
    public SubMessage init(String type, byte[] data) {
        return super.init(TYPE, data);
    }

    /** */
    public SubMessage init(String data) {
        return super.init(TYPE, data);
    }

    @Override
    public String toString() {
        try {
            int length = getDataLength();
            byte[] data = getData();

            String string = new String(data, readingEncoding);
            return "supt: " + length + ": \"" + string + "\"";
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
