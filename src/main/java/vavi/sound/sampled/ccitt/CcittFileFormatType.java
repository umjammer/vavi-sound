/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.ccitt;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the CCITT audio decoder.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050722 nsano initial version <br>
 */
public class CcittFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an CCITT file.
     */
    public static final AudioFileFormat.Type CCITT = new CcittFileFormatType("CCITT", "adpcm");

    /**
     * Constructs a file type.
     *
     * @param name the name of the Flac File Format.
     * @param extension the file extension for this Flac File Format.
     */
    public CcittFileFormatType(String name, String extension) {
        super(name, extension);
    }
}

/* */
