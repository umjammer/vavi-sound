/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.logging.Level;

import vavi.util.Debug;


/**
 * SmafFileWriter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071012 nsano initial version <br>
 */
class SmafFileWriter {

    /** smaf file types */
    private static int[] types = { SmafFileFormat.FILE_TYPE };

    /** */
    public int[] getSmafFileTypes() {
        return types;
    }

    /** */
    public int[] getSmafFileTypes(Sequence sequence) {
        // ignoring sequence, but there's only one type of SMAF Sequence, so that's fine
        return types;
    }

    /** */
    public boolean isFileTypeSupported(int fileType) {
        for (int type : types) {
            if (type == fileType) {
                return true;
            }
        }
        return false;
    }

    /** */
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
        // ignoring sequence, but there's only one type of SMAF Sequence, so that's fine
        return isFileTypeSupported(fileType);
    }

    /**
     * @param in You can specify the contents of the header chunk by setting various {@link SmafMessage TODO}
     *           to {@link Sequence#getTracks() Sequence#tracks}[0].
     * @return 0: if fileType is not supported, if there is an error in the write data
     *         else: number of bytes written
     */
    public int write(Sequence in, int fileType, OutputStream out)
        throws IOException {

        if (!isFileTypeSupported(fileType)) {
Debug.println(Level.WARNING, "unsupported fileType: " + fileType);
            return 0;
        }

        SmafFileFormat ff = new SmafFileFormat(in);

        // header (set the defaults for the minimum requirements)
//        try {
//        } catch (InvalidSmafDataException e) {
//            // TODO is IOException ok?
//            throw (IOException) new IOException().initCause(e);
//        }

        // body
        try {
            ff.writeTo(out);
        } catch (InvalidSmafDataException e) {
Debug.printStackTrace(e);
            return 0;
        }

        return ff.getByteLength();
    }

    /** Delegates to {@link #write(Sequence, int, OutputStream)} */
    public int write(Sequence in, int fileType, File out)
        throws IOException {

        OutputStream os = new BufferedOutputStream(Files.newOutputStream(out.toPath()));
        return write(in, fileType, os);
    }
}
