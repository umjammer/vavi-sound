/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import vavi.sound.LimitedInputStream;
import vavi.util.Debug;
import vavi.util.win32.Chunk;
import vavi.util.win32.WAVE;


/**
 * Provider for some ADPCM audio files reading services. This implementation can parse
 * the format information from WAVE audio file, and can produce audio input
 * streams from files of this type.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 240316 nsano initial version <br>
 */
public abstract class AdpcmWaveAudioFileReader extends AudioFileReader {

    @Override
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return getAudioFileFormat(new BufferedInputStream(inputStream), (int) file.length());
        }
    }

    @Override
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        try (InputStream inputStream = url.openStream()) {
            return getAudioFileFormat(inputStream instanceof BufferedInputStream ? inputStream : new BufferedInputStream(inputStream));
        }
    }

    @Override
    public AudioFileFormat getAudioFileFormat(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioFileFormat(stream, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * @see "https://www.iana.org/assignments/wave-avi-codec-registry/wave-avi-codec-registry.xhtml"
     */
    protected abstract int getFormatCode();

    /** @return encoding for return */
    protected abstract AudioFormat.Encoding getEncoding();

    /** @return size needed for wave file header analysis */
    protected int getBufferSize() {
        return 512;
    }

    /** @param fmt wave file header */
    protected abstract Map<String, Object> toProperties(WAVE.fmt fmt);

    /**
     * Returns the AudioFileFormat from the given InputStream. Implementation.
     *
     * @param bitStream input to decode
     * @param mediaLength unused
     * @return an AudioInputStream object based on the audio file data contained
     * in the input stream.
     * @throws UnsupportedAudioFileException if the File does not point to a
     *                                       valid audio file data recognized by the system.
     * @throws IOException                   if an I/O exception occurs.
     */
    protected AudioFileFormat getAudioFileFormat(InputStream bitStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
Debug.println(Level.FINER, "enter available: " + bitStream.available() + ", " + getClass().getSimpleName());
        float sampleRate;
        int channels;
        AudioFormat.Encoding encoding;
        Map<String, Object> properties;
        try {
            int bufferSize = getBufferSize();
            bitStream.mark(bufferSize);
            LimitedInputStream is = new LimitedInputStream(bitStream, bufferSize);
            Map<String, Object> context = new HashMap<>();
            context.put(WAVE.CHUNK_PARSE_STRICT_KEY, true);
            context.put(WAVE.MULTIPART_CHUNK_PARSE_STRICT_KEY, true);
            context.put(WAVE.WAVE_DATA_NOT_LOAD_KEY, true);
            WAVE wave = Chunk.readFrom(is, WAVE.class, context);
            WAVE.fmt fmt = wave.findChildOf(WAVE.fmt.class);
            int formatCode = fmt.getFormatId();
 Debug.println(Level.FINER, "formatCode: " + formatCode);
            if (formatCode != getFormatCode()) {
 Debug.println(Level.FINER, "unsupported wave format code: " + formatCode);
                throw new UnsupportedAudioFileException("unsupported wave format code: " + formatCode);
            }
            sampleRate = fmt.getSamplingRate();
            channels = fmt.getNumberChannels();
            properties = toProperties(fmt);
 Debug.println(Level.FINER, "properties: " + properties);
        } catch (IOException e) {
            if (e.getMessage().equals(LimitedInputStream.ERROR_MESSAGE_REACHED_TO_LIMIT)) {
Debug.println(Level.FINER, e);
Debug.printStackTrace(Level.FINEST, e);
                throw (UnsupportedAudioFileException) new UnsupportedAudioFileException(e.getMessage()).initCause(e);
            } else {
                throw e;
            }
        } catch (Exception e) {
Debug.println(Level.FINER, e);
Debug.printStackTrace(Level.FINEST, e);
            throw (UnsupportedAudioFileException) new UnsupportedAudioFileException(e.getMessage()).initCause(e);
        } finally {
            try {
                bitStream.reset();
            } catch (IOException e) {
                if (Debug.isLoggable(Level.FINEST))
Debug.printStackTrace(e);
                else
Debug.println(Level.FINE, e);
            }
Debug.println(Level.FINER, "finally available: " + bitStream.available());
        }
        AudioFormat format;
        if (properties == null) {
            format = new AudioFormat(getEncoding(), sampleRate, AudioSystem.NOT_SPECIFIED, channels, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false);
        } else {
            format = new AudioFormat(getEncoding(), sampleRate, AudioSystem.NOT_SPECIFIED, channels, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false, properties);
        }
        return new AudioFileFormat(Type.WAVE, format, AudioSystem.NOT_SPECIFIED);
    }

    @Override
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = Files.newInputStream(file.toPath());
        try {
            return getAudioInputStream(new BufferedInputStream(inputStream), (int) file.length());
        } catch (UnsupportedAudioFileException | IOException e) {
            inputStream.close();
            throw e;
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = url.openStream();
        try {
            return getAudioInputStream(inputStream instanceof BufferedInputStream ? inputStream : new BufferedInputStream(inputStream));
        } catch (UnsupportedAudioFileException | IOException e) {
            inputStream.close();
            throw e;
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(stream, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Obtains an audio input stream from the input stream provided. The stream
     * must point to valid audio file data.
     *
     * @param inputStream the input stream from which the AudioInputStream
     *                    should be constructed.
     * @param mediaLength unused
     * @return an AudioInputStream object based on the audio file data contained
     * in the input stream.
     * @throws UnsupportedAudioFileException if the File does not point to a
     *                                       valid audio file data recognized by the system.
     * @throws IOException                   if an I/O exception occurs.
     */
    protected AudioInputStream getAudioInputStream(InputStream inputStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat audioFileFormat = getAudioFileFormat(inputStream, mediaLength);
        // TODO super cutting corner, should get data position in above method and set it in props and skip here
        Map<String, Object> context = new HashMap<>();
        context.put(WAVE.CHUNK_PARSE_STRICT_KEY, true);
        context.put(WAVE.MULTIPART_CHUNK_PARSE_STRICT_KEY, true);
        context.put(WAVE.WAVE_DATA_NOT_LOAD_KEY, true);
        WAVE wave = Chunk.readFrom(inputStream, WAVE.class, context);
        return new AudioInputStream(inputStream, audioFileFormat.getFormat(), audioFileFormat.getFrameLength());
    }
}
