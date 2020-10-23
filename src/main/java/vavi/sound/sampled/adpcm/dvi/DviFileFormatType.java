/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.dvi;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the DVI audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class DviFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an DVI file.
     */
    public static final AudioFileFormat.Type DVI = new DviFileFormatType("DVI", "wav");

    /**
     * Constructs a file type.
     *
     * @param name the name of the DVI File Format.
     * @param extension the file extension for this DVI File Format.
     */
    public DviFileFormatType(String name, String extension) {
        super(name, extension);
    }
}

/* */
