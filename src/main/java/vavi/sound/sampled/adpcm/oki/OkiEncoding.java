/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.oki;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the Oki adpcm decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class OkiEncoding extends AudioFormat.Encoding {

    /** Specifies any Oki encoded data. */
    public static final OkiEncoding OKI = new OkiEncoding("OKI");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the Oki encoding.
     */
    public OkiEncoding(String name) {
        super(name);
    }
}

/* */
