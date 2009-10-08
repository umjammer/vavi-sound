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
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.spi.AudioFileWriter;

import vavi.sound.mfi.InvalidMfiDataException;


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

    /** ���̃I�[�f�B�I�t�@�C�����C�^���t�@�C���������݃T�|�[�g��񋟂���t�@�C���^���擾���܂��B */
    public Type[] getAudioFileTypes() {
        return outputTypes;
    }

    /** �w�肳�ꂽ�I�[�f�B�I���̓X�g���[�����炱�̃I�[�f�B�I�t�@�C�����C�^���������݂ł���t�@�C���^���擾���܂��B */
    public Type[] getAudioFileTypes(AudioInputStream stream) {
        return getAudioFileTypes(); // TODO check stream
    }

    /**
     * �w�肳�ꂽ�t�@�C���`���̃I�[�f�B�I�t�@�C����\���o�C�g�X�g���[�����A�w�肳�ꂽ�O���t�@�C���֏������݂܂��B
     * @param fileType must be instance of {@link MFi}
     * @param out null �o�͂��w�肵�Ă��������B win32: "nul", *nix: "/dev/null"
     * @throws IllegalArgumentException fileType �� MFi �łȂ��ꍇ�X���[����܂��B
     * @throws NullPointerException fileType �� properties ���ݒ肳��Ă��Ȃ��ꍇ�X���[����܂��B
     */
    public int write(AudioInputStream stream, Type fileType, OutputStream out) throws IOException {
        if (!isFileTypeSupported(fileType, stream)) {
            throw new IllegalArgumentException("unsupported fileType: " + fileType.getClass().getName());
        }
        try {
            // properties
            boolean divided = (Boolean) ((MFi) fileType).getProperty("mfi.divided");
            if (divided) {
                // ����
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
                // �P��
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
        }
    }

    /**
     * �w�肳�ꂽ�t�@�C���^�̃I�[�f�B�I�t�@�C����\���o�C�g�̃X�g���[�����A�w�肳�ꂽ�o�̓X�g���[���֏������݂܂��B
     * @param fileType must be instance of {@link MFi}
     * @param out null �o�͂��w�肵�Ă��������B win32: "nul", *nix: "/dev/null"
     */
    public int write(AudioInputStream stream, Type fileType, File out) throws IOException {
        return write(stream, fileType, new FileOutputStream(out));
    }

    //----

    /**
     * @param args 0: input PCM, 1: output base dir, 2: length in seconds, 3: base file name, 4: type, 5: null device
     */
    public static void main(String[] args) throws Exception {
        String inFilename = args[0];
        String outDir = args[1];
        float time = Float.parseFloat(args[2]);
        String base = args[3];
        String model = args[4];
        String nullDevice = args[5]; // "nul" or "/dev/null"
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(inFilename));
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("mfi.divided", true);
        properties.put("mfi.directory", outDir);
        properties.put("mfi.base", base);
        properties.put("mfi.model", model);
        properties.put("mfi.time", time);
        properties.put("mfi.sampleRate", 8000);
        properties.put("mfi.bits", 4);
        properties.put("mfi.channels", 1);
        properties.put("mfi.masterVolume", 100);
        properties.put("mfi.adpcmVolume", 100);
        AudioSystem.write(ais, new MFi(properties), new File(nullDevice));
        System.exit(0);
    }
}

/* */
