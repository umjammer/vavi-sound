/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiFileFormat;
import vavi.sound.mfi.Sequence;


/**
 * MfiFileReader.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 020704 nsano midi compliant <br>
 */
public abstract class MfiFileReader {

    /** */
    public abstract MfiFileFormat getMfiFileFormat(InputStream stream)
        throws InvalidMfiDataException,
               IOException;

    /** */
    public abstract MfiFileFormat getMfiFileFormat(File file)
        throws InvalidMfiDataException,
               IOException;

    /** */
    public abstract MfiFileFormat getMfiFileFormat(URL url)
        throws InvalidMfiDataException,
               IOException;

    /** */
    public abstract Sequence getSequence(InputStream stream)
        throws InvalidMfiDataException,
               IOException;

    /** */
    public abstract Sequence getSequence(File file)
        throws InvalidMfiDataException,
               IOException;

    /** */
    public abstract Sequence getSequence(URL url)
        throws InvalidMfiDataException,
               IOException;
}

/* */
