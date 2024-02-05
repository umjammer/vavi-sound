/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.yamaha;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the Yamaha adpcm decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class YamahaEncoding extends AudioFormat.Encoding {

    /** Specifies any DVI encoded data. */
    public static final YamahaEncoding YAMAHA = new YamahaEncoding("YAMAHA");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the Yamaha encoding.
     */
    public YamahaEncoding(String name) {
        super(name);
    }
}
