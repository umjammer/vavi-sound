/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorShuffle;
import jdk.incubator.vector.VectorSpecies;


/**
 * {@link SplitRadixFft#rdft(int, int, double[], int[], double[])} compatible real fft
 * by a split format (separated re[] and im[]) radix 4 decimation in frequency complex fft with the Vector API.
 * <p>
 * n real data are packed into n/2 complex data, the n/2 points complex fft is done
 * on separated real and imaginary arrays, so the butterflies are pure lane wise operations
 * (no shuffles), then the spectrum is unpacked into the Ooura's format.
 * the interleaved input is deinterleaved in the 1st stage.
 * the result is not bit exact to {@link ScalarFft}, the difference is a rounding error.
 * </p>
 * <p>
 * {@code ip} and {@code w} are not used except n &lt; {@link #MIN_N} (done by {@link SplitRadixFft}),
 * the tables are cached in this class, the work areas are per thread.
 * needs {@code --add-modules jdk.incubator.vector}.
 * </p>
 * <p>
 * a radix 4 Stockham autosort version (no bit reversal, all contiguous) was tried also,
 * but it was slower on 2 lanes (aarch64), the small stride stages cost more than the gathers.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-08 nsano initial version <br>
 */
public class VectorRadix4Fft implements Fft {

    /** */
    private static final VectorSpecies<Double> S = DoubleVector.SPECIES_PREFERRED;

    /** */
    private static final int SL = S.length();

    /** even lanes of (v0, v1), for deinterleaving */
    private static final VectorShuffle<Double> EVEN = VectorShuffle.fromOp(S, i -> 2 * i < SL ? 2 * i : 2 * i - 2 * SL);

    /** odd lanes of (v0, v1), for deinterleaving */
    private static final VectorShuffle<Double> ODD = VectorShuffle.fromOp(S, i -> 2 * i + 1 < SL ? 2 * i + 1 : 2 * i + 1 - 2 * SL);

    /** smaller data are done by {@link SplitRadixFft}, the 1st stage needs n / 8 &gt;= lanes */
    private static final int MIN_N = Math.max(64, 8 * SL);

    /** tables for a data length */
    private static final class Plan {
        /** complex points (n / 2) */
        final int nh;
        /** bit reversal of nh */
        final int[] rev;
        /** log2(nh) is odd, a radix 2 stage at first */
        final boolean radix2;
        /** radix 2 twiddles [cos(h), sin(h)] for forward, backward */
        final double[] r2f, r2b;
        /** radix 4 twiddles per stage [c1(q), s1(q), c2(q), s2(q), c3(q), s3(q)] for forward, backward */
        final double[][] r4f, r4b;
        /** cos, sin (2 pi k / n), k = 0 ... nh / 2 for packing */
        final double[] pc, ps;

        Plan(int n) {
            nh = n >> 1;
            int bits = Integer.numberOfTrailingZeros(nh);
            rev = new int[nh];
            for (int i = 0; i < nh; i++) {
                rev[i] = Integer.reverse(i) >>> (32 - bits);
            }
            radix2 = (bits & 1) != 0;
            if (radix2) {
                int h = nh >> 1;
                r2f = new double[2 * h];
                r2b = new double[2 * h];
                for (int j = 0; j < h; j++) {
                    double t = 2 * Math.PI * j / (2 * h);
                    r2f[j] = r2b[j] = Math.cos(t);
                    r2f[h + j] = Math.sin(t);
                    r2b[h + j] = -Math.sin(t);
                }
            } else {
                r2f = r2b = null;
            }
            int stages = bits / 2;
            r4f = new double[stages][];
            r4b = new double[stages][];
            int q = radix2 ? nh >> 3 : nh >> 2;
            for (int s = 0; s < stages; s++, q >>= 2) {
                double[] f = new double[6 * q];
                double[] b = new double[6 * q];
                for (int r = 1; r <= 3; r++) {
                    for (int j = 0; j < q; j++) {
                        double t = 2 * Math.PI * j * r / (4 * q);
                        f[(2 * r - 2) * q + j] = b[(2 * r - 2) * q + j] = Math.cos(t);
                        f[(2 * r - 1) * q + j] = Math.sin(t);
                        b[(2 * r - 1) * q + j] = -Math.sin(t);
                    }
                }
                r4f[s] = f;
                r4b[s] = b;
            }
            pc = new double[nh / 2 + 1];
            ps = new double[nh / 2 + 1];
            for (int k = 0; k <= nh / 2; k++) {
                double t = 2 * Math.PI * k / n;
                pc[k] = Math.cos(t);
                ps[k] = Math.sin(t);
            }
            pc[nh / 2] = 0;
            ps[nh / 2] = 1;
        }
    }

    /** n to plan */
    private static final Map<Integer, Plan> plans = new ConcurrentHashMap<>();

    /** work area per thread, [re, im] */
    private static final ThreadLocal<double[][]> work = ThreadLocal.withInitial(() -> new double[2][0]);

    @Override
    public void rdft(int n, int isgn, double[] a, int[] ip, double[] w) {
        if (n < MIN_N) {
            SplitRadixFft.rdft(n, isgn, a, ip, w);
            return;
        }

        Plan p = plans.computeIfAbsent(n, Plan::new);
        int nh = p.nh;
        double[][] wk = work.get();
        if (wk[0].length < nh) {
            wk[0] = new double[nh];
            wk[1] = new double[nh];
        }
        double[] re = wk[0];
        double[] im = wk[1];

        if (isgn >= 0) {
            fft(p, a, re, im, true);
            unpack(p, re, im, a);
        } else {
            pack(p, a, re, im);
            fft(p, null, re, im, false);
            int[] rev = p.rev;
            for (int j = 0; j < nh; j++) {
                int r = rev[j];
                a[2 * j] = re[r];
                a[2 * j + 1] = im[r];
            }
        }
    }

    /**
     * the spectrum Z (bit reversed) of the packed data to the Ooura's rdft format.
     * X[k] = (Z[k] + conj Z[N-k]) / 2 - i W^k (Z[k] - conj Z[N-k]) / 2
     */
    private static void unpack(Plan p, double[] re, double[] im, double[] a) {
        int nh = p.nh;
        int[] rev = p.rev;
        double[] pc = p.pc, ps = p.ps;
        a[0] = re[0] + im[0];
        a[1] = re[0] - im[0];
        for (int k = 1; k <= nh / 2; k++) {
            int m = nh - k;
            int ik = rev[k], im_ = rev[m];
            double zr = re[ik], zi = im[ik], mr = re[im_], mi = im[im_];
            double ar = zr + mr, ai = zi - mi, br = zr - mr, bi = zi + mi;
            double c = pc[k], s = ps[k];
            double pr = c * br - s * bi, pi = c * bi + s * br;
            a[2 * m] = 0.5 * (ar - pi);
            a[2 * m + 1] = -0.5 * (ai + pr);
            a[2 * k] = 0.5 * (ar + pi);
            a[2 * k + 1] = 0.5 * (ai - pr);
        }
    }

    /**
     * the Ooura's rdft format to the spectrum Z of the packed data.
     * Z[k] = (X[k] + conj X[N-k]) / 2 + i W^-k (X[k] - conj X[N-k]) / 2
     */
    private static void pack(Plan p, double[] a, double[] re, double[] im) {
        int nh = p.nh;
        double[] pc = p.pc, ps = p.ps;
        re[0] = 0.5 * (a[0] + a[1]);
        im[0] = 0.5 * (a[0] - a[1]);
        for (int k = 1; k <= nh / 2; k++) {
            int m = nh - k;
            double xr = a[2 * k], xi = a[2 * k + 1], mr = a[2 * m], mi = a[2 * m + 1];
            double ar = xr + mr, ai = xi - mi, br = xr - mr, bi = xi + mi;
            double c = pc[k], s = ps[k];
            double pr = c * br + s * bi, pi = c * bi - s * br;
            re[m] = 0.5 * (ar + pi);
            im[m] = 0.5 * (pr - ai);
            re[k] = 0.5 * (ar - pi);
            im[k] = 0.5 * (ai + pr);
        }
    }

    /**
     * complex fft in place, decimation in frequency, natural order in, bit reversed order out.
     * X[k] = sum x[j] exp(sign 2 pi i j k / N), sign is + for forward
     *
     * @param a when not null, the input is interleaved (re, im) in a, it is deinterleaved in the 1st stage
     */
    private static void fft(Plan p, double[] a, double[] re, double[] im, boolean forward) {
        int nh = p.nh;
        double[][] t4 = forward ? p.r4f : p.r4b;
        int q = p.radix2 ? nh >> 3 : nh >> 2;
        int s = 0;
        if (p.radix2) {
            if (a != null) {
                radix2From(a, re, im, nh >> 1, forward ? p.r2f : p.r2b);
            } else {
                radix2(re, im, nh >> 1, forward ? p.r2f : p.r2b);
            }
        } else if (a != null) {
            radix4From(a, re, im, q, t4[0], forward);
            s = 1;
            q >>= 2;
        }
        for (; s < t4.length; s++, q >>= 2) {
            double[] t = t4[s];
            if (q >= SL) {
                radix4(re, im, nh, q, t, forward);
            } else if (q == 1) {
                radix4last(re, im, nh, forward);
            } else {
                for (int b = 0; b < nh; b += 4 * q) {
                    radix4scalar(re, im, b, q, t, forward);
                }
            }
        }
    }

    /** {@link #radix2} reading the interleaved input a (h is a multiple of the lanes) */
    private static void radix2From(double[] a, double[] re, double[] im, int h, double[] t) {
        for (int j = 0; j < h; j += SL) {
            DoubleVector p0 = DoubleVector.fromArray(S, a, 2 * j);
            DoubleVector p1 = DoubleVector.fromArray(S, a, 2 * j + SL);
            DoubleVector ur = p0.rearrange(EVEN, p1), ui = p0.rearrange(ODD, p1);
            p0 = DoubleVector.fromArray(S, a, 2 * (j + h));
            p1 = DoubleVector.fromArray(S, a, 2 * (j + h) + SL);
            DoubleVector vr = p0.rearrange(EVEN, p1), vi = p0.rearrange(ODD, p1);
            DoubleVector c = DoubleVector.fromArray(S, t, j);
            DoubleVector s = DoubleVector.fromArray(S, t, h + j);
            ur.add(vr).intoArray(re, j);
            ui.add(vi).intoArray(im, j);
            DoubleVector dr = ur.sub(vr);
            DoubleVector di = ui.sub(vi);
            dr.mul(c).sub(di.mul(s)).intoArray(re, j + h);
            dr.mul(s).add(di.mul(c)).intoArray(im, j + h);
        }
    }

    /** the 1st {@link #radix4} stage (one block) reading the interleaved input a (q is a multiple of the lanes) */
    private static void radix4From(double[] a, double[] re, double[] im, int q, double[] t, boolean forward) {
        for (int j = 0; j < q; j += SL) {
            int i0 = j, i1 = i0 + q, i2 = i1 + q, i3 = i2 + q;
            DoubleVector p0 = DoubleVector.fromArray(S, a, 2 * i0);
            DoubleVector p1 = DoubleVector.fromArray(S, a, 2 * i0 + SL);
            DoubleVector a0r = p0.rearrange(EVEN, p1), a0i = p0.rearrange(ODD, p1);
            p0 = DoubleVector.fromArray(S, a, 2 * i1);
            p1 = DoubleVector.fromArray(S, a, 2 * i1 + SL);
            DoubleVector a1r = p0.rearrange(EVEN, p1), a1i = p0.rearrange(ODD, p1);
            p0 = DoubleVector.fromArray(S, a, 2 * i2);
            p1 = DoubleVector.fromArray(S, a, 2 * i2 + SL);
            DoubleVector a2r = p0.rearrange(EVEN, p1), a2i = p0.rearrange(ODD, p1);
            p0 = DoubleVector.fromArray(S, a, 2 * i3);
            p1 = DoubleVector.fromArray(S, a, 2 * i3 + SL);
            DoubleVector a3r = p0.rearrange(EVEN, p1), a3i = p0.rearrange(ODD, p1);
            DoubleVector t0r = a0r.add(a2r), t0i = a0i.add(a2i);
            DoubleVector t1r = a0r.sub(a2r), t1i = a0i.sub(a2i);
            DoubleVector t2r = a1r.add(a3r), t2i = a1i.add(a3i);
            DoubleVector t3r = a1r.sub(a3r), t3i = a1i.sub(a3i);
            t0r.add(t2r).intoArray(re, i0);
            t0i.add(t2i).intoArray(im, i0);
            DoubleVector yr = t0r.sub(t2r), yi = t0i.sub(t2i);
            DoubleVector c = DoubleVector.fromArray(S, t, 2 * q + j);
            DoubleVector s = DoubleVector.fromArray(S, t, 3 * q + j);
            yr.mul(c).sub(yi.mul(s)).intoArray(re, i1);
            yr.mul(s).add(yi.mul(c)).intoArray(im, i1);
            DoubleVector ur = t1r.sub(t3i), ui = t1i.add(t3r);
            DoubleVector vr = t1r.add(t3i), vi = t1i.sub(t3r);
            DoubleVector y1r = forward ? ur : vr, y1i = forward ? ui : vi;
            c = DoubleVector.fromArray(S, t, j);
            s = DoubleVector.fromArray(S, t, q + j);
            y1r.mul(c).sub(y1i.mul(s)).intoArray(re, i2);
            y1r.mul(s).add(y1i.mul(c)).intoArray(im, i2);
            DoubleVector y3r = forward ? vr : ur, y3i = forward ? vi : ui;
            c = DoubleVector.fromArray(S, t, 4 * q + j);
            s = DoubleVector.fromArray(S, t, 5 * q + j);
            y3r.mul(c).sub(y3i.mul(s)).intoArray(re, i3);
            y3r.mul(s).add(y3i.mul(c)).intoArray(im, i3);
        }
    }

    /** the 1st stage for odd log2(N), x[j], x[j + h] */
    private static void radix2(double[] re, double[] im, int h, double[] t) {
        for (int j = 0; j < h; j += SL) {
            DoubleVector ur = DoubleVector.fromArray(S, re, j);
            DoubleVector ui = DoubleVector.fromArray(S, im, j);
            DoubleVector vr = DoubleVector.fromArray(S, re, j + h);
            DoubleVector vi = DoubleVector.fromArray(S, im, j + h);
            DoubleVector c = DoubleVector.fromArray(S, t, j);
            DoubleVector s = DoubleVector.fromArray(S, t, h + j);
            ur.add(vr).intoArray(re, j);
            ui.add(vi).intoArray(im, j);
            DoubleVector dr = ur.sub(vr);
            DoubleVector di = ui.sub(vi);
            dr.mul(c).sub(di.mul(s)).intoArray(re, j + h);
            dr.mul(s).add(di.mul(c)).intoArray(im, j + h);
        }
    }

    /**
     * radix 4 blocks of 4q, quarter q (q is a multiple of the lanes).
     * outputs for r = 0, 2, 1, 3 are stored at b, b + q, b + 2q, b + 3q (bit reversed).
     */
    private static void radix4(double[] re, double[] im, int nh, int q, double[] t, boolean forward) {
        for (int b = 0; b < nh; b += 4 * q) {
            for (int j = 0; j < q; j += SL) {
                int i0 = b + j, i1 = i0 + q, i2 = i1 + q, i3 = i2 + q;
                DoubleVector a0r = DoubleVector.fromArray(S, re, i0);
                DoubleVector a0i = DoubleVector.fromArray(S, im, i0);
                DoubleVector a1r = DoubleVector.fromArray(S, re, i1);
                DoubleVector a1i = DoubleVector.fromArray(S, im, i1);
                DoubleVector a2r = DoubleVector.fromArray(S, re, i2);
                DoubleVector a2i = DoubleVector.fromArray(S, im, i2);
                DoubleVector a3r = DoubleVector.fromArray(S, re, i3);
                DoubleVector a3i = DoubleVector.fromArray(S, im, i3);
                DoubleVector t0r = a0r.add(a2r), t0i = a0i.add(a2i);
                DoubleVector t1r = a0r.sub(a2r), t1i = a0i.sub(a2i);
                DoubleVector t2r = a1r.add(a3r), t2i = a1i.add(a3i);
                DoubleVector t3r = a1r.sub(a3r), t3i = a1i.sub(a3i);
                // r = 0
                t0r.add(t2r).intoArray(re, i0);
                t0i.add(t2i).intoArray(im, i0);
                // r = 2
                DoubleVector yr = t0r.sub(t2r), yi = t0i.sub(t2i);
                DoubleVector c = DoubleVector.fromArray(S, t, 2 * q + j);
                DoubleVector s = DoubleVector.fromArray(S, t, 3 * q + j);
                yr.mul(c).sub(yi.mul(s)).intoArray(re, i1);
                yr.mul(s).add(yi.mul(c)).intoArray(im, i1);
                // t1 + i t3, t1 - i t3
                DoubleVector ur = t1r.sub(t3i), ui = t1i.add(t3r);
                DoubleVector vr = t1r.add(t3i), vi = t1i.sub(t3r);
                // r = 1: t1 + sign i t3
                DoubleVector y1r = forward ? ur : vr, y1i = forward ? ui : vi;
                c = DoubleVector.fromArray(S, t, j);
                s = DoubleVector.fromArray(S, t, q + j);
                y1r.mul(c).sub(y1i.mul(s)).intoArray(re, i2);
                y1r.mul(s).add(y1i.mul(c)).intoArray(im, i2);
                // r = 3: t1 - sign i t3
                DoubleVector y3r = forward ? vr : ur, y3i = forward ? vi : ui;
                c = DoubleVector.fromArray(S, t, 4 * q + j);
                s = DoubleVector.fromArray(S, t, 5 * q + j);
                y3r.mul(c).sub(y3i.mul(s)).intoArray(re, i3);
                y3r.mul(s).add(y3i.mul(c)).intoArray(im, i3);
            }
        }
    }

    /** {@link #radix4} for q less than the lanes */
    private static void radix4scalar(double[] re, double[] im, int b, int q, double[] t, boolean forward) {
        for (int j = 0; j < q; j++) {
            int i0 = b + j, i1 = i0 + q, i2 = i1 + q, i3 = i2 + q;
            double t0r = re[i0] + re[i2], t0i = im[i0] + im[i2];
            double t1r = re[i0] - re[i2], t1i = im[i0] - im[i2];
            double t2r = re[i1] + re[i3], t2i = im[i1] + im[i3];
            double t3r = re[i1] - re[i3], t3i = im[i1] - im[i3];
            re[i0] = t0r + t2r;
            im[i0] = t0i + t2i;
            double yr = t0r - t2r, yi = t0i - t2i;
            double c = t[2 * q + j], s = t[3 * q + j];
            re[i1] = yr * c - yi * s;
            im[i1] = yr * s + yi * c;
            double ur = t1r - t3i, ui = t1i + t3r;
            double vr = t1r + t3i, vi = t1i - t3r;
            double y1r = forward ? ur : vr, y1i = forward ? ui : vi;
            c = t[j];
            s = t[q + j];
            re[i2] = y1r * c - y1i * s;
            im[i2] = y1r * s + y1i * c;
            double y3r = forward ? vr : ur, y3i = forward ? vi : ui;
            c = t[4 * q + j];
            s = t[5 * q + j];
            re[i3] = y3r * c - y3i * s;
            im[i3] = y3r * s + y3i * c;
        }
    }

    /** the last radix 4 stage (q = 1), no twiddles */
    private static void radix4last(double[] re, double[] im, int nh, boolean forward) {
        for (int i0 = 0; i0 < nh; i0 += 4) {
            int i1 = i0 + 1, i2 = i0 + 2, i3 = i0 + 3;
            double t0r = re[i0] + re[i2], t0i = im[i0] + im[i2];
            double t1r = re[i0] - re[i2], t1i = im[i0] - im[i2];
            double t2r = re[i1] + re[i3], t2i = im[i1] + im[i3];
            double t3r = re[i1] - re[i3], t3i = im[i1] - im[i3];
            re[i0] = t0r + t2r;
            im[i0] = t0i + t2i;
            re[i1] = t0r - t2r;
            im[i1] = t0i - t2i;
            double ur = t1r - t3i, ui = t1i + t3r;
            double vr = t1r + t3i, vi = t1i - t3r;
            re[i2] = forward ? ur : vr;
            im[i2] = forward ? ui : vi;
            re[i3] = forward ? vr : ur;
            im[i3] = forward ? vi : ui;
        }
    }
}
