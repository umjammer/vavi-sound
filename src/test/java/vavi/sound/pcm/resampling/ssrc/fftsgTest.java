/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.ssrc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import vavi.util.SplitRadixFft;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * test of fftsg
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060127 nsano initial version <br>
 */
public class fftsgTest {

    /* random number generator, 0 <= RND < 1 */
    double RND(int[] p) {
        return (p[0] = (p[0] * 7141 + 54773) % 259200) * (1.0 / 259200.0);
    }

    /** */
    static final int NMAX = 8192;

    /** */
    static final int NMAXSQRT = 64;

    /** */
    int[] ip;
    /** */
    double[] a, w, t;

    /** */
    SplitRadixFft fftsg = new SplitRadixFft();

    /** */
    @BeforeAll
    public void setUp() throws Exception {
        ip = new int[NMAXSQRT + 2];
        a = new double[NMAX + 1];
        w = new double[NMAX * 5 / 4];
        t = new double[NMAX / 2 + 1];
        ip[0] = 0;
    }

    /** check of CDFT */
    @Test
    public void testCDFT() throws Exception {
        putdata(0, NMAX - 1, a);
        fftsg.cdft(NMAX, 1, a, ip, w);
        fftsg.cdft(NMAX, -1, a, ip, w);
        double err = errorcheck(0, NMAX - 1, 2.0 / NMAX, a);
        System.out.printf("cdft err= %g \n", err);
        assertEquals(0, err, 0);
    }

    /** check of RDFT */
    @Test
    public void testRDFT() throws Exception {
        putdata(0, NMAX - 1, a);
        fftsg.rdft(NMAX, 1, a, ip, w);
        fftsg.rdft(NMAX, -1, a, ip, w);
        double err = errorcheck(0, NMAX - 1, 2.0 / NMAX, a);
        System.out.printf("rdft err= %g \n", err);
        assertEquals(0, err, 0);
    }

    /** check of DDCT */
    @Test
    public void testDDCT() throws Exception {
        putdata(0, NMAX - 1, a);
        fftsg.ddct(NMAX, 1, a, ip, w);
        fftsg.ddct(NMAX, -1, a, ip, w);
        a[0] *= 0.5;
        double err = errorcheck(0, NMAX - 1, 2.0 / NMAX, a);
        System.out.printf("ddct err= %g \n", err);
        assertEquals(0, err, 0);
    }

    /** check of DDST */
    @Test
    public void testDDST() throws Exception {
        putdata(0, NMAX - 1, a);
        fftsg.ddst(NMAX, 1, a, ip, w);
        fftsg.ddst(NMAX, -1, a, ip, w);
        a[0] *= 0.5;
        double err = errorcheck(0, NMAX - 1, 2.0 / NMAX, a);
        System.out.printf("ddst err= %g \n", err);
        assertEquals(0, err, 0);
    }

    /** check of DFCT */
    @Test
    public void testDFCT() throws Exception {
        putdata(0, NMAX, a);
        a[0] *= 0.5;
        a[NMAX] *= 0.5;
        fftsg.dfct(NMAX, a, t, ip, w);
        a[0] *= 0.5;
        a[NMAX] *= 0.5;
        fftsg.dfct(NMAX, a, t, ip, w);
        double err = errorcheck(0, NMAX, 2.0 / NMAX, a);
        System.out.printf("dfct err= %g \n", err);
        assertEquals(0, err, 0);
    }

    /** check of DFST */
    @Test
    public void testDFST() throws Exception {
        putdata(1, NMAX - 1, a);
        fftsg.dfst(NMAX, a, t, ip, w);
        fftsg.dfst(NMAX, a, t, ip, w);
        double err = errorcheck(1, NMAX - 1, 2.0 / NMAX, a);
        System.out.printf("dfst err= %g \n", err);
        assertEquals(0, err, 0);
    }

    /** */
    private void putdata(int nini, int nend, double[] a) {
        int[] seed = new int[] { 0 };

        for (int j = nini; j <= nend; j++) {
            a[j] = RND(seed);
        }
    }

    /** */
    private double errorcheck(int nini, int nend, double scale, double[] a) {
        int[] seed = new int[] { 0 };
        double err = 0, e;

        for (int j = nini; j <= nend; j++) {
            e = RND(seed) - a[j] * scale;
            err = Math.max(err, Math.abs(e));
        }
        return err;
    }
}

/* */
