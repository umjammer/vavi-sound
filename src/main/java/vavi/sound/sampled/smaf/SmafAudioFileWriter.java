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
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.spi.AudioFileWriter;

import vavi.sound.smaf.InvalidSmafDataException;


/**
 * SmafAudioFileWriter.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080415 nsano initial version <br>
 */
public class SmafAudioFileWriter extends AudioFileWriter {

    /** */
    private static final Type[] outputTypes = new Type[] {
        new SMAF(null) // TODO null
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
     * @param fileType must be instance of {@link SMAF}
     * @param out null �o�͂��w�肵�Ă��������B win32: "nul", *nix: "/dev/null"
     * @throws IllegalArgumentException fileType �� SMAF �łȂ��ꍇ�X���[����܂��B
     * @throws NullPointerException fileType �� properties ���ݒ肳��Ă��Ȃ��ꍇ�X���[����܂��B
     */
    public int write(AudioInputStream stream, Type fileType, OutputStream out) throws IOException {
        if (!isFileTypeSupported(fileType, stream)) {
            throw new IllegalArgumentException("unsupported fileType: " + fileType.getClass().getName());
        }
        try {
            // properties
            boolean divided = (Boolean) ((SMAF) fileType).getProperty("smaf.divided");
            if (divided) {
                // ����
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
                // �P��
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
        } catch (UnsupportedAudioFileException e) {
            throw (IOException) new IOException().initCause(e);
        } catch (InvalidSmafDataException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    /**
     * �w�肳�ꂽ�t�@�C���^�̃I�[�f�B�I�t�@�C����\���o�C�g�̃X�g���[�����A�w�肳�ꂽ�o�̓X�g���[���֏������݂܂��B
     * @param fileType must be instance of {@link SMAF}
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
        String nullDevice = args[4]; // "nul" or "/dev/null"
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(inFilename));
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("smaf.divided", true);
        properties.put("smaf.directory", outDir);
        properties.put("smaf.base", base);
        properties.put("smaf.time", time);
        properties.put("smaf.sampleRate", 8000);
        properties.put("smaf.bits", 4);
        properties.put("smaf.channels", 1);
        properties.put("smaf.masterVolume", 100);
        properties.put("smaf.adpcmVolume", 100);
        AudioSystem.write(ais, new SMAF(properties), new File(nullDevice));
        System.exit(0);
    }
}

/* */
