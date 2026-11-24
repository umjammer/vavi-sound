/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util;


/**
 * The scalar fft, {@link SplitRadixFft} as {@link Fft}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-08 nsano initial version <br>
 */
public class ScalarFft implements Fft {

    @Override
    public void rdft(int n, int isgn, double[] a, int[] ip, double[] w) {
        SplitRadixFft.rdft(n, isgn, a, ip, w);
    }
}
