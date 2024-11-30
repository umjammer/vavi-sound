/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.rohm;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the ROHM adpcm decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class RohmEncoding extends AudioFormat.Encoding {

    /** Specifies any Rohm encoded data. */
    public static final RohmEncoding ROHM = new RohmEncoding("ROHM");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the Rohm encoding.
     */
    private RohmEncoding(String name) {
        super(name);
    }
}
