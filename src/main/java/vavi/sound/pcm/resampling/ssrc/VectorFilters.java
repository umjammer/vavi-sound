/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.ssrc;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;


/**
 * the {@link SSRC} polyphase filters with the Vector API.
 * <p>
 * the interleaved stereo samples (L, R) are a 2 lanes vector,
 * the both channels are done at once lane wise in the same order as the scalar ones, so the result is bit exact.
 * this class is loaded only when jdk.incubator.vector is available.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-23 nsano initial version <br>
 */
final class VectorFilters {

    private VectorFilters() {}

    /** 2 doubles, (L, R) */
    private static final VectorSpecies<Double> S2 = DoubleVector.SPECIES_128;

    /**
     * the upsampler stage 1 filter for stereo.
     *
     * @param in interleaved stereo samples
     * @param ip the position of the 1st sample of the left channel in {@code in}
     * @param s1p the 1st phase
     * @param no the number of the phases
     * @param n the number of the outputs
     * @param out0 the left output
     * @param out1 the right output
     * @return the next phase
     */
    static int upStage1(double[] in, int ip, double[][] stage1, int[] f1order, int[] f1inc, int s1p, int no, int n, int n1x,
                        double[] out0, double[] out1) {
        for (int p = 0; p < n; p++) {
            double[] c = stage1[f1order[s1p]];
            DoubleVector acc = DoubleVector.zero(S2);
            int ip2 = ip;
            for (int i = 0; i < n1x; i++) {
                acc = acc.add(DoubleVector.fromArray(S2, in, ip2).mul(c[i]));
                ip2 += 2;
            }
            out0[p] = acc.lane(0);
            out1[p] = acc.lane(1);

            ip += f1inc[s1p];

            s1p++;
            if (s1p == no) {
                s1p = 0;
            }
        }
        return s1p;
    }
}
