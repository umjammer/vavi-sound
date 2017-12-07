/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    /** */
    public int[] getMfiFileTypes() {
        return types;
    }

    /** */
    public int[] getMfiFileTypes(Sequence sequence) {
        // sequence を無視しているけど MFi Sequence 一つしか型ないからいい
        return types;
    }

    /** */
    public boolean isFileTypeSupported(int fileType) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == fileType) {
                return true;
            }
        }
        return false;
    }

    /** */
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
        // sequence を無視しているけど MFi Sequence 一つしか型ないからいい
        return isFileTypeSupported(fileType);
    }

    /**
     * @param in {@link Sequence#getTracks() Sequence#tracks}[0] に
     *           各種 {@link SubMessage} を設定することで
     *           ヘッダチャンクの内容を指定することが出来ます。
     * @return 0: fileType がサポートされていない場合、書き込みデータにエラーがある場合
     *         else: 書き込んだバイト数
     */
    public int write(Sequence in, int fileType, OutputStream out)
        throws IOException {

        if (!isFileTypeSupported(fileType)) {
Debug.println(Level.WARNING, "unsupported fileType: " + fileType);
            return 0;
        }

        VaviMfiFileFormat ff = new VaviMfiFileFormat(in);

        // header (最低限必要なものはデフォルトを設定)
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
            // TODO IOException でいいのか？
            throw (IOException) new IOException().initCause(e);
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
Debug.printStackTrace(e);
            return 0;
        }

        return ff.getByteLength();
    }

    /** {@link #write(Sequence, int, OutputStream)} に委譲 */
    public int write(Sequence in, int fileType, File out)
        throws IOException {

        OutputStream os = new BufferedOutputStream(new FileOutputStream(out));
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
Debug.println("major: " + defaultMajorType);
            }

            value = props.getProperty("format.type.minor");
            if (value != null) {
                defaultMinorType = Integer.parseInt(value);
Debug.println("minor: " + defaultMinorType);
            }

            value = props.getProperty("format.header.titl");
            if (value != null) {
                defaultTitle = value;
Debug.println("titl: " + defaultTitle);
            }

            value = props.getProperty("format.header.prot");
            if (value != null) {
                defaultCreator = value;
Debug.println("prot: " + defaultCreator);
            }

            value = props.getProperty("format.header.vers");
            if (value != null) {
                defaultVersion = value;
Debug.println("vers: " + defaultVersion);
            }
        } catch (Exception e) {
Debug.printStackTrace(e);
            throw new IllegalStateException(e);
        }
    }
}

/* */
