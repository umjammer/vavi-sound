/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ms;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the MS adpcm decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class MsEncoding extends AudioFormat.Encoding {

    /** Specifies any MS encoded data. */
    public static final MsEncoding MS = new MsEncoding("MS");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the MS encoding.
     */
    private MsEncoding(String name) {
        super(name);
    }
}
