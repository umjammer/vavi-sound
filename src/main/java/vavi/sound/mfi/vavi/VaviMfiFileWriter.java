/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.spi.MfiFileWriter;
import vavi.util.Debug;


/**
 * VaviMfiFileWriter
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020704 nsano initial version <br>
 *          0.01 030817 nsano add isFileTypeSupported <br>
 *          0.02 030819 nsano change sequence related <br>
 */
public class VaviMfiFileWriter extends MfiFileWriter {

    /** mfi file types */
    private static int[] types = { VaviMfiFileFormat.FILE_TYPE };

    /** MFi major version */
    private static int defaultMajorType = HeaderChunk.MAJOR_TYPE_RING_TONE;

    /** MFi minor version */
    private static int defaultMinorType = HeaderChunk.MINOR_TYPE_ALL;

    /** @see vavi.sound.mfi.vavi.header.TitlMessage */
    private static String defaultTitle = "untitled";

    /** @see vavi.sound.mfi.vavi.header.VersMessage */
    private static String defaultVersion = "0400";

    /** @see vavi.sound.mfi.vavi.header.ProtMessage */
    private static String defaultCreator = "vavi";

    @Override
    public int[] getMfiFileTypes() {
        return types;
    }

    @Override
    public int[] getMfiFileTypes(Sequence sequence) {
        // ignoring sequence, but there's only one type of MFi sequence, so that's fine.
        return types;
    }

    @Override
    public boolean isFileTypeSupported(int fileType) {
        for (int type : types) {
            if (type == fileType) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
        // ignoring sequence, but there's only one type of MFi sequence, so that's fine.
        return isFileTypeSupported(fileType);
    }

    /**
     * @param in You can specify the contents of the header chunk by setting
     *           various {@link SubMessage} to {@link Sequence#getTracks() Sequence#tracks}[0].
     * @return 0: if fileType is not supported, if there is an error in the write data
     *         else: number of bytes written
     */
    @Override
    public int write(Sequence in, int fileType, OutputStream out) throws IOException {

        if (!isFileTypeSupported(fileType)) {
Debug.println(Level.WARNING, "unsupported fileType: " + fileType);
            return 0;
        }

        VaviMfiFileFormat ff = new VaviMfiFileFormat(in);

        // header (set the defaults for the minimum requirements)
        try {
            // sorc
            try { ff.getSorc(); }
            catch (NoSuchElementException e) { ff.setSorc(0); }
            // titl
            try { ff.getTitle(); }
            catch (NoSuchElementException e) { ff.setTitle(defaultTitle); }
            // vers
            try { ff.getVersion(); }
            catch (NoSuchElementException e) { ff.setVersion(defaultVersion); }
            // prot
            try { ff.getProt(); }
            catch (NoSuchElementException e) { ff.setProt(defaultCreator); }
            // note length
            ff.setNoteLength(1);
        } catch (InvalidMfiDataException e) {
            // TODO is IOException ok?
            throw new IOException(e);
        }

        int type = ff.getMajorType();
        if (type == -1) {
            ff.setMajorType(defaultMajorType);
        }
        type = ff.getMinorType();
        if (type == -1) {
            ff.setMinorType(defaultMinorType);
        }

        // body
        try {
            ff.writeTo(out);
        } catch (InvalidMfiDataException e) {
Debug.printStackTrace(Level.WARNING, e);
            return 0;
        }

        return ff.getByteLength();
    }

    /** delegate to {@link #write(Sequence, int, OutputStream)} */
    @Override
    public int write(Sequence in, int fileType, File out)
        throws IOException {

        OutputStream os = new BufferedOutputStream(Files.newOutputStream(out.toPath()));
        return write(in, fileType, os);
    }

    static {
        try {
            // props
            Properties props = new Properties();
            final String path = "vavi.properties";
            props.load(SubMessage.class.getResourceAsStream(path));

            String value = props.getProperty("format.type.major");
            if (value != null) {
                defaultMajorType = Integer.parseInt(value);
Debug.println(Level.FINE, "major: " + defaultMajorType);
            }

            value = props.getProperty("format.type.minor");
            if (value != null) {
                defaultMinorType = Integer.parseInt(value);
Debug.println(Level.FINE, "minor: " + defaultMinorType);
            }

            value = props.getProperty("format.header.titl");
            if (value != null) {
                defaultTitle = value;
Debug.println(Level.FINE, "titl: " + defaultTitle);
            }

            value = props.getProperty("format.header.prot");
            if (value != null) {
                defaultCreator = value;
Debug.println(Level.FINE, "prot: " + defaultCreator);
            }

            value = props.getProperty("format.header.vers");
            if (value != null) {
                defaultVersion = value;
Debug.println(Level.FINE, "vers: " + defaultVersion);
            }
        } catch (Exception e) {
Debug.printStackTrace(Level.SEVERE, e);
            throw new IllegalStateException(e);
        }
    }
}
