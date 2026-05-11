/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.misc;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;


/**
 * HijackSourceDataLine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-04-27 nsano initial version <br>
 */
public class HijackSourceDataLine implements SourceDataLine {

    private static final Logger logger = System.getLogger(HijackSourceDataLine.class.getName());

    private AudioFormat format;
    private long totalBytes;
    private boolean running;
    private boolean open;

    // For Throttling
    private long startTime;
    private int bufferSize = 4096;

    public static HijackLineListener specialListener; // TODO gross

    @Override
    public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
        this.format = format;
        this.bufferSize = bufferSize > 0 ? bufferSize : 4096;
        this.open = true;
    }

    @Override
    public void open(AudioFormat format) throws LineUnavailableException {
        open(format, 0);
    }

    @Override
    public int write(byte[] b, int off, int len) {
        if (!open) return 0;

        fireUpdate(new HijackLineEvent(this, HijackType.WRITE, totalBytes, Arrays.copyOfRange(b, off, off + len)));
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
    }

    @Override
    public void drain() {
        flush();
    }

    @Override
    public void flush() {
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
            throw new IllegalStateException("format not set, use open(AudioFormat) or open(AudioFormat, int) for wave out");
        }
        open(format);
    }

    @Override
    public void close() {
        if (!open) return;
        running = false;
        open = false;
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

    public static class HijackLineEvent extends LineEvent {
        private byte[] data;
        public HijackLineEvent(Line line, Type type, long position, byte[] data) {
            super(line, type, position);
            this.data = data;
        }
        public byte[] getData() {
            return data;
        }
    }

    public static class HijackType extends LineEvent.Type {
        protected HijackType(String name) {
            super(name);
        }

        public static final HijackType WRITE = new HijackType("WRITE");
    }

    public interface HijackLineListener extends LineListener {
    }

    private final List<HijackLineListener> listeners = new ArrayList<>();

    private void fireUpdate(LineEvent event) {
        listeners.forEach(l -> l.update(event));
        if (specialListener != null) specialListener.update(event); // TODO gross
    }

    @Override
    public void addLineListener(LineListener listener) {
        if (listener instanceof HijackLineListener hijackLineListener) {
            listeners.add(hijackLineListener);
        } else {
            throw new IllegalArgumentException("only accept HijackLineListener");
        }
    }

    @Override
    public void removeLineListener(LineListener listener) {
        if (listener instanceof HijackLineListener hijackLineListener) {
            listeners.remove(hijackLineListener);
        } else {
            throw new IllegalArgumentException("only accept HijackLineListener");
        }
    }
}
