/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ima;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the IMA audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class ImaFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an DVI file.
     */
    public static final AudioFileFormat.Type IMA = new ImaFileFormatType("IMA", "wav");

    /**
     * Constructs a file type.
     *
     * @param name the name of the IMA File Format.
     * @param extension the file extension for this IMA File Format.
     */
    public ImaFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
