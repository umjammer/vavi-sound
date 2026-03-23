/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.misc;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;


/**
 * WaveOutSourceDataLine.
 * <p>
 * system property
 * <li>{@code vavi.sound.sampled.misc.waveout} ... wave out file path (get after {@link #close})</li>
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-18 nsano initial version <br>
 * @see "https://github.com/fourthline/mmlTools/blob/master/src/jp/fourthline/mabiicco/midi/WavoutDataLine.java"
 */
public class WaveOutSourceDataLine implements SourceDataLine {

    private static final Logger logger = System.getLogger(WaveOutSourceDataLine.class.getName());

    private AudioFormat format;
    private Path tempPath;
    private OutputStream os;
    private long totalBytes;
    private boolean running;
    private boolean open;

    // For Throttling
    private long startTime;
    private int bufferSize = 4096;

    @Override
    public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
        this.format = format;
        this.bufferSize = bufferSize > 0 ? bufferSize : 4096;
        this.open = true;
        try {
            tempPath = Files.createTempFile("waveout", ".raw");
            os = Files.newOutputStream(tempPath);
            totalBytes = 0;
logger.log(Level.INFO, "open: " + format + ", " + tempPath);
        } catch (IOException e) {
            throw new LineUnavailableException(e.getMessage());
        }
    }

    @Override
    public void open(AudioFormat format) throws LineUnavailableException {
        open(format, 0);
    }

    @Override
    public int write(byte[] b, int off, int len) {
        if (!open || os == null) return 0;
        try {
            os.write(b, off, len);
            totalBytes += len;

            if (running && format != null) {
                int frameSize = format.getFrameSize();
                float sampleRate = format.getSampleRate();
                if (frameSize > 0 && sampleRate > 0) {
                    long expectedMs = (long) (totalBytes * 1000.0 / (frameSize * sampleRate));
                    long elapsedMs = System.currentTimeMillis() - startTime;
                    long waitTime = expectedMs - elapsedMs;
                    if (waitTime > 0) {
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }

            return len;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void drain() {
        flush();
    }

    @Override
    public void flush() {
        try {
            if (os != null) {
                os.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void start() {
        running = true;
        if (format != null) {
            int frameSize = format.getFrameSize();
            float sampleRate = format.getSampleRate();
            if (frameSize > 0 && sampleRate > 0) {
                long writtenMs = (long) (totalBytes * 1000.0 / (frameSize * sampleRate));
                startTime = System.currentTimeMillis() - writtenMs;
            } else {
                startTime = System.currentTimeMillis();
            }
        } else {
            startTime = System.currentTimeMillis();
        }
    }

    @Override
    public void stop() {
        running = false;
        flush();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isActive() {
        return running;
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public int available() {
        return bufferSize;
    }

    @Override
    public int getFramePosition() {
        return format == null || format.getFrameSize() <= 0 ? 0 : (int) (totalBytes / format.getFrameSize());
    }

    @Override
    public long getLongFramePosition() {
        return format == null || format.getFrameSize() <= 0 ? 0 : totalBytes / format.getFrameSize();
    }

    @Override
    public long getMicrosecondPosition() {
        return format == null || format.getSampleRate() <= 0 ? 0 : (long) (getLongFramePosition() * 1000000.0 / format.getSampleRate());
    }

    @Override
    public float getLevel() {
        return AudioSystem.NOT_SPECIFIED;
    }

    @Override
    public Line.Info getLineInfo() {
        return new Line.Info(SourceDataLine.class);
    }

    @Override
    public void open() throws LineUnavailableException {
        if (format == null) {
            throw new IllegalStateException("Format not set");
        }
        open(format);
    }

    @Override
    public void close() {
        if (!open) return;
        running = false;
        open = false;
        if (os != null) {
            try {
                os.close();
                Path path = Files.createTempFile(Path.of("tmp"), "waveout", ".wav");
                try (InputStream is = new BufferedInputStream(Files.newInputStream(tempPath))) {
                    long frameLength = format.getFrameSize() > 0 ? totalBytes / format.getFrameSize() : AudioSystem.NOT_SPECIFIED;
                    AudioInputStream ais = new AudioInputStream(is, format, frameLength);
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, path.toFile());
                }
                Files.deleteIfExists(tempPath);
                os = null;
logger.log(Level.INFO, "close: " + Files.size(path));
                System.setProperty("vavi.sound.sampled.misc.waveout", path.toString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public Control[] getControls() {
        return new Control[0];
    }

    @Override
    public boolean isControlSupported(Type control) {
        return false;
    }

    @Override
    public Control getControl(Type control) {
        throw new IllegalArgumentException("Control not supported: " + control);
    }

    @Override
    public void addLineListener(LineListener listener) {
    }

    @Override
    public void removeLineListener(LineListener listener) {
    }
}
