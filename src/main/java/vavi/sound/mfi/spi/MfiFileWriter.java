/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.spi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import vavi.sound.mfi.Sequence;


/**
 * MfiFileWriter.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020629 nsano initial version <br>
 *          0.01 020704 nsano midi compliant <br>
 *          0.02 030817 nsano add isFileTypeSupported <br>
 */
public abstract class MfiFileWriter {

    /** */
    public abstract int[] getMfiFileTypes();

    /** */
    public abstract int[] getMfiFileTypes(Sequence sequence);

    /** */
    public boolean isFileTypeSupported(int fileType) {
        return false;
    }

    /** TODO */
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
        return false;
    }

    /** */
    public abstract int write(Sequence in,
                              int fileType,
                              OutputStream out)
        throws IOException;

    /** */
    public abstract int write(Sequence in,
                              int fileType,
                              File out)
        throws IOException;
}

/* */
