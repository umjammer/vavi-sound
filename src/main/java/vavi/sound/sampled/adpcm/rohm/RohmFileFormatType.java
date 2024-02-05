/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.rohm;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the Rohm audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class RohmFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an Rohm file.
     */
    public static final AudioFileFormat.Type ROHM = new RohmFileFormatType("ROHM", "wav");

    /**
     * Constructs a file type.
     *
     * @param name the name of the Rohm File Format.
     * @param extension the file extension for this Rohm File Format.
     */
    public RohmFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
