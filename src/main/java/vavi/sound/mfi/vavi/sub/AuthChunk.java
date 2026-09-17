/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sub;

import java.io.UnsupportedEncodingException;

import vavi.sound.mfi.vavi.SubChunk;


/**
 * MFi Header Sub Chunk for copyright control information.
 * <li> TODO use {@link CodeChunk}
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class AuthChunk extends SubChunk {

    /** */
    public static final String TYPE = "auth";

    @Override
    public boolean accept(String key) {
        return TYPE.equals(key);
    }

    /**
     * for {@link SubChunk#readFrom(java.io.InputStream)}
     *
     * @param type ignored
     * @return this
     */
    @Override
    public SubChunk init(String type, byte[] data) {
        return super.init(TYPE, data);
    }

    /** */
    public SubChunk init(String data) {
        return super.init(TYPE, data);
    }

    @Override
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
