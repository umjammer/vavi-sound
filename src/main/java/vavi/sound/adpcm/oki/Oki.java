/*
 * Copyright (c) 2001 Tetsuya Isaki. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *      This product includes software developed by Tetsuya Isaki.
 * 4. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package vavi.sound.adpcm.oki;

import vavi.sound.adpcm.Codec;


/**
 * OKI MSM6258 ADPCM voice synthesizer codec.
 * <p>
 * TODO support 8 bit
 * </p>
 * @author Tetsuya Isaki
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030816 nsano port to java <br>
 * @see "http://www.pastel-flower.jp/~isaki/NetBSD/src/?sys/dev/ic/msm6258.c"
 */
class Oki implements Codec {

    /** */
    private int mc_amp;
    /** */
    private int mc_estim;

    /** table for next PCM value prediction */
    private static final int[] adpcm_estimindex = {
        2,  6,  10,  14,  18,  22,  26,  30,
        -2, -6, -10, -14, -18, -22, -26, -30
    };

    /** quantizing width */
    private static final int[] adpcm_estim = {
        16,  17,  19,  21,  23,  25,  28,  31,  34,  37,
        41,  45,  50,  55,  60,  66,  73,  80,  88,  97,
        107, 118, 130, 143, 157, 173, 190, 209, 230, 253,
        279, 307, 337, 371, 408, 449, 494, 544, 598, 658,
        724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552
    };

    /** what for? */
    private static final int[] adpcm_estimstep = {
        -1, -1, -1, -1, 2, 4, 6, 8,
        -1, -1, -1, -1, 2, 4, 6, 8
    };

    /**
     * Converts signed linear 16 1 sample into Oki ADPCM 1 sample.
     * <p>
     * MSM6258 expect 12bit signed PCM (corresponds to slinear12),
     * so amplitude conversion (16bit -> 12bit) is done also.
     * </p>
     *
     * @param a 16bit signed linear pcm
     * @return 4bit oki adpcm
     */
    @Override
    public int encode(int a) {

        // `mc_estim` holds previous difference prediction index.
        int estim = this.mc_estim;
        int b;
        int s;

        // `df` is difference between real PCM value `a` and predicted PCM value
        int df = a - this.mc_amp;
        // dl is difference ratio of predicted value from difference prediction table `adpcm_estim[]`.
        int dl = adpcm_estim[estim];
        // `c` is ratio of 12bit converted difference and `(dl/8)`
        // `/16` is difference 16bit and 12bit, that means `2^4`.
        // `*8` is `dl`, that means because `adpcm_estim[]` is stored multiple 8 times
        int c = (df / 16) * 8 / dl;
        // processing changes depending on `c` is positive or negative.
        // except when `c` is 0 by division, avoiding the value is dealt as positive number,
        // use `df` for determining sign
        //
        // actual encoded ADPCM data consists a sign bit and amplitude bits.
        // amplitude bits are division `c` by 2.
        if (df < 0) {
            b = -c / 2;
            s = 0x08;
        } else {
            b = c / 2;
            s = 0;
        }
        // amplitude are 3bits, so limit by 7
        if (b > 7) {
            b = 7;
        }
        // by doing this, `s` will be a signed 4 bit from now on.
        // `b` can be used as an unsigned absolute value
        s |= b;
        // conversion so far, relation of amplitude bits `b` and real ratio `c` is below
        // b : ratio range c
        // 0 : 0 <= ratio < 2
        // 1 : 2 <= ratio < 4
        // 2 : 4 <= ratio < 6
        // 3 : 6 <= ratio < 8
        // 4 : 8 <= ratio < 10
        // 5 : 10 <= ratio < 12
        // 6 : 12 <= ratio < 14
        // 7 : 14 <= ratio
        //
        // prediction next PCM value. It's simply but actually
        //
        //  static int adpcm_estimindex_0[16] = {
        //    1,  3,  5,  7,  9,  11,  13,  15,
        //   -1, -3, -5, -7, -9, -11, -13, -15
        //  };
        //  mc->mc_amp += (short) (adpcm_estimindex_0[(int) s] * 16 / 8 * dl);
        //
        // `adpcm_estimindex_0[]` is 1/2 of `adpcm_estimindex[]`
        // and this array means median of ratio ranges above.
        // multiplying it by `16 = 2 ^ 4`, make it 16 bit. and multiplying it by `(dl / 8)`
        // prediction difference is calculated. then store it.
        // that's because multiplying `adpcm_estimindex[]` by 2 in advance
        // make it simple.
        this.mc_amp += adpcm_estimindex[s] * dl;
        // predict next difference ratio using `b` then store it to `mc_estim`.
        // those are all 49 steps.
        // As a side note for only this method `adpcm_estimstep[16]` size [8] is enough.
        // `adpcm2pcm` requires [16].
        estim += adpcm_estimstep[b];
        if (estim < 0) {
            estim = 0;
        } else if (estim > 48) {
            estim = 48;
        }

        this.mc_estim = estim;
        return s;
    }

    /**
     * Converts Oki ADPCM 1 sample to signed linear 16 1 sample.
     * <p>
     * output PCM by MSM6258 is 12bit signed PCM (corresponds to slinear12).
     * so amplitude conversion (12bit -> 16bit) is done also.
     * </p>
     *
     * @param b 4bit adpcm
     * @return 16bit linear pcm
     */
    @Override
    public int decode(int b) {
        // `mc_estim` holds previous difference prediction index.
        int estim = this.mc_estim;

        // calc actual PCM value. originally a formula is
        //
        //  mc.mc_amp += adpcm_estim[estim] / 8 * adpcm_estimindex_0[b] * 16;
        //
        // as described in `pcm2adpcm_step()` `adpcm_estim[]` are 8x values,
        // divide by 8. then multiply it by a ratio of `adpcm_estimindex_0[]`.
        // and then multiply it by `16 (= 2^4)` for 12bit -> 16bit conversion.
        // `adpcm_estimindex_0[] * 2` is `adpcm_estimindex[]`, so
        // a formula above become simply below
        this.mc_amp += adpcm_estim[estim] * adpcm_estimindex[b];
        // predict next difference ration, then store it to `mc_estim`
        estim += adpcm_estimstep[b];

        if (estim < 0) {
            estim = 0;
        } else if (estim > 48) {
            estim = 48;
        }

        this.mc_estim = estim;

        return this.mc_amp;
    }
}
