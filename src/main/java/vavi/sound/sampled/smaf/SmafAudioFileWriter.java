/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.smaf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileWriter;

import vavi.sound.smaf.InvalidSmafDataException;


/**
 * SmafAudioFileWriter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080415 nsano initial version <br>
 */
public class SmafAudioFileWriter extends AudioFileWriter {

    /** */
    private static final Type[] outputTypes = new Type[] {
        new SMAF(null) // TODO null
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
     * @param fileType must be instance of {@link SMAF}
     * @param out null 出力を指定してください。 win32: "nul", *nix: "/dev/null"
     * @throws IllegalArgumentException fileType が SMAF でない場合スローされます。
     * @throws NullPointerException fileType に properties が設定されていない場合スローされます。
     */
    public int write(AudioInputStream stream, Type fileType, OutputStream out) throws IOException {
        if (!isFileTypeSupported(fileType, stream)) {
            throw new IllegalArgumentException("unsupported fileType: " + fileType.getClass().getName());
        }
        try {
            // properties
            boolean divided = (Boolean) ((SMAF) fileType).getProperty("smaf.divided");
            if (divided) {
                // 分割
                String directory = (String) ((SMAF) fileType).getProperty("smaf.directory");
                String base = (String) ((SMAF) fileType).getProperty("smaf.base");
                float time = (Float) ((SMAF) fileType).getProperty("smaf.time");
                int sampleRate = (Integer) ((SMAF) fileType).getProperty("smaf.sampleRate");
                int bits = (Integer) ((SMAF) fileType).getProperty("smaf.bits");
                int channels = (Integer) ((SMAF) fileType).getProperty("smaf.channels");
                int masterVolume = (Integer) ((SMAF) fileType).getProperty("smaf.masterVolume");
                int adpcmVolume = (Integer) ((SMAF) fileType).getProperty("smaf.adpcmVolume");

                DividedSmafWithVoiceMaker mwvm = new DividedSmafWithVoiceMaker(stream, directory, base, time, sampleRate, bits, channels, masterVolume, adpcmVolume);
                int r = mwvm.create();
                return r;
            } else {
                // 単体
                String filename = (String) ((SMAF) fileType).getProperty("smaf.filename");
                float time = (Float) ((SMAF) fileType).getProperty("smaf.time");
                int sampleRate = (Integer) ((SMAF) fileType).getProperty("smaf.sampleRate");
                int bits = (Integer) ((SMAF) fileType).getProperty("smaf.bits");
                int channels = (Integer) ((SMAF) fileType).getProperty("smaf.channels");
                int masterVolume = (Integer) ((SMAF) fileType).getProperty("smaf.masterVolume");
                int adpcmVolume = (Integer) ((SMAF) fileType).getProperty("smaf.adpcmVolume");

                SmafWithVoiceMaker mwvm = new SmafWithVoiceMaker(stream, filename, time, sampleRate, bits, channels, masterVolume, adpcmVolume);
                int r = mwvm.create();
                return r;
            }
        } catch (UnsupportedAudioFileException | InvalidSmafDataException e) {
            throw new IOException(e);
        }
    }

    /**
     * 指定されたファイル型のオーディオファイルを表すバイトのストリームを、指定された出力ストリームへ書き込みます。
     * @param fileType must be instance of {@link SMAF}
     * @param out null 出力を指定してください。 win32: "nul", *nix: "/dev/null"
     */
    public int write(AudioInputStream stream, Type fileType, File out) throws IOException {
        return write(stream, fileType, new FileOutputStream(out));
    }
}

/* */
