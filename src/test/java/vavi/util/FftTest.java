/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import vavi.sound.pcm.resampling.ssrc.SSRC;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * FftTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-08 nsano initial version <br>
 */
class FftTest {

    static final int NMAX = 1 << 17;

    /** the ssrc polyphase filter selection */
    static final String FIR = "vavi.sound.pcm.resampling.ssrc.fir";

    static double[] random(int n, long seed) {
        Random random = new Random(seed);
        double[] a = new double[n];
        for (int i = 0; i < n; i++) {
            a[i] = random.nextDouble() * 2 - 1;
        }
        return a;
    }

    static int[] ip(int n) {
        int[] ip = new int[2 + (int) Math.ceil(Math.sqrt(n / 2.0)) + 1];
        ip[0] = 0;
        return ip;
    }

    @Test
    @DisplayName("selection by name")
    void testInstance() {
        assertInstanceOf(ScalarFft.class, Fft.getInstance("scalar"));
        assertInstanceOf(VectorFft.class, Fft.getInstance("vector"));
        assertInstanceOf(VectorFft.class, Fft.getInstance("vavi.util.VectorFft"));
        assertInstanceOf(VectorRadix4Fft.class, Fft.getInstance("vectorradix4"));
        assertInstanceOf(ScalarFft.class, Fft.getInstance("no such fft"));
    }

    @Test
    @DisplayName("vector is the same as scalar")
    void testSame() {
        Fft scalar = new ScalarFft();
        Fft vector = new VectorFft();
        for (int n = 4; n <= NMAX; n <<= 1) {
            int[] ipS = ip(n), ipV = ip(n);
            double[] wS = new double[n / 2], wV = new double[n / 2];
            for (int isgn : new int[] {1, -1}) {
                double[] a = random(n, n * 31L + isgn);
                double[] s = a.clone();
                double[] v = a.clone();
                scalar.rdft(n, isgn, s, ipS, wS);
                vector.rdft(n, isgn, v, ipV, wV);
                for (int i = 0; i < n; i++) {
                    // delta 0: exact, but +0 == -0
                    assertEquals(s[i], v[i], 0.0, "n=" + n + ", isgn=" + isgn + ", i=" + i);
                }
            }
        }
    }

    @Test
    @DisplayName("vector radix 4 is the same as scalar within rounding errors")
    void testRadix4() {
        Fft scalar = new ScalarFft();
        Fft radix4 = new VectorRadix4Fft();
        for (int n = 4; n <= NMAX; n <<= 1) {
            int[] ipS = ip(n), ipV = ip(n);
            double[] wS = new double[n / 2], wV = new double[n / 2];
            for (int isgn : new int[] {1, -1}) {
                double[] a = random(n, n * 31L + isgn);
                double[] s = a.clone();
                double[] v = a.clone();
                scalar.rdft(n, isgn, s, ipS, wS);
                radix4.rdft(n, isgn, v, ipV, wV);
                double max = 0, err = 0;
                for (int i = 0; i < n; i++) {
                    max = Math.max(max, Math.abs(s[i]));
                    err = Math.max(err, Math.abs(s[i] - v[i]));
                }
                assertTrue(err / max < 1e-14, "n=" + n + ", isgn=" + isgn + ", err=" + err / max);
            }
        }
    }

    @Test
    @DisplayName("round trip")
    void testRoundTrip() {
      for (Fft vector : new Fft[] {new VectorFft(), new VectorRadix4Fft()}) {
        for (int n = 4; n <= NMAX; n <<= 1) {
            int[] ip = ip(n);
            double[] w = new double[n / 2];
            double[] a = random(n, n);
            double[] b = a.clone();
            vector.rdft(n, 1, b, ip, w);
            vector.rdft(n, -1, b, ip, w);
            double err = 0;
            for (int i = 0; i < n; i++) {
                err = Math.max(err, Math.abs(a[i] - b[i] * 2.0 / n));
            }
            assertTrue(err < 1e-13, vector.getClass().getSimpleName() + " n=" + n + ", err=" + err);
        }
      }
    }

    /** median of nanos per forward + inverse */
    static double bench(Fft fft, int n, int runs, int reps) {
        int[] ip = ip(n);
        double[] w = new double[n / 2];
        double[] src = random(n, n);
        double[] a = new double[n];
        long[] times = new long[runs];
        for (int r = 0; r < runs; r++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < reps; i++) {
                System.arraycopy(src, 0, a, 0, n);
                fft.rdft(n, 1, a, ip, w);
                fft.rdft(n, -1, a, ip, w);
            }
            times[r] = System.nanoTime() - t0;
        }
        Arrays.sort(times);
        return times[runs / 2] / (double) reps;
    }

    @Test
    @DisplayName("benchmark rdft")
    @EnabledIfSystemProperty(named = "vavi.test.bench", matches = "true")
    void benchRdft() {
        Fft scalar = new ScalarFft();
        Fft vector = new VectorFft();
        Fft radix4 = new VectorRadix4Fft();
        for (int n = 1 << 10; n <= NMAX; n <<= 1) {
            int reps = Math.max(1, (1 << 22) / n);
            // warm up
            bench(scalar, n, 5, reps);
            bench(vector, n, 5, reps);
            bench(radix4, n, 5, reps);
            double s = bench(scalar, n, 15, reps);
            double v = bench(vector, n, 15, reps);
            double r = bench(radix4, n, 15, reps);
            System.out.printf("rdft n=%7d scalar %8.1f us, vector %8.1f us x%.2f, vectorradix4 %8.1f us x%.2f%n",
                    n, s / 1000, v / 1000, s / v, r / 1000, s / r);
        }
    }

    @Test
    @DisplayName("benchmark ssrc")
    @EnabledIfSystemProperty(named = "vavi.test.bench", matches = "true")
    void benchSsrc() throws Exception {
        String in = "src/test/resources/vavi/sound/pcm/resampling/ssrc/44100.wav";
        Path out = Paths.get("tmp/fft.bench.wav");
        Files.createDirectories(out.getParent());
        String[] keys = {Fft.PROPERTY, Convolver.PROPERTY, FIR};
        String[] saved = new String[keys.length];
        for (int i = 0; i < keys.length; i++) saved[i] = System.getProperty(keys[i]);
        try {
            for (String rate : new String[] {"48000", "8000"}) {
                String[] args = {"--quiet", "--rate", rate, "--twopass", "--normalize", in, out.toString()};
                String[] names = {"scalar", "vector", "vectorradix4", "all vector"};
                long[] sums = new long[names.length];
                long[][] times = new long[names.length][5];
                for (int r = 0; r < 6; r++) { // the 1st round is a warm up
                    for (int i = 0; i < names.length; i++) {
                        boolean all = names[i].equals("all vector");
                        System.setProperty(Fft.PROPERTY, all ? "vectorradix4" : names[i]);
                        System.setProperty(Convolver.PROPERTY, all ? "vector" : "scalar");
                        System.setProperty(FIR, all ? "vector" : "scalar");
                        long t0 = System.nanoTime();
                        SSRC.main(args);
                        long t = System.nanoTime() - t0;
                        if (r > 0) times[i][r - 1] = t;
                        sums[i] = Checksum.getChecksum(out);
                    }
                }
                for (int i = 1; i < names.length; i++) {
                    assertEquals(sums[0], sums[i], "output of scalar and " + names[i] + " differ");
                    Arrays.sort(times[i]);
                }
                Arrays.sort(times[0]);
                double s = times[0][2] / 1e6, v = times[1][2] / 1e6, r4 = times[2][2] / 1e6, a = times[3][2] / 1e6;
                System.out.printf("ssrc 44100->%s scalar %8.1f ms, vector %8.1f ms x%.2f, vectorradix4 %8.1f ms x%.2f, all vector %8.1f ms x%.2f%n",
                        rate, s, v, s / v, r4, s / r4, a, s / a);
            }
        } finally {
            for (int i = 0; i < keys.length; i++) {
                if (saved[i] == null) System.clearProperty(keys[i]); else System.setProperty(keys[i], saved[i]);
            }
        }
    }
}
