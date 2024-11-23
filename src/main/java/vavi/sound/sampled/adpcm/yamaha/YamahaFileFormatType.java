/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.yamaha;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the Yamaha audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
@Deprecated
public class YamahaFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies a Yamaha file.
     */
    public static final AudioFileFormat.Type YAMAHA = new YamahaFileFormatType("YAMAHA", "adpcm");

    /**
     * Constructs a file type.
     *
     * @param name the name of the Yamaha File Format.
     * @param extension the file extension for this Yamaha File Format.
     */
    private YamahaFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
