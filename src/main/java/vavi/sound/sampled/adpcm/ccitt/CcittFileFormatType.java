/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ccitt;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the CCITT audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050722 nsano initial version <br>
 */
public class CcittFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an CCITT file.
     */
    public static final AudioFileFormat.Type G721 = new CcittFileFormatType("G721", "wav");

    /**
     * Constructs a file type.
     *
     * @param name the name of the CCITT File Format.
     * @param extension the file extension for this CCITT File Format.
     */
    public CcittFileFormatType(String name, String extension) {
        super(name, extension);
    }
}

/* */
