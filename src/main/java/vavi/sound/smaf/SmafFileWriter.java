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
        // sequence を無視しているけど SMAF Sequence 一つしか型ないからいい
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
        // sequence を無視しているけど SMAF Sequence 一つしか型ないからいい
        return isFileTypeSupported(fileType);
    }

    /**
     * @param in {@link Sequence#getTracks() Sequence#tracks}[0] に
     *           各種 {@link SmafMessage TODO} を設定することで
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

        SmafFileFormat ff = new SmafFileFormat(in);

        // header (最低限必要なものはデフォルトを設定)
//        try {
//        } catch (InvalidSmafDataException e) {
//            // TODO IOException でいいのか？
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

    /** {@link #write(Sequence, int, OutputStream)} に委譲 */
    public int write(Sequence in, int fileType, File out)
        throws IOException {

        OutputStream os = new BufferedOutputStream(Files.newOutputStream(out.toPath()));
        return write(in, fileType, os);
    }
}

/* */
