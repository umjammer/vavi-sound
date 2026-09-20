/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * What a stream of an engine is, and is not: the stream beside it, or the one this number was
 * in the song before.
 * <p>
 * The engines play to {@link AudioEngineMixer} here, what a line would have got is pulled and
 * no audio device is needed.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-20 nsano initial version <br>
 */
class AudioEngineStreamTest {

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

    /** frames of an 8 bit unsigned constant, {@link PcmAudioEngine} plays it as it is */
    static byte[] constant(int frames, int value) {
        byte[] pcm = new byte[frames];
        Arrays.fill(pcm, (byte) value);
        return pcm;
    }

    /** what {@link PcmAudioEngine} makes of one of those bytes, as 16 bit */
    static int pcm16(int value) {
        return (value - 0x80) << 8;
    }

    /** the mixer's output at 8 kHz, [0] left and [1] right */
    static int[][] rendered(int frames) {
        short[] buffer = new short[frames * 2];
        AudioEngineMixer.render(buffer, 0, frames, 8000);
        int[][] out = new int[2][frames];
        for (int i = 0; i < frames; i++) {
            out[0][i] = buffer[i * 2];
            out[1][i] = buffer[i * 2 + 1];
        }
        return out;
    }

    /** one channel of it, 0: left, 1: right */
    static int[] rendered(int frames, int channel) {
        return rendered(frames)[channel];
    }

    /**
     * Two mono waves of its own, of the numbers a stereo pair would have had. Each is heard by
     * itself: the one beside it is not the other half of it, and it is played when it is asked
     * for and not only as the other half of the one before it.
     *
     * @see PcmAudioEngine#getChannels
     */
    @Test
    void aMonoStreamIsNotHalfOfThePairOfItsNumber() {
        PcmAudioEngine engine = new PcmAudioEngine();
        engine.setData(2, -1, 8000, 8, 1, constant(800, 0x90), false);
        engine.setData(3, -1, 8000, 8, 1, constant(800, 0xa0), false);

        engine.start(2);
        int[][] out = rendered(400);
        assertEquals(pcm16(0x90), out[0][200]);
        assertEquals(pcm16(0x90), out[1][200]); // mono is heard on both sides, not the wave beside it
        assertNotEquals(pcm16(0xa0), out[1][200]);

        AudioEngineMixer.clear();
        engine.start(3);
        assertEquals(pcm16(0xa0), rendered(400, 0)[200]); // and 3 is heard at all
    }

    /** a stream which says it is stereo still is: L then R in the one stream */
    @Test
    void aStereoStreamIsStillStereo() {
        PcmAudioEngine engine = new PcmAudioEngine();
        byte[] lr = new byte[1600];
        Arrays.fill(lr, 0, 800, (byte) 0x90);
        Arrays.fill(lr, 800, 1600, (byte) 0xa0);
        engine.setData(2, -1, 8000, 8, 2, lr, false);

        engine.start(2);
        int[][] out = rendered(400);
        assertEquals(pcm16(0x90), out[0][200]);
        assertEquals(pcm16(0xa0), out[1][200]);
    }

    /**
     * A stream number stored again is what the message which stores it says, not what the
     * stream of that number was: a stereo one stored again as mono was played as its own two
     * halves.
     */
    @Test
    void storingAStreamAgainReplacesItsFormat() {
        PcmAudioEngine engine = new PcmAudioEngine();
        engine.setData(2, -1, 8000, 8, 2, constant(1600, 0x90), false);
        engine.setData(2, -1, 4000, 8, 1, constant(800, 0xa0), false);

        engine.start(2);
        int[][] out = rendered(800);
        assertEquals(pcm16(0xa0), out[0][400]);
        assertEquals(pcm16(0xa0), out[1][400]); // mono, not the second half of it
        // 800 frames at the 4 kHz stored with them, not the 8 kHz of the stereo one before
        assertTrue(AudioEngineMixer.isPlaying());
    }

    /** the parts of a wave sent in packets are one wave */
    @Test
    void aContinuedStreamIsPutTogether() {
        PcmAudioEngine engine = new PcmAudioEngine();
        engine.setData(2, -1, 8000, 8, 1, constant(400, 0x90), true);
        engine.setData(2, -1, 8000, 8, 1, constant(400, 0xa0), false);

        engine.start(2);
        int[] left = rendered(800, 0);
        assertEquals(pcm16(0x90), left[200]);
        assertEquals(pcm16(0xa0), left[600]);
    }

    /**
     * A continue flag standing on a stream which has been played does not put the next wave of
     * that number after it: the one before it and this one were heard one after the other.
     */
    @Test
    void aPlayedStreamDoesNotKeepTheNextWave() {
        PcmAudioEngine engine = new PcmAudioEngine();
        engine.setData(2, -1, 8000, 8, 1, constant(400, 0x90), true); // the flag is left standing
        engine.start(2);
        AudioEngineMixer.clear();

        engine.setData(2, -1, 8000, 8, 1, constant(400, 0xa0), false);
        engine.start(2);
        int[] left = rendered(800, 0);
        assertEquals(pcm16(0xa0), left[200]); // this wave from its first frame
        assertEquals(0, left[600]);           // and nothing after it
    }

    /** a song is over, what it stored is not the next song's */
    @Test
    void resetForgetsTheStreams() {
        PcmAudioEngine engine = new PcmAudioEngine();
        engine.setData(2, -1, 8000, 8, 1, constant(800, 0x90), false);

        AudioEngine.resetAll();

        engine.start(2);
        assertFalse(AudioEngineMixer.isPlaying());
        assertEquals(0, rendered(400, 0)[200]);
    }
}
