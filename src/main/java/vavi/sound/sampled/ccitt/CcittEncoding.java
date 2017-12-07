/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.ccitt;


import javax.sound.sampled.AudioFormat;

/**
 * Encodings used by the Flac audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050722 nsano initial version <br>
 */
public class CcittEncoding extends AudioFormat.Encoding {

    /** Specifies any CCITT encoded data. */
    public static final CcittEncoding CCITT = new CcittEncoding("CCITT");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the CCITT encoding.
     */
    public CcittEncoding(String name) {
        super(name);
    }
}

/* */
