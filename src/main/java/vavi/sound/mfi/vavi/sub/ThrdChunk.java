/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sub;

import vavi.sound.mfi.vavi.SubChunk;


/**
 * MFi Header Sub Chunk for 3D information.
 * <pre>
 *  &quot;thrd&quot;  bytes:
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070125 nsano initial version <br>
 * @since MFi 4.0
 */
public class ThrdChunk extends SubChunk {

    /** */
    public static final String TYPE = "thrd";

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
}
