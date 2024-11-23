/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ma;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the MA adpcm decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class MaEncoding extends AudioFormat.Encoding {

    /** Specifies any MA encoded data. */
    public static final MaEncoding MA = new MaEncoding("MA");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the MA encoding.
     */
    private MaEncoding(String name) {
        super(name);
    }
}
