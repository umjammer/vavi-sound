/*
 * This source code is a product of Sun Microsystems, Inc. and is provided
 * for unrestricted use.  Users may copy or modify this source code without
 * charge.
 *
 * SUN SOURCE CODE IS PROVIDED AS IS WITH NO WARRANTIES OF ANY KIND INCLUDING
 * THE WARRANTIES OF DESIGN, MERCHANTIBILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE, OR ARISING FROM A COURSE OF DEALING, USAGE OR TRADE PRACTICE.
 *
 * Sun source code is provided with no support and without any obligation on
 * the part of Sun Microsystems, Inc. to assist in its use, correction,
 * modification or enhancement.
 *
 * SUN MICROSYSTEMS, INC. SHALL HAVE NO LIABILITY WITH RESPECT TO THE
 * INFRINGEMENT OF COPYRIGHTS, TRADE SECRETS OR ANY PATENTS BY THIS SOFTWARE
 * OR ANY PART THEREOF.
 *
 * In no event will Sun Microsystems, Inc. be liable for any lost revenue
 * or profits or other special, indirect and consequential damages, even if
 * Sun has been advised of the possibility of such damages.
 *
 * Sun Microsystems, Inc.
 * 2550 Garcia Avenue
 * Mountain View, California  94043
 */

package vavi.sound.adpcm.ccitt;

import javax.sound.sampled.AudioFormat;


/**
 * These routines comprise an implementation of the CCITT G.723 24 Kbps
 * ADPCM coding algorithm.  Essentially, this implementation is identical to
 * the bit level description except for a few deviations which take advantage
 * of workstation attributes, such as hardware 2's complement arithmetic.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030713 nsano port to java <br>
 *          0.01 030714 nsano fine tune <br>
 */
class G723_24 extends G711 {

    /**
     * Maps G.723_24 code word to reconstructed scale factor normalized log
     * magnitude values.
     */
    private static int[] _dqlntab = {
        -2048, 135, 273, 373, 373, 273, 135, -2048
    };

    /** Maps G.723_24 code word to log of scale factor multiplier. */
    private static int[] _witab = {
        -128, 960, 4384, 18624, 18624, 4384, 960, -128
    };

    /**
     * Maps G.723_24 code words to a set of values whose long and short
     * term averages are computed and then compared to give an indication
     * how stationary (steady state) the signal is.
     */
    private static int[] _fitab = {
        0, 0x200, 0x400, 0xE00, 0xE00, 0x400, 0x200, 0
    };

    /** */
    private static int[] qtab_723_24 = { 8, 218, 331 };

    /**
     * Encodes a linear PCM, A-law or u-law input sample and returns its 3-bit
     * code.
     * @return -1 if invalid input coding value.
     */
    @Override
    public int encode(int sl) {

        // linearize input sample to 14-bit PCM
        if (AudioFormat.Encoding.ALAW.equals(encoding)) {
            sl = alaw2linear(sl) >> 2;
        } else if (AudioFormat.Encoding.ULAW.equals(encoding)) {
            sl = ulaw2linear(sl) >> 2;
        } else if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
            sl >>= 2;                                       // sl of 14-bit dynamic range
        } else {
            throw new IllegalArgumentException(encoding.toString());
        }

        // ACCUM
        int sezi = state.getZeroPredictor();
        int sez = sezi >> 1;
        int sei = sezi + state.getPolePredictor();
        int se = sei >> 1;                                  // se = estimated signal

        // SUBTA estimation diff.
        int d = sl - se;

        // MIX quantize prediction difference d
        int y = state.getStepSize();                        // quantizer step size
        int i = quantize(d, y, qtab_723_24, 3);             // i = ADPCM code
        // quantized diff.
        int dq = reconstruct((i & 4) != 0, _dqlntab[i], y);

        // ADDB reconstructed signal
        int sr = (dq < 0) ? se - (dq & 0x3fff) : se + dq;

        // ADDC pole prediction diff.
        int dqsez = sr + sez - se;

        state.update(3, y, _witab[i], _fitab[i], dq, sr, dqsez);

        return i;
    }

    /**
     * Decodes a 3-bit CCITT G.723_24 ADPCM code and returns
     * the resulting 16-bit linear PCM, A-law or u-law sample value.
     * -1 is returned if the output coding is unknown.
     */
    @Override
    public int decode(int i) {

        i &= 0x07;                                          // mask to get proper bits

        // ACCUM
        int sezi = state.getZeroPredictor();
        int sez = sezi >> 1;
        int sei = sezi + state.getPolePredictor();
        int se = sei >> 1;                                  // se = estimated signal

        // MIX
        int y = state.getStepSize();                        // adaptive quantizer step size
        // unquantize pred diff
        int dq = reconstruct((i & 0x04) != 0, _dqlntab[i], y);
        // ADDB reconst. signal
        int sr = (dq < 0) ? (se - (dq & 0x3fff)) : (se + dq);
        int dqsez = sr - se + sez;                          // pole prediction diff.

        state.update(3, y, _witab[i], _fitab[i], dq, sr, dqsez);

        if (AudioFormat.Encoding.ALAW.equals(encoding)) {
            return adjustAlawTandem(sr, se, y, i, 4, qtab_723_24);
        } else if (AudioFormat.Encoding.ULAW.equals(encoding)) {
            return adjustUlawTandem(sr, se, y, i, 4, qtab_723_24);
        } else if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
            return sr << 2;                                 // sr was of 14-bit dynamic range
        } else {
            throw new IllegalArgumentException(encoding.toString());
        }
    }

    @Override
    public int getEncodingBits() {
        return 3;
    }
}

/* */
