/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.dvi;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the DVI adpcm decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class DviEncoding extends AudioFormat.Encoding {

    /** Specifies any DVI encoded data. */
    public static final DviEncoding DVI = new DviEncoding("DVI");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the DVI encoding.
     */
    public DviEncoding(String name) {
        super(name);
    }
}

/* */
