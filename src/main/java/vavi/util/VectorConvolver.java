/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util;

import java.util.Arrays;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;


/**
 * {@link Convolver} by a complex fft with the Vector API.
 * <p>
 * as the filter is real, two real channels are done at once as a complex signal (x0 + i x1),
 * the channel arrays are used as the real and the imaginary arrays directly.
 * the spectrum is left bit reversed, the filter spectrum is made by the same fft,
 * then the inverse fft takes the bit reversed order, so no reordering and no packing are needed
 * ({@link VectorRadix4Fft#fft}, {@link VectorRadix4Fft#ifft}).
 * the result is not bit exact to {@link ScalarConvolver}, the difference is a rounding error.
 * needs {@code --add-modules jdk.incubator.vector}.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-23 nsano initial version <br>
 */
public class VectorConvolver implements Convolver {

    private static final VectorSpecies<Double> S = DoubleVector.SPECIES_PREFERRED;

    private static final int SL = S.length();

    /** smaller filters are done by {@link ScalarConvolver} */
    private static final int MIN_N = Math.max(64, 8 * SL);

    private int n;

    private VectorRadix4Fft.Plan plan;

    /** the filter spectrum (bit reversed) / 2 */
    private double[] hr, hi;

    /** the imaginary part for an odd channel */
    private double[] zero;

    /** for a small n */
    private Convolver fallback;

    @Override
    public boolean isExact() {
        return false;
    }

    @Override
    public void init(int n, double[] h) {
        this.n = n;
        if (n < MIN_N) {
            fallback = new ScalarConvolver();
            fallback.init(n, h);
            return;
        }
        // a plan for n complex points
        plan = VectorRadix4Fft.plans.computeIfAbsent(2 * n, VectorRadix4Fft.Plan::new);
        hr = Arrays.copyOf(h, n);
        hi = new double[n];
        VectorRadix4Fft.fft(plan, null, hr, hi, true);
        // ifft(fft(x)) is n x, the rdft way is n / 2 x
        for (int i = 0; i < n; i++) {
            hr[i] *= 0.5;
            hi[i] *= 0.5;
        }
    }

    @Override
    public void convolve(double[][] x, int nch) {
        if (fallback != null) {
            fallback.convolve(x, nch);
            return;
        }
        int ch = 0;
        for (; ch + 1 < nch; ch += 2) {
            convolve(x[ch], x[ch + 1]);
        }
        if (ch < nch) {
            if (zero == null) {
                zero = new double[n];
            } else {
                Arrays.fill(zero, 0);
            }
            convolve(x[ch], zero);
        }
    }

    /** re + i im */
    private void convolve(double[] re, double[] im) {
        VectorRadix4Fft.fft(plan, null, re, im, true, true);
        middle(re, im, hr, hi, n);
        VectorRadix4Fft.ifft(plan, re, im, true);
    }

    /**
     * the last stage of the fft (forward radix 4, q = 1), multiplying the filter spectrum,
     * and the 1st stage of the inverse fft (q = 1) in a pass.
     */
    private static void middle(double[] re, double[] im, double[] hr, double[] hi, int n) {
        for (int i0 = 0; i0 < n; i0 += 4) {
            int i1 = i0 + 1, i2 = i0 + 2, i3 = i0 + 3;
            // VectorRadix4Fft#radix4last
            double t0r = re[i0] + re[i2], t0i = im[i0] + im[i2];
            double t1r = re[i0] - re[i2], t1i = im[i0] - im[i2];
            double t2r = re[i1] + re[i3], t2i = im[i1] + im[i3];
            double t3r = re[i1] - re[i3], t3i = im[i1] - im[i3];
            double y0r = t0r + t2r, y0i = t0i + t2i;
            double y1r = t0r - t2r, y1i = t0i - t2i;
            double y2r = t1r - t3i, y2i = t1i + t3r;
            double y3r = t1r + t3i, y3i = t1i - t3r;
            // the filter
            double hr0 = hr[i0], hi0 = hi[i0], hr1 = hr[i1], hi1 = hi[i1], hr2 = hr[i2], hi2 = hi[i2], hr3 = hr[i3], hi3 = hi[i3];
            double z0r = y0r * hr0 - y0i * hi0, z0i = y0r * hi0 + y0i * hr0;
            double z1r = y1r * hr1 - y1i * hi1, z1i = y1r * hi1 + y1i * hr1;
            double z2r = y2r * hr2 - y2i * hi2, z2i = y2r * hi2 + y2i * hr2;
            double z3r = y3r * hr3 - y3i * hi3, z3i = y3r * hi3 + y3i * hr3;
            // VectorRadix4Fft#dit4last
            double s02r = z0r + z1r, s02i = z0i + z1i;
            double d02r = z0r - z1r, d02i = z0i - z1i;
            double s13r = z2r + z3r, s13i = z2i + z3i;
            double d13r = z2r - z3r, d13i = z2i - z3i;
            re[i0] = s02r + s13r;
            im[i0] = s02i + s13i;
            re[i1] = d02r + d13i;
            im[i1] = d02i - d13r;
            re[i2] = s02r - s13r;
            im[i2] = s02i - s13i;
            re[i3] = d02r - d13i;
            im[i3] = d02i + d13r;
        }
    }
}
