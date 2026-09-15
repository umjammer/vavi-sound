/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * FuetrekReconstructionInputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-15 nsano initial version <br>
 */
class FuetrekReconstructionInputStreamTest {

    private static byte[] dc(int samples, int value) {
        byte[] pcm = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            pcm[i * 2] = (byte) value;
            pcm[i * 2 + 1] = (byte) (value >> 8);
        }
        return pcm;
    }

    /** 16 kHz twice, 8 kHz 4 times, a dc settles at the level it came in (the zero stuffing is made up for) */
    @Test
    void upsamplesAndKeepsLevel() throws Exception {
        for (int[] rate : new int[][] {{16000, 2}, {8000, 4}}) {
            byte[] out = new FuetrekReconstructionInputStream(new ByteArrayInputStream(dc(4000, 10000)), rate[0]).readAllBytes();
            assertEquals(4000 * rate[1] * 2, out.length, "rate " + rate[0]);
            int last = (short) ((out[out.length - 2] & 0xff) | (out[out.length - 1] << 8));
            assertEquals(10000, last, 10000 * 0.02, "rate " + rate[0]);
        }
    }

    @Test
    void rates() {
        assertTrue(FuetrekReconstructionInputStream.isSupported(8000));
        assertTrue(FuetrekReconstructionInputStream.isSupported(16000));
        assertFalse(FuetrekReconstructionInputStream.isSupported(32000));
        assertThrows(IllegalArgumentException.class, () -> new FuetrekReconstructionInputStream(new ByteArrayInputStream(new byte[0]), 32000));
    }

    /** the line is at 32 kHz for 8 and 16 kHz ADPCM, unless the stage is turned off */
    @Test
    void lineRate() {
        FuetrekAudioEngine engine = new FuetrekAudioEngine();
        assertEquals(32000, engine.getAudioFormat(16000, 1).getSampleRate());
        assertEquals(32000, engine.getAudioFormat(32000, 1).getSampleRate());
        assertEquals(4000, engine.getAudioFormat(4000, 1).getSampleRate());
        System.setProperty("vavi.sound.mobile.FuetrekAudioEngine.reconstruction", "none");
        try {
            assertEquals(16000, engine.getAudioFormat(16000, 1).getSampleRate());
        } finally {
            System.clearProperty("vavi.sound.mobile.FuetrekAudioEngine.reconstruction");
        }
    }
}
