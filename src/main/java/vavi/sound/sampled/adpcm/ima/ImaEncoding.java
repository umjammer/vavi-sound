/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ima;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the IMA adpcm decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class ImaEncoding extends AudioFormat.Encoding {

    /** Specifies any DVI encoded data. */
    public static final ImaEncoding IMA = new ImaEncoding("IMA");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the IMA encoding.
     */
    public ImaEncoding(String name) {
        super(name);
    }
}

/* */
