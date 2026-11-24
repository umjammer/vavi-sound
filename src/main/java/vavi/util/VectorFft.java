/*
 * Copyright Takuya OOURA, 1996-2001
 *
 * You may use, copy, modify and distribute this code
 * for any purpose (include commercial use) and without fee.
 * Please refer to this package when you modify this code.
 */

package vavi.util;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorShuffle;
import jdk.incubator.vector.VectorSpecies;


/**
 * {@link SplitRadixFft#rdft(int, int, double[], int[], double[])} with the Vector API.
 * <p>
 * the butterflies are vectorized over the interleaved (re, im) data,
 * the operations are the same as the scalar one in the same order,
 * so the result is the same as {@link ScalarFft} (except the sign of zeros).
 * the bit reversal and the tables are shared with {@link SplitRadixFft}.
 * </p>
 * <p>
 * needs {@code --add-modules jdk.incubator.vector}.
 * </p>
 *
 * @author <a href="mailto:ooura@mmm.t.u-tokyo.ac.jp">Takuya OOURA</a>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-08 nsano initial version <br>
 */
public class VectorFft implements Fft {

    /**
     * the species for the strided butterflies, at most 8 lanes,
     * so that every stage length (l &gt;= 8 when n &gt;= {@link #MIN_N}) is a multiple of it
     */
    private static final VectorSpecies<Double> S =
            DoubleVector.SPECIES_PREFERRED.length() > 8 ? DoubleVector.SPECIES_512 : DoubleVector.SPECIES_PREFERRED;

    /** lanes of {@link #S} */
    private static final int SL = S.length();

    /** (re, im) to (im, re) */
    private static final VectorShuffle<Double> SWAP = VectorShuffle.fromOp(S, i -> i ^ 1);

    /** (-1, 1, ...) */
    private static final DoubleVector NP = signs(S, -1, 1);

    /** (1, -1, ...) */
    private static final DoubleVector PN = signs(S, 1, -1);

    /** the species for one complex */
    private static final VectorSpecies<Double> C = DoubleVector.SPECIES_128;

    /** (re, im) to (im, re) for {@link #C} */
    private static final VectorShuffle<Double> CSWAP = VectorShuffle.fromOp(C, i -> i ^ 1);

    /** (-1, 1) */
    private static final DoubleVector CNP = signs(C, -1, 1);

    /** (1, -1) */
    private static final DoubleVector CPN = signs(C, 1, -1);

    /** smaller data are done by {@link SplitRadixFft} */
    private static final int MIN_N = 32;

    /** */
    private static DoubleVector signs(VectorSpecies<Double> species, double even, double odd) {
        double[] s = new double[species.length()];
        for (int i = 0; i < s.length; i++) {
            s[i] = (i & 1) == 0 ? even : odd;
        }
        return DoubleVector.fromArray(species, s, 0);
    }

    @Override
    public void rdft(int n, int isgn, double[] a, int[] ip, double[] w) {
        if (n < MIN_N) {
            SplitRadixFft.rdft(n, isgn, a, ip, w);
            return;
        }

        int nw = ip[0];
        if (n > (nw << 2)) {
            nw = n >> 2;
            SplitRadixFft.makewt(nw, ip, w);
        }
        int nc = ip[1];
        if (n > (nc << 2)) {
            nc = n >> 2;
            SplitRadixFft.makect(nc, ip, w, nw);
        }
        if (isgn >= 0) {
            SplitRadixFft.bitrv2(n, ip, 2, a);
            cftfsub(n, a, w);
            rftfsub(n, a, nc, w, nw);
            double xi = a[0] - a[1];
            a[0] += a[1];
            a[1] = xi;
        } else {
            a[1] = 0.5 * (a[0] - a[1]);
            a[0] -= a[1];
            rftbsub(n, a, nc, w, nw);
            SplitRadixFft.bitrv2(n, ip, 2, a);
            cftbsub(n, a, w);
        }
    }

    /** radix 4 butterflies without twiddles for a[j...j+l-1] */
    private static void bfly4(double[] a, int start, int l) {
        for (int j = start; j < start + l; j += SL) {
            int j1 = j + l;
            int j2 = j1 + l;
            int j3 = j2 + l;
            DoubleVector a0 = DoubleVector.fromArray(S, a, j);
            DoubleVector a1 = DoubleVector.fromArray(S, a, j1);
            DoubleVector a2 = DoubleVector.fromArray(S, a, j2);
            DoubleVector a3 = DoubleVector.fromArray(S, a, j3);
            DoubleVector x0 = a0.add(a1);
            DoubleVector x1 = a0.sub(a1);
            DoubleVector x2 = a2.add(a3);
            DoubleVector x3 = a2.sub(a3);
            DoubleVector s3 = x3.rearrange(SWAP);
            x0.add(x2).intoArray(a, j);
            x0.sub(x2).intoArray(a, j2);
            x1.add(s3.mul(NP)).intoArray(a, j1); // (x1r - x3i, x1i + x3r)
            x1.add(s3.mul(PN)).intoArray(a, j3); // (x1r + x3i, x1i - x3r)
        }
    }

    /** radix 4 butterflies with twiddles for a[j...j+l-1] */
    private static void bfly4(double[] a, int start, int l,
                              double wk1r, double wk1i, double wk2r, double wk2i, double wk3r, double wk3i) {
        DoubleVector w1r = DoubleVector.broadcast(S, wk1r);
        DoubleVector w1i = NP.mul(wk1i);
        DoubleVector w2r = DoubleVector.broadcast(S, wk2r);
        DoubleVector w2i = NP.mul(wk2i);
        DoubleVector w3r = DoubleVector.broadcast(S, wk3r);
        DoubleVector w3i = NP.mul(wk3i);
        for (int j = start; j < start + l; j += SL) {
            int j1 = j + l;
            int j2 = j1 + l;
            int j3 = j2 + l;
            DoubleVector a0 = DoubleVector.fromArray(S, a, j);
            DoubleVector a1 = DoubleVector.fromArray(S, a, j1);
            DoubleVector a2 = DoubleVector.fromArray(S, a, j2);
            DoubleVector a3 = DoubleVector.fromArray(S, a, j3);
            DoubleVector x0 = a0.add(a1);
            DoubleVector x1 = a0.sub(a1);
            DoubleVector x2 = a2.add(a3);
            DoubleVector x3 = a2.sub(a3);
            DoubleVector s3 = x3.rearrange(SWAP);
            x0.add(x2).intoArray(a, j);
            // x * (wr + i wi) = x * wr + swap(x) * (-wi, wi), expanded by hand, a vector over a call is boxed
            DoubleVector y2 = x0.sub(x2);
            y2.mul(w2r).add(y2.rearrange(SWAP).mul(w2i)).intoArray(a, j2);
            DoubleVector y1 = x1.add(s3.mul(NP));
            y1.mul(w1r).add(y1.rearrange(SWAP).mul(w1i)).intoArray(a, j1);
            DoubleVector y3 = x1.add(s3.mul(PN));
            y3.mul(w3r).add(y3.rearrange(SWAP).mul(w3i)).intoArray(a, j3);
        }
    }

    /** radix 4 butterfly with twiddles for 4 complexes at a[j], a[j+2], a[j+4], a[j+6] */
    private static void cbfly4(double[] a, int j,
                               double wk1r, double wk1i, double wk2r, double wk2i, double wk3r, double wk3i) {
        DoubleVector a0 = DoubleVector.fromArray(C, a, j);
        DoubleVector a1 = DoubleVector.fromArray(C, a, j + 2);
        DoubleVector a2 = DoubleVector.fromArray(C, a, j + 4);
        DoubleVector a3 = DoubleVector.fromArray(C, a, j + 6);
        DoubleVector x0 = a0.add(a1);
        DoubleVector x1 = a0.sub(a1);
        DoubleVector x2 = a2.add(a3);
        DoubleVector x3 = a2.sub(a3);
        DoubleVector s3 = x3.rearrange(CSWAP);
        x0.add(x2).intoArray(a, j);
        DoubleVector y2 = x0.sub(x2);
        y2.mul(wk2r).add(y2.rearrange(CSWAP).mul(CNP.mul(wk2i))).intoArray(a, j + 4);
        DoubleVector y1 = x1.add(s3.mul(CNP));
        y1.mul(wk1r).add(y1.rearrange(CSWAP).mul(CNP.mul(wk1i))).intoArray(a, j + 2);
        DoubleVector y3 = x1.add(s3.mul(CPN));
        y3.mul(wk3r).add(y3.rearrange(CSWAP).mul(CNP.mul(wk3i))).intoArray(a, j + 6);
    }

    /** @see SplitRadixFft cftfsub */
    private static void cftfsub(int n, double[] a, double[] w) {
        cft1st(n, a, w);
        int l = 8;
        while ((l << 2) < n) {
            cftmdl(n, l, a, w);
            l <<= 2;
        }
        if ((l << 2) == n) {
            bfly4(a, 0, l);
        } else {
            for (int j = 0; j < l; j += SL) {
                DoubleVector a0 = DoubleVector.fromArray(S, a, j);
                DoubleVector a1 = DoubleVector.fromArray(S, a, j + l);
                a0.sub(a1).intoArray(a, j + l);
                a0.add(a1).intoArray(a, j);
            }
        }
    }

    /** @see SplitRadixFft cftbsub */
    private static void cftbsub(int n, double[] a, double[] w) {
        cft1st(n, a, w);
        int l = 8;
        while ((l << 2) < n) {
            cftmdl(n, l, a, w);
            l <<= 2;
        }
        if ((l << 2) == n) {
            for (int j = 0; j < l; j += SL) {
                int j1 = j + l;
                int j2 = j1 + l;
                int j3 = j2 + l;
                DoubleVector a0 = DoubleVector.fromArray(S, a, j);
                DoubleVector a1 = DoubleVector.fromArray(S, a, j1);
                DoubleVector a2 = DoubleVector.fromArray(S, a, j2);
                DoubleVector a3 = DoubleVector.fromArray(S, a, j3);
                DoubleVector x0 = a0.add(a1).mul(PN); // (a0r + a1r, -a0i - a1i)
                DoubleVector x1 = a0.sub(a1).mul(PN); // (a0r - a1r, -a0i + a1i)
                DoubleVector x2 = a2.add(a3);
                DoubleVector x3 = a2.sub(a3);
                DoubleVector s3 = x3.rearrange(SWAP);
                x0.add(x2.mul(PN)).intoArray(a, j); // (x0r + x2r, x0i - x2i)
                x0.add(x2.mul(NP)).intoArray(a, j2); // (x0r - x2r, x0i + x2i)
                x1.sub(s3).intoArray(a, j1); // (x1r - x3i, x1i - x3r)
                x1.add(s3).intoArray(a, j3); // (x1r + x3i, x1i + x3r)
            }
        } else {
            for (int j = 0; j < l; j += SL) {
                DoubleVector a0 = DoubleVector.fromArray(S, a, j);
                DoubleVector a1 = DoubleVector.fromArray(S, a, j + l);
                a0.sub(a1).mul(PN).intoArray(a, j + l);
                a0.add(a1).mul(PN).intoArray(a, j);
            }
        }
    }

    /** @see SplitRadixFft cft1st */
    private static void cft1st(int n, double[] a, double[] w) {
        double wk1r, wk1i, wk2r, wk2i, wk3r, wk3i;
        double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;

        x0r = a[0] + a[2];
        x0i = a[1] + a[3];
        x1r = a[0] - a[2];
        x1i = a[1] - a[3];
        x2r = a[4] + a[6];
        x2i = a[5] + a[7];
        x3r = a[4] - a[6];
        x3i = a[5] - a[7];
        a[0] = x0r + x2r;
        a[1] = x0i + x2i;
        a[4] = x0r - x2r;
        a[5] = x0i - x2i;
        a[2] = x1r - x3i;
        a[3] = x1i + x3r;
        a[6] = x1r + x3i;
        a[7] = x1i - x3r;
        wk1r = w[2];
        x0r = a[8] + a[10];
        x0i = a[9] + a[11];
        x1r = a[8] - a[10];
        x1i = a[9] - a[11];
        x2r = a[12] + a[14];
        x2i = a[13] + a[15];
        x3r = a[12] - a[14];
        x3i = a[13] - a[15];
        a[8] = x0r + x2r;
        a[9] = x0i + x2i;
        a[12] = x2i - x0i;
        a[13] = x0r - x2r;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        a[10] = wk1r * (x0r - x0i);
        a[11] = wk1r * (x0r + x0i);
        x0r = x3i + x1r;
        x0i = x3r - x1i;
        a[14] = wk1r * (x0i - x0r);
        a[15] = wk1r * (x0i + x0r);
        int k1 = 0;
        for (int j = 16; j < n; j += 16) {
            k1 += 2;
            int k2 = 2 * k1;
            wk2r = w[k1];
            wk2i = w[k1 + 1];
            wk1r = w[k2];
            wk1i = w[k2 + 1];
            wk3r = wk1r - 2 * wk2i * wk1i;
            wk3i = 2 * wk2i * wk1r - wk1i;
            cbfly4(a, j, wk1r, wk1i, wk2r, wk2i, wk3r, wk3i);
            wk1r = w[k2 + 2];
            wk1i = w[k2 + 3];
            wk3r = wk1r - 2 * wk2r * wk1i;
            wk3i = 2 * wk2r * wk1r - wk1i;
            cbfly4(a, j + 8, wk1r, wk1i, -wk2i, wk2r, wk3r, wk3i);
        }
    }

    /** @see SplitRadixFft cftmdl */
    private static void cftmdl(int n, int l, double[] a, double[] w) {
        int m = l << 2;
        bfly4(a, 0, l);
        DoubleVector w1 = DoubleVector.broadcast(S, w[2]);
        for (int j = m; j < l + m; j += SL) {
            int j1 = j + l;
            int j2 = j1 + l;
            int j3 = j2 + l;
            DoubleVector a0 = DoubleVector.fromArray(S, a, j);
            DoubleVector a1 = DoubleVector.fromArray(S, a, j1);
            DoubleVector a2 = DoubleVector.fromArray(S, a, j2);
            DoubleVector a3 = DoubleVector.fromArray(S, a, j3);
            DoubleVector x0 = a0.add(a1);
            DoubleVector x1 = a0.sub(a1);
            DoubleVector x2 = a2.add(a3);
            DoubleVector x3 = a2.sub(a3);
            DoubleVector s3 = x3.rearrange(SWAP);
            x0.add(x2).intoArray(a, j);
            x0.sub(x2).rearrange(SWAP).mul(NP).intoArray(a, j2); // (x2i - x0i, x0r - x2r)
            DoubleVector u = x1.add(s3.mul(NP)); // (x1r - x3i, x1i + x3r)
            u.add(u.rearrange(SWAP).mul(NP)).mul(w1).intoArray(a, j1); // wk1r * (ur - ui, ur + ui)
            DoubleVector v = s3.add(x1.mul(PN)); // (x3i + x1r, x3r - x1i)
            v.rearrange(SWAP).add(v.mul(NP)).mul(w1).intoArray(a, j3); // wk1r * (vi - vr, vi + vr)
        }
        int k1 = 0;
        int m2 = 2 * m;
        for (int k = m2; k < n; k += m2) {
            k1 += 2;
            int k2 = 2 * k1;
            double wk2r = w[k1];
            double wk2i = w[k1 + 1];
            double wk1r = w[k2];
            double wk1i = w[k2 + 1];
            double wk3r = wk1r - 2 * wk2i * wk1i;
            double wk3i = 2 * wk2i * wk1r - wk1i;
            bfly4(a, k, l, wk1r, wk1i, wk2r, wk2i, wk3r, wk3i);
            wk1r = w[k2 + 2];
            wk1i = w[k2 + 3];
            wk3r = wk1r - 2 * wk2r * wk1i;
            wk3i = 2 * wk2r * wk1r - wk1i;
            bfly4(a, k + m, l, wk1r, wk1i, -wk2i, wk2r, wk3r, wk3i);
        }
    }

    /** @see SplitRadixFft rftfsub */
    private static void rftfsub(int n, double[] a, int nc, double[] c, int cP) {
        int m = n >> 1;
        int ks = 2 * nc / m;
        int kk = 0;
        for (int j = 2; j < m; j += 2) {
            int k = n - j;
            kk += ks;
            double wkr = 0.5 - c[cP + nc - kk];
            double wki = c[cP + kk];
            DoubleVector aj = DoubleVector.fromArray(C, a, j);
            DoubleVector ak = DoubleVector.fromArray(C, a, k);
            DoubleVector x = aj.add(ak.mul(CNP)); // (ajr - akr, aji + aki)
            DoubleVector y = x.mul(wkr).add(x.rearrange(CSWAP).mul(CNP.mul(wki)));
            aj.sub(y).intoArray(a, j);
            ak.add(y.mul(CPN)).intoArray(a, k); // (akr + yr, aki - yi)
        }
    }

    /** @see SplitRadixFft rftbsub */
    private static void rftbsub(int n, double[] a, int nc, double[] c, int cP) {
        a[1] = -a[1];
        int m = n >> 1;
        int ks = 2 * nc / m;
        int kk = 0;
        for (int j = 2; j < m; j += 2) {
            int k = n - j;
            kk += ks;
            double wkr = 0.5 - c[cP + nc - kk];
            double wki = c[cP + kk];
            DoubleVector aj = DoubleVector.fromArray(C, a, j);
            DoubleVector ak = DoubleVector.fromArray(C, a, k);
            DoubleVector x = aj.add(ak.mul(CNP)); // (ajr - akr, aji + aki)
            DoubleVector y = x.mul(wkr).add(x.rearrange(CSWAP).mul(CPN.mul(wki))); // (wkr xr + wki xi, wkr xi - wki xr)
            aj.mul(CPN).add(y.mul(CNP)).intoArray(a, j); // (ajr - yr, yi - aji)
            ak.mul(CPN).add(y).intoArray(a, k); // (akr + yr, yi - aki)
        }
        a[m + 1] = -a[m + 1];
    }
}
