/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.psx;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the PSX (PS-ADPCM) audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-03 nsano initial version <br>
 */
@Deprecated
public class PsxFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies a PSX (PS-ADPCM) file.
     */
    public static final AudioFileFormat.Type PSX = new PsxFileFormatType("PSX", "mib");

    /**
     * Constructs a file type.
     *
     * @param name the name of the PSX File Format.
     * @param extension the file extension for this PSX File Format.
     */
    private PsxFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
