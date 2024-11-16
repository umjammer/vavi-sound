/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.oki;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the Oki audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
@Deprecated
public class OkiFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an DVI file.
     */
    public static final AudioFileFormat.Type OKI = new OkiFileFormatType("OKI", "adpcm");

    /**
     * Constructs a file type.
     *
     * @param name the name of the Oki File Format.
     * @param extension the file extension for this Oki File Format.
     */
    public OkiFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
