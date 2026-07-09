/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * BasicAudioEngineGateTimeTest.
 * <p>
 * frame accurate gate time, no real audio device needed.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-03 nsano initial version <br>
 */
class BasicAudioEngineGateTimeTest {

    /** records written bytes instead of playing */
    static class FakeLine implements SourceDataLine {
        final AudioFormat format;
        long bytesWritten;
        FakeLine(AudioFormat format) {
            this.format = format;
        }
        @Override public AudioFormat getFormat() { return format; }
        @Override public int write(byte[] b, int off, int len) { bytesWritten += len; return len; }
        @Override public Control getControl(Control.Type control) { throw new IllegalArgumentException("no controls"); }
        @Override public Control[] getControls() { return new Control[0]; }
        @Override public boolean isControlSupported(Control.Type control) { return false; }
        @Override public void open(AudioFormat format, int bufferSize) {}
        @Override public void open(AudioFormat format) {}
        @Override public void open() {}
        @Override public void close() {}
        @Override public boolean isOpen() { return true; }
        @Override public void drain() {}
        @Override public void flush() {}
        @Override public void start() {}
        @Override public void stop() {}
        @Override public boolean isRunning() { return true; }
        @Override public boolean isActive() { return true; }
        @Override public int getBufferSize() { return 8192; }
        @Override public int available() { return 8192; }
        @Override public int getFramePosition() { return 0; }
        @Override public long getLongFramePosition() { return 0; }
        @Override public long getMicrosecondPosition() { return 0; }
        @Override public float getLevel() { return 0; }
        @Override public Line.Info getLineInfo() { return null; }
        @Override public void addLineListener(LineListener listener) {}
        @Override public void removeLineListener(LineListener listener) {}
    }

    /** raw pcm pass-through engine over an injected fake line */
    static class TestEngine extends BasicAudioEngine {
        TestEngine(FakeLine line) {
            data = new Data[4];
            this.line = line;
        }
        @Override public boolean accept(int format) { return true; }
        @Override protected int getChannels(int streamNumber) { return data[streamNumber].channels; }
        @Override protected InputStream[] getInputStreams(int streamNumber, int channels) {
            InputStream[] iss = new InputStream[2];
            byte[] adpcm = data[streamNumber].adpcm;
            if (channels == 1) {
                iss[0] = new ByteArrayInputStream(adpcm);
            } else {
                iss[0] = new ByteArrayInputStream(adpcm, 0, adpcm.length / 2);
                iss[1] = new ByteArrayInputStream(adpcm, adpcm.length / 2, adpcm.length / 2);
            }
            return iss;
        }
        @Override protected OutputStream getOutputStream(OutputStream os) { return os; }
    }

    static TestEngine engine(FakeLine line, int channels, int bytes) {
        TestEngine engine = new TestEngine(line);
        AudioEngine.Data datum = new AudioEngine.Data();
        datum.channel = 0;
        datum.sampleRate = 8000;
        datum.bits = 16;
        datum.channels = channels;
        datum.adpcm = new byte[bytes];
        engine.data[0] = datum;
        return engine;
    }

    static AudioFormat format(int channels) {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, channels, 2 * channels, 8000, false);
    }

    @Test
    void monoGateTimeCutsAtExactFrame() {
        FakeLine line = new FakeLine(format(1));
        // 16000 bytes = 8000 frames = 1000 ms
        engine(line, 1, 16000).start(0, 500);
        // 500 ms * 8000 Hz = 4000 frames * 2 bytes
        assertEquals(4000 * 2, line.bytesWritten);
    }

    @Test
    void monoGateTimeRounds() {
        FakeLine line = new FakeLine(format(1));
        engine(line, 1, 16000).start(0, 333);
        assertEquals(Math.round(333 * 8000 / 1000.0) * 2, line.bytesWritten);
    }

    @Test
    void monoNoGateTimePlaysAll() {
        FakeLine line = new FakeLine(format(1));
        engine(line, 1, 16000).start(0);
        assertEquals(16000, line.bytesWritten);
    }

    @Test
    void monoGateTimeLongerThanDataPlaysAll() {
        FakeLine line = new FakeLine(format(1));
        engine(line, 1, 16000).start(0, 5000);
        assertEquals(16000, line.bytesWritten);
    }

    @Test
    void stereoGateTimeCutsAtExactFrame() {
        FakeLine line = new FakeLine(format(2));
        // 16000 bytes = 8000 bytes/side = 4000 frames = 500 ms
        engine(line, 2, 16000).start(0, 100);
        // 100 ms * 8000 Hz = 800 frames * 4 bytes
        assertEquals(800 * 4, line.bytesWritten);
    }

    @Test
    void stereoNoGateTimePlaysAll() {
        FakeLine line = new FakeLine(format(2));
        engine(line, 2, 16000).start(0, -1);
        assertEquals(16000, line.bytesWritten);
    }
}
