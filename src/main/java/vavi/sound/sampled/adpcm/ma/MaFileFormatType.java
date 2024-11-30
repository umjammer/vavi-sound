/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ma;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the MA audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
@Deprecated
public class MaFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an DVI file.
     */
    public static final AudioFileFormat.Type MA = new MaFileFormatType("MA", "wav");

    /**
     * Constructs a file type.
     *
     * @param name the name of the MA File Format.
     * @param extension the file extension for this MA File Format.
     */
    private MaFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
