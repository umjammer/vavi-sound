/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.vox;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the VOX adpcm decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class VoxEncoding extends AudioFormat.Encoding {

    /** Specifies any DVI encoded data. */
    public static final VoxEncoding VOX = new VoxEncoding("VOX");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the VOX encoding.
     */
    public VoxEncoding(String name) {
        super(name);
    }
}
