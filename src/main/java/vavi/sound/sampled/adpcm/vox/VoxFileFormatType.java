/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.vox;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the VOX audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
@Deprecated
public class VoxFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an VOX file.
     */
    public static final AudioFileFormat.Type VOX = new VoxFileFormatType("VOX", "adpcm");

    /**
     * Constructs a file type.
     *
     * @param name the name of the VOX File Format.
     * @param extension the file extension for this VOX File Format.
     */
    public VoxFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
