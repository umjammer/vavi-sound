/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ccitt;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the CCITT adpcm decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050722 nsano initial version <br>
 */
public class CcittEncoding extends AudioFormat.Encoding {

    /** Specifies any G721 encoded data. */
    public static final CcittEncoding G721 = new CcittEncoding("G721");
    /** Specifies any G711 encoded data. */
    public static final CcittEncoding G711 = new CcittEncoding("G711");
    /** Specifies any G723 16 encoded data. */
    public static final CcittEncoding G723_16 = new CcittEncoding("G723_16");
    /** Specifies any G723 24 encoded data. */
    public static final CcittEncoding G723_24 = new CcittEncoding("G723_24");
    /** Specifies any G723 40 encoded data. */
    public static final CcittEncoding G723_40 = new CcittEncoding("G723_40");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the CCITT encoding.
     */
    private CcittEncoding(String name) {
        super(name);
    }
}
