/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.ssrc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.util.SplitRadixFft.cdft;
import static vavi.util.SplitRadixFft.ddct;
import static vavi.util.SplitRadixFft.ddst;
import static vavi.util.SplitRadixFft.dfct;
import static vavi.util.SplitRadixFft.dfst;
import static vavi.util.SplitRadixFft.rdft;


/**
 * test of fftsg, port of testxg.c in Ooura's fft package.
 * <p>
 * the original prints the round trip errors, they are about 1e-16 with n = 8192.
 * so the errors are checked with {@link #EPSILON} for every data length.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060127 nsano initial version <br>
 */
public class fftsgTest {

    /* random number generator, 0 <= RND < 1 */
    static double RND(int[] p) {
        return (p[0] = (p[0] * 7141 + 54773) % 259200) * (1.0 / 259200.0);
    }

    /** */
    static final int NMAX = 8192;

    /** */
    static final int NMAXSQRT = 64;

    /** allowed round trip error, the original is less than 1e-15 */
    static final double EPSILON = 1e-14;

    /** a round trip of a transform with data length n */
    interface Check {
        /** @return error */
        double apply(int n, double[] a, double[] t, int[] ip, double[] w);
    }

    /** runs the check for n = 4 ~ {@link #NMAX}, ip and w are initialized once like the original */
    static void check(String name, Check check) {
        for (int n = 4; n <= NMAX; n <<= 1) {
            int[] ip = new int[NMAXSQRT + 2];
            double[] a = new double[NMAX + 1];
            double[] w = new double[NMAX * 5 / 4];
            double[] t = new double[NMAX / 2 + 1];
            ip[0] = 0;
            double err = check.apply(n, a, t, ip, w);
            System.out.printf("n=%d %s err= %g \n", n, name, err);
            assertTrue(err < EPSILON, "n=" + n + ", " + name + " err=" + err);
        }
    }

    @Test
    @DisplayName("check of CDFT")
    public void testCDFT() throws Exception {
        check("cdft", (n, a, t, ip, w) -> {
            putData(0, n - 1, a);
            cdft(n, 1, a, ip, w);
            cdft(n, -1, a, ip, w);
            return checkError(0, n - 1, 2.0 / n, a);
        });
    }

    @Test
    @DisplayName("check of RDFT")
    public void testRDFT() throws Exception {
        check("rdft", (n, a, t, ip, w) -> {
            putData(0, n - 1, a);
            rdft(n, 1, a, ip, w);
            rdft(n, -1, a, ip, w);
            return checkError(0, n - 1, 2.0 / n, a);
        });
    }

    @Test
    @DisplayName("check of DDCT")
    public void testDDCT() throws Exception {
        check("ddct", (n, a, t, ip, w) -> {
            putData(0, n - 1, a);
            ddct(n, 1, a, ip, w);
            ddct(n, -1, a, ip, w);
            a[0] *= 0.5;
            return checkError(0, n - 1, 2.0 / n, a);
        });
    }

    @Test
    @DisplayName("check of DDST")
    public void testDDST() throws Exception {
        check("ddst", (n, a, t, ip, w) -> {
            putData(0, n - 1, a);
            ddst(n, 1, a, ip, w);
            ddst(n, -1, a, ip, w);
            a[0] *= 0.5;
            return checkError(0, n - 1, 2.0 / n, a);
        });
    }

    @Test
    @DisplayName("check of DFCT")
    public void testDFCT() throws Exception {
        check("dfct", (n, a, t, ip, w) -> {
            putData(0, n, a);
            a[0] *= 0.5;
            a[n] *= 0.5;
            dfct(n, a, t, ip, w);
            a[0] *= 0.5;
            a[n] *= 0.5;
            dfct(n, a, t, ip, w);
            return checkError(0, n, 2.0 / n, a);
        });
    }

    @Test
    @DisplayName("check of DFST")
    public void testDFST() throws Exception {
        check("dfst", (n, a, t, ip, w) -> {
            putData(1, n - 1, a);
            dfst(n, a, t, ip, w);
            dfst(n, a, t, ip, w);
            return checkError(1, n - 1, 2.0 / n, a);
        });
    }

    /**
     * with n = 4 (2 complex samples) the kernel exp(±2πijk/2) is real,
     * so both directions are X[0] = x[0] + x[1], X[1] = x[0] - x[1] without conjugation.
     */
    @Test
    @DisplayName("CDFT with n = 4 is not conjugated")
    public void testCDFT4() throws Exception {
        for (int isgn : new int[] {1, -1}) {
            double[] a = {1, 2, 3, 5};
            cdft(4, isgn, a, new int[4], new double[4]);
            assertEquals(1 + 3, a[0], 0, "isgn " + isgn);
            assertEquals(2 + 5, a[1], 0, "isgn " + isgn);
            assertEquals(1 - 3, a[2], 0, "isgn " + isgn);
            assertEquals(2 - 5, a[3], 0, "isgn " + isgn);
        }
    }

    /** */
    private static void putData(int nini, int nend, double[] a) {
        int[] seed = new int[] { 0 };

        for (int j = nini; j <= nend; j++) {
            a[j] = RND(seed);
        }
    }

    /** */
    private static double checkError(int nini, int nend, double scale, double[] a) {
        int[] seed = new int[] { 0 };
        double err = 0, e;

        for (int j = nini; j <= nend; j++) {
            e = RND(seed) - a[j] * scale;
            err = Math.max(err, Math.abs(e));
        }
        return err;
    }
}
