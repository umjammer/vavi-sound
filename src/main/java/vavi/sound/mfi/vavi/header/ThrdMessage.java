/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import vavi.sound.mfi.vavi.SubMessage;


/**
 * MFi Header Sub Chunk for 3D information.
 * <pre>
 *  &quot;thrd&quot;  bytes:
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070125 nsano initial version <br>
 * @since MFi 4.0
 */
public class ThrdMessage extends SubMessage {

    /** */
    public static final String TYPE = "thrd";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public ThrdMessage(String type, byte[] data) {
        super(TYPE, data);
    }
}
