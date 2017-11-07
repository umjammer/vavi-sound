/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.mfi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileWriter;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.util.Debug;


/**
 * MfiAudioFileWriter.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 051222 nsano initial version <br>
 */
public class MfiAudioFileWriter extends AudioFileWriter {

    /** */
    private static final Type[] outputTypes = new Type[] {
        new MFi(null) // TODO null
    };

    /** このオーディオファイルライタがファイル書き込みサポートを提供するファイル型を取得します。 */
    public Type[] getAudioFileTypes() {
        return outputTypes;
    }

    /** 指定されたオーディオ入力ストリームからこのオーディオファイルライタが書き込みできるファイル型を取得します。 */
    public Type[] getAudioFileTypes(AudioInputStream stream) {
        return getAudioFileTypes(); // TODO check stream
    }

    /**
     * 指定されたファイル形式のオーディオファイルを表すバイトストリームを、指定された外部ファイルへ書き込みます。
     * @param fileType must be instance of {@link MFi}
     * @param out null 出力を指定してください。 win32: "nul", *nix: "/dev/null"
     * @throws IllegalArgumentException fileType が MFi でない場合スローされます。
     * @throws NullPointerException fileType に properties が設定されていない場合スローされます。
     */
    public int write(AudioInputStream stream, Type fileType, OutputStream out) throws IOException {
        if (!isFileTypeSupported(fileType, stream)) {
            throw new IllegalArgumentException("unsupported fileType: " + fileType.getClass().getName());
        }
        try {
            // properties
            boolean divided = (Boolean) ((MFi) fileType).getProperty("mfi.divided");
            if (divided) {
                // 分割
                String directory = (String) ((MFi) fileType).getProperty("mfi.directory");
                String base = (String) ((MFi) fileType).getProperty("mfi.base");
                String model = (String) ((MFi) fileType).getProperty("mfi.model");
                float time = (Float) ((MFi) fileType).getProperty("mfi.time");
                int sampleRate = (Integer) ((MFi) fileType).getProperty("mfi.sampleRate");
                int bits = (Integer) ((MFi) fileType).getProperty("mfi.bits");
                int channels = (Integer) ((MFi) fileType).getProperty("mfi.channels");
                int masterVolume = (Integer) ((MFi) fileType).getProperty("mfi.masterVolume");
                int adpcmVolume = (Integer) ((MFi) fileType).getProperty("mfi.adpcmVolume");

                DividedMfiWithVoiceMaker mwvm = new DividedMfiWithVoiceMaker(stream, directory, base, model, time, sampleRate, bits, channels, masterVolume, adpcmVolume);
                int r = mwvm.create();
                return r;
            } else {
                // 単体
                String filename = (String) ((MFi) fileType).getProperty("mfi.filename");
                String model = (String) ((MFi) fileType).getProperty("mfi.model");
                float time = (Float) ((MFi) fileType).getProperty("mfi.time");
                int sampleRate = (Integer) ((MFi) fileType).getProperty("mfi.sampleRate");
                int bits = (Integer) ((MFi) fileType).getProperty("mfi.bits");
                int channels = (Integer) ((MFi) fileType).getProperty("mfi.channels");
                int masterVolume = (Integer) ((MFi) fileType).getProperty("mfi.masterVolume");
                int adpcmVolume = (Integer) ((MFi) fileType).getProperty("mfi.adpcmVolume");

                MfiWithVoiceMaker mwvm = new MfiWithVoiceMaker(stream, filename, model, time, sampleRate, bits, channels, masterVolume, adpcmVolume);
                int r = mwvm.create();
                return r;
            }
        } catch (UnsupportedAudioFileException e) {
            throw (IOException) new IOException().initCause(e);
        } catch (InvalidMfiDataException e) {
            throw (IOException) new IOException().initCause(e);
        } catch (IllegalArgumentException e) {
Debug.printStackTrace(e);
            throw e;
        }
    }

    /**
     * 指定されたファイル型のオーディオファイルを表すバイトのストリームを、指定された出力ストリームへ書き込みます。
     * @param fileType must be instance of {@link MFi}
     * @param out null 出力を指定してください。 win32: "nul", *nix: "/dev/null"
     */
    public int write(AudioInputStream stream, Type fileType, File out) throws IOException {
        return write(stream, fileType, new FileOutputStream(out));
    }
}

/* */
