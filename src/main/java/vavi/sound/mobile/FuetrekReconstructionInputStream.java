/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.IOException;
import java.io.InputStream;


/**
 * The output stage of the fuetrek sound source for its ADPCM: the decoded 8 or 16 kHz
 * samples are zero stuffed up to 32 kHz and go through a 3 stage IIR low pass.
 * <p>
 * The decoder itself (G.726 16/32 kbit, G.723_16 and G.721 here) comes out the same, what
 * the sound source sounds like as against a plain 16 kHz playback is this.
 * The coefficients are the ones of {@code MFiSoundLibMFi5.dll} as recovered by openDoJa's
 * {@code MLDNativeADPCMDecoder}.
 * <p>
 * in: 16 bit signed little endian mono, out: the same at {@link #OUTPUT_SAMPLE_RATE}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-15 nsano initial version <br>
 */
public class FuetrekReconstructionInputStream extends InputStream {

    /** the rate the sound source mixes at */
    public static final int OUTPUT_SAMPLE_RATE = 32000;

    /** a sample rate and its filter */
    private enum Profile {
        /** 8 kHz, 4 times */
        RATE_8K(8000, 4,
                new double[] { 0.375, -0.00390625, 0.375, 1.4150390625, -0.6875 },
                0.04296875, new double[] { 0.0654296875, 0.04296875, 1.439453125, -0.54296875 },
                0.306640625, new double[] { -0.171875, 0.306640625, 1.4462890625, -0.8896484375 }),
        /** 16 kHz, 2 times */
        RATE_16K(16000, 2,
                new double[] { 0.207692, 0.36527, 0.207692, 0.892037, -0.282364 },
                0.35532, new double[] { 0.289359, 0.35532, 0.588666, -0.588666 },
                0.631808, new double[] { 0.226858, 0.631808, 0.385442, -0.875916 });

        final int sampleRate;
        final int factor;
        /** x0, x1, x2, y1, y2 */
        final double[] stage0;
        final double gain1;
        /** x1, x2, y1, y2 */
        final double[] stage1;
        final double gain2;
        /** x1, x2, y1, y2 */
        final double[] stage2;

        Profile(int sampleRate, int factor, double[] stage0, double gain1, double[] stage1, double gain2, double[] stage2) {
            this.sampleRate = sampleRate;
            this.factor = factor;
            this.stage0 = stage0;
            this.gain1 = gain1;
            this.stage1 = stage1;
            this.gain2 = gain2;
            this.stage2 = stage2;
        }
    }

    /** @return whether the sample rate has the output stage */
    public static boolean isSupported(int sampleRate) {
        return profile(sampleRate) != null;
    }

    private static Profile profile(int sampleRate) {
        for (Profile profile : Profile.values()) {
            if (profile.sampleRate == sampleRate) return profile;
        }
        return null;
    }

    private final InputStream in;
    private final Profile profile;

    /** input history */
    private double x0, x1, x2;
    /** stage 0, 1, 2 output histories */
    private double s0h0, s0h1, s1h0, s1h1, s2h0, s2h1;
    private int phase;
    private boolean end;
    /** the high byte of the current output sample, -1 if none */
    private int pending = -1;

    /**
     * @param in 16 bit signed little endian mono pcm
     * @param sampleRate 8000 or 16000
     */
    public FuetrekReconstructionInputStream(InputStream in, int sampleRate) {
        this.in = in;
        this.profile = profile(sampleRate);
        if (profile == null) {
            throw new IllegalArgumentException("no output stage for " + sampleRate + " Hz");
        }
    }

    /** @return the next output sample, or {@code Integer.MIN_VALUE} at the end */
    private int next() throws IOException {
        if (end) return Integer.MIN_VALUE;

        double[] c = profile.stage0;
        double stage0 = x0 * c[0] + x1 * c[1] + x2 * c[2] + s0h0 * c[3] + s0h1 * c[4];
        c = profile.stage1;
        double stage1 = stage0 * profile.gain1 + s0h0 * c[0] + s0h1 * c[1] + s1h0 * c[2] + s1h1 * c[3];
        c = profile.stage2;
        double stage2 = stage1 * profile.gain2 + s1h0 * c[0] + s1h1 * c[1] + s2h0 * c[2] + s2h1 * c[3];
        int output = level((int) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (long) stage2)));

        s2h1 = s2h0;
        s2h0 = stage2;
        s1h1 = s1h0;
        s1h0 = stage1;
        s0h1 = s0h0;
        s0h0 = stage0;
        x2 = x1;
        x1 = x0;
        if (phase == 0) {
            int lo = in.read();
            int hi = lo < 0 ? -1 : in.read();
            if (hi < 0) {
                end = true;
                return Integer.MIN_VALUE;
            }
            x0 = (short) (lo | (hi << 8));
        } else {
            x0 = 0;
        }
        phase = (phase + 1) % profile.factor;
        return output;
    }

    /** the level (4095) and the pan (4095) of a lane, in Q12, truncated */
    private static int level(int sample) {
        for (int i = 0; i < 2; i++) {
            int product = sample * 4095;
            sample = (product + (product < 0 ? 0x0fff : 0)) >> 12;
        }
        return sample;
    }

    @Override
    public int read() throws IOException {
        if (pending >= 0) {
            int b = pending;
            pending = -1;
            return b;
        }
        int sample = next();
        if (sample == Integer.MIN_VALUE) return -1;
        pending = (sample >> 8) & 0xff;
        return sample & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = 0;
        while (n < len) {
            int c = read();
            if (c < 0) return n == 0 ? -1 : n;
            b[off + n++] = (byte) c;
        }
        return n;
    }

    @Override
    public int available() throws IOException {
        if (end) return pending >= 0 ? 1 : 0;
        // every input sample makes factor output ones
        return in.available() * profile.factor + (pending >= 0 ? 1 : 0) + (phase != 0 ? 2 * (profile.factor - phase) : 0);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
