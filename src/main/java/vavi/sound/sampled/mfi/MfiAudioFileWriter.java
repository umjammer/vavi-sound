/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.mfi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileWriter;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.util.Debug;


/**
 * MfiAudioFileWriter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051222 nsano initial version <br>
 */
public class MfiAudioFileWriter extends AudioFileWriter {

    /** */
    private static final Type[] outputTypes = new Type[] {
        new MFi(null) // TODO null
    };

    @Override
    public Type[] getAudioFileTypes() {
        return outputTypes;
    }

    @Override
    public Type[] getAudioFileTypes(AudioInputStream stream) {
        return getAudioFileTypes(); // TODO check stream
    }

    /**
     * {@inheritDoc}
     *
     * @param fileType must be instance of {@link MFi}
     * @param out specify null device. e.g. win32: "nul", *nix: "/dev/null"
     * @throws IllegalArgumentException when fileType is not MFi
     * @throws NullPointerException when fileType is not set in properties
     */
    @Override
    public int write(AudioInputStream stream, Type fileType, OutputStream out) throws IOException {
        if (!isFileTypeSupported(fileType, stream)) {
            throw new IllegalArgumentException("unsupported fileType: " + fileType.getClass().getName());
        }
        try {
            // properties
            boolean divided = (Boolean) ((MFi) fileType).getProperty("mfi.divided");
            if (divided) {
                // division
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
                // single unit
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
        } catch (UnsupportedAudioFileException | InvalidMfiDataException e) {
            throw (IOException) new IOException(e);
        } catch (IllegalArgumentException e) {
Debug.printStackTrace(e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fileType must be instance of {@link MFi}
     * @param out specify null device. e.g. win32: "nul", *nix: "/dev/null"
     */
    @Override
    public int write(AudioInputStream stream, Type fileType, File out) throws IOException {
        return write(stream, fileType, Files.newOutputStream(out.toPath()));
    }
}
