/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ms;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the DVI audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class MsFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an DVI file.
     */
    public static final AudioFileFormat.Type MS = new MsFileFormatType("MS", "wav");

    /**
     * Constructs a file type.
     *
     * @param name the name of the MS File Format.
     * @param extension the file extension for this MS File Format.
     */
    public MsFileFormatType(String name, String extension) {
        super(name, extension);
    }
}

/* */
