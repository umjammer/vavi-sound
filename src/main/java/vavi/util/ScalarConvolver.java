/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util;

import java.util.Arrays;


/**
 * {@link Convolver} by {@link Fft#rdft}, the {@link Fft} is selected by {@link Fft#getInstance()}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-23 nsano initial version <br>
 */
public class ScalarConvolver implements Convolver {

    private final Fft fft = Fft.getInstance();

    private int n;

    /** the spectrum of the filter */
    private double[] h;

    private int[] ip;

    private double[] w;

    @Override
    public void init(int n, double[] h) {
        this.n = n;
        this.h = Arrays.copyOf(h, n);
        ip = new int[(int) (2 + Math.sqrt(n))];
        w = new double[n / 2];
        fft.rdft(n, 1, this.h, ip, w);
    }

    @Override
    public void convolve(double[][] x, int nch) {
        for (int ch = 0; ch < nch; ch++) {
            double[] a = x[ch];
            fft.rdft(n, 1, a, ip, w);

            a[0] = h[0] * a[0];
            a[1] = h[1] * a[1];

            for (int i = 1; i < n / 2; i++) {
                double re = h[i * 2] * a[i * 2] - h[i * 2 + 1] * a[i * 2 + 1];
                double im = h[i * 2 + 1] * a[i * 2] + h[i * 2] * a[i * 2 + 1];
                a[i * 2] = re;
                a[i * 2 + 1] = im;
            }

            fft.rdft(n, -1, a, ip, w);
        }
    }
}
