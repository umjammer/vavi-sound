/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * ConvolverTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-23 nsano initial version <br>
 */
class ConvolverTest {

    @Test
    @DisplayName("selection by name")
    void testInstance() {
        assertInstanceOf(ScalarConvolver.class, Convolver.getInstance("scalar"));
        assertInstanceOf(VectorConvolver.class, Convolver.getInstance("vector"));
        assertInstanceOf(ScalarConvolver.class, Convolver.getInstance("no such convolver"));
    }

    @Test
    @DisplayName("vector is the same as scalar within rounding errors")
    void testSame() {
        for (int n = 4; n <= 1 << 17; n <<= 1) {
            double[] h = FftTest.random(n, n);
            for (int i = n / 2; i < n; i++) h[i] = 0; // as ssrc, a filter and zero padding
            Convolver scalar = new ScalarConvolver();
            Convolver vector = new VectorConvolver();
            scalar.init(n, h);
            vector.init(n, h);
            for (int nch = 1; nch <= 3; nch++) {
                double[][] s = new double[nch][];
                double[][] v = new double[nch][];
                for (int ch = 0; ch < nch; ch++) {
                    s[ch] = FftTest.random(n, n * 7L + ch);
                    v[ch] = s[ch].clone();
                }
                scalar.convolve(s, nch);
                vector.convolve(v, nch);
                for (int ch = 0; ch < nch; ch++) {
                    double max = 0, err = 0;
                    for (int i = 0; i < n; i++) {
                        max = Math.max(max, Math.abs(s[ch][i]));
                        err = Math.max(err, Math.abs(s[ch][i] - v[ch][i]));
                    }
                    assertTrue(err / max < 1e-13, "n=" + n + ", nch=" + nch + ", ch=" + ch + ", err=" + err / max);
                }
            }
        }
    }
}
