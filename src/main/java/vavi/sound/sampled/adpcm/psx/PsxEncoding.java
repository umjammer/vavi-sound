/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.psx;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the PSX (PS-ADPCM) decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-03 nsano initial version <br>
 */
public class PsxEncoding extends AudioFormat.Encoding {

    /** Specifies any PS-ADPCM encoded data. */
    public static final PsxEncoding PSX = new PsxEncoding("PSX");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the PSX encoding.
     */
    private PsxEncoding(String name) {
        super(name);
    }
}
