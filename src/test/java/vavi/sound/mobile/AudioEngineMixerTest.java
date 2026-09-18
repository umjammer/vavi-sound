/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * AudioEngineMixerTest.
 * <p>
 * the engines without a line: what they would have written is pulled, no audio device needed.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
class AudioEngineMixerTest {

    private String previousOutput, previousVolume;

    @BeforeEach
    void setUp() {
        previousOutput = System.getProperty(AudioEngineMixer.OUTPUT_KEY);
        previousVolume = System.getProperty("vavi.sound.mobile.AudioEngine.volume");
        System.setProperty(AudioEngineMixer.OUTPUT_KEY, "mixer");
        System.setProperty("vavi.sound.mobile.AudioEngine.volume", "1.0");
        AudioEngineMixer.clear();
    }

    @AfterEach
    void tearDown() {
        AudioEngineMixer.clear();
        restore(AudioEngineMixer.OUTPUT_KEY, previousOutput);
        restore("vavi.sound.mobile.AudioEngine.volume", previousVolume);
    }

    private static void restore(String key, String value) {
        if (value == null) System.clearProperty(key);
        else System.setProperty(key, value);
    }

    /** raw 16 bit pcm pass-through engine */
    static class TestEngine extends BasicAudioEngine {
        TestEngine() {
            data = new Data[4];
        }
        @Override public boolean accept(int format) { return true; }
        @Override protected int getChannels(int streamNumber) { return data[streamNumber].channels; }
        @Override protected InputStream[] getInputStreams(int streamNumber, int channels) {
            InputStream[] iss = new InputStream[2];
            byte[] pcm = data[streamNumber].adpcm;
            if (channels == 1) {
                iss[0] = new ByteArrayInputStream(pcm);
            } else {
                iss[0] = new ByteArrayInputStream(pcm, 0, pcm.length / 2);
                iss[1] = new ByteArrayInputStream(pcm, pcm.length / 2, pcm.length / 2);
            }
            return iss;
        }
        @Override protected OutputStream getOutputStream(OutputStream os) { return os; }
    }

    /** a mono stream of {@code frames} frames of {@code value} at 8 kHz */
    static byte[] constant(int frames, short value) {
        byte[] pcm = new byte[frames * 2];
        for (int i = 0; i < frames; i++) {
            pcm[i * 2] = (byte) value;
            pcm[i * 2 + 1] = (byte) (value >> 8);
        }
        return pcm;
    }

    static TestEngine engine(byte[] pcm) {
        TestEngine engine = new TestEngine();
        engine.setData(0, 0, 8000, 16, 1, pcm, false);
        return engine;
    }

    /** frames that are not silent in the left channel */
    static int sounding(short[] buffer) {
        int n = 0;
        for (int i = 0; i < buffer.length; i += 2) {
            if (buffer[i] != 0) n++;
        }
        return n;
    }

    @Test
    void theDefaultIsTheLine() {
        System.clearProperty(AudioEngineMixer.OUTPUT_KEY);
        // unless a synthesizer left open by another test mixes them
        assertEquals(AudioEngineMixer.attached() > 0, AudioEngineMixer.isEnabled());
        System.setProperty(AudioEngineMixer.OUTPUT_KEY, "line");
        assertFalse(AudioEngineMixer.isEnabled());
    }

    @Test
    void aSynthesizerMixingThemTurnsItOn() {
        System.clearProperty(AudioEngineMixer.OUTPUT_KEY);
        int before = AudioEngineMixer.attached(); // synthesizers another test left open
        assertTrue(AudioEngineMixer.attach());
        try {
            assertEquals(before + 1, AudioEngineMixer.attached());
            assertTrue(AudioEngineMixer.isEnabled());
            engine(constant(8000, (short) 1000)).start(0);
            assertTrue(AudioEngineMixer.isPlaying());
        } finally {
            AudioEngineMixer.detach();
        }
        assertEquals(before, AudioEngineMixer.attached());
        assertEquals(before > 0, AudioEngineMixer.isEnabled());
        if (before == 0) {
            assertFalse(AudioEngineMixer.isPlaying()); // dropped with the synthesizer
        }
    }

    @Test
    void lineSaysNoToASynthesizer() {
        System.setProperty(AudioEngineMixer.OUTPUT_KEY, "line");
        int before = AudioEngineMixer.attached();
        assertFalse(AudioEngineMixer.attach());
        assertEquals(before, AudioEngineMixer.attached());
        assertFalse(AudioEngineMixer.isEnabled());
    }

    @Test
    void noLineIsOpened() {
        TestEngine engine = engine(constant(800, (short) 1000));
        assertNull(engine.line);
        engine.start(0);
        assertNull(engine.line);
        assertTrue(AudioEngineMixer.isPlaying());
    }

    @Test
    void theStreamComesOutAtTheSameRate() {
        engine(constant(800, (short) 1000)).start(0);
        short[] buffer = new short[2000 * 2];
        AudioEngineMixer.render(buffer, 0, 2000, 8000);
        // 800 frames, give or take the one the interpolation starts from
        int n = sounding(buffer);
        assertTrue(Math.abs(n - 800) <= 1, "sounding: " + n);
        assertEquals(1000, buffer[200]);
        assertEquals(1000, buffer[201]); // mono is heard on both sides
        assertFalse(AudioEngineMixer.isPlaying());
    }

    @Test
    void theStreamIsResampledToTheOutput() {
        engine(constant(800, (short) 1000)).start(0);
        short[] buffer = new short[8000 * 2];
        AudioEngineMixer.render(buffer, 0, 8000, 44100);
        // 100 ms at 44.1 kHz
        int n = sounding(buffer);
        assertTrue(Math.abs(n - 4410) <= 6, "sounding: " + n);
    }

    @Test
    void theGateTimeCutsIt() {
        engine(constant(8000, (short) 1000)).start(0, 500);
        short[] buffer = new short[8000 * 2];
        AudioEngineMixer.render(buffer, 0, 8000, 8000);
        int n = sounding(buffer);
        assertTrue(Math.abs(n - 4000) <= 1, "sounding: " + n);
    }

    @Test
    void aStopCutsItThere() {
        TestEngine engine = engine(constant(8000, (short) 1000));
        engine.start(0);
        short[] buffer = new short[1000 * 2];
        AudioEngineMixer.render(buffer, 0, 1000, 8000);
        engine.stop(0);
        assertFalse(AudioEngineMixer.isPlaying());
        short[] after = new short[1000 * 2];
        AudioEngineMixer.render(after, 0, 1000, 8000);
        assertEquals(0, sounding(after));
    }

    @Test
    void itIsMixedOverWhatIsThere() {
        engine(constant(800, (short) 1000)).start(0);
        short[] buffer = new short[400 * 2];
        java.util.Arrays.fill(buffer, (short) 500);
        AudioEngineMixer.render(buffer, 0, 400, 8000);
        assertEquals(1500, buffer[200]);
    }

    @Test
    void theVolumeIsApplied() {
        System.setProperty("vavi.sound.mobile.AudioEngine.volume", "0.5");
        engine(constant(800, (short) 1000)).start(0);
        short[] buffer = new short[400 * 2];
        AudioEngineMixer.render(buffer, 0, 400, 8000);
        assertEquals(500, buffer[200]);
    }

    @Test
    void startingAStreamAgainRestartsIt() {
        TestEngine engine = engine(constant(800, (short) 1000));
        engine.start(0);
        engine.start(0);
        short[] buffer = new short[400 * 2];
        AudioEngineMixer.render(buffer, 0, 400, 8000);
        assertEquals(1000, buffer[200]); // once, not twice over
    }

    @Test
    void twoStreamsSoundTogether() {
        TestEngine engine = new TestEngine();
        engine.setData(0, 0, 8000, 16, 1, constant(800, (short) 1000), false);
        engine.setData(1, 1, 8000, 16, 1, constant(800, (short) 300), false);
        engine.start(0);
        engine.start(1);
        short[] buffer = new short[400 * 2];
        AudioEngineMixer.render(buffer, 0, 400, 8000);
        assertEquals(1300, buffer[200]);
    }

    @Test
    void closingTheEngineStopsItsStreams() {
        TestEngine engine = engine(constant(8000, (short) 1000));
        engine.start(0);
        engine.close();
        assertFalse(AudioEngineMixer.isPlaying());
    }

    @Test
    void syncRunsOnTheCallersThread() {
        AtomicReference<Thread> ran = new AtomicReference<>();
        AudioEngine.Sync.schedule(() -> ran.set(Thread.currentThread()));
        assertSame(Thread.currentThread(), ran.get());
        ran.set(null);
        AudioEngine.Sync.scheduleStop(() -> ran.set(Thread.currentThread()));
        assertSame(Thread.currentThread(), ran.get());
    }

    /** a real codec: yamaha adpcm, encoded and played back through the mixer */
    @Test
    void anAdpcmStreamIsDecoded() {
        YamahaAudioEngine engine = new YamahaAudioEngine();
        int frames = 8000;
        byte[] pcm = new byte[frames * 2];
        for (int i = 0; i < frames; i++) {
            short v = (short) (Math.sin(2 * Math.PI * 440 * i / 8000.0) * 8000);
            pcm[i * 2] = (byte) v;
            pcm[i * 2 + 1] = (byte) (v >> 8);
        }
        byte[] adpcm = engine.encode(4, 1, pcm);
        engine.setData(0, 0, 8000, 4, 1, adpcm, false);
        engine.start(0);
        short[] buffer = new short[frames * 2];
        AudioEngineMixer.render(buffer, 0, frames, 8000);

        // correlates with what went in
        double xy = 0, xx = 0, yy = 0;
        for (int i = 100; i < frames - 100; i++) {
            double x = (short) ((pcm[i * 2] & 0xff) | (pcm[i * 2 + 1] << 8));
            double y = buffer[i * 2];
            xy += x * y;
            xx += x * x;
            yy += y * y;
        }
        double r = xy / Math.sqrt(xx * yy);
        assertTrue(r > 0.9, "correlation: " + r);
        engine.close();
    }
}
