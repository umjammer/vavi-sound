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
 * These routines comprise an implementation of the CCITT G.723 40Kbps
 * ADPCM coding algorithm.  Essentially, this implementation is identical to
 * the bit level description except for a few deviations which
 * take advantage of workstation attributes, such as hardware 2's
 * complement arithmetic.
 *
 * The deviation from the bit level specification (lookup tables),
 * preserves the bit level performance specifications.
 *
 * As outlined in the G.723 Recommendation, the algorithm is broken
 * down into modules.  Each section of code below is preceded by
 * the name of the module which it is implementing.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030713 nsano port to java <br>
 *          0.01 030714 nsano fine tune <br>
 */
class G723_40 extends G711 {

    /**
     * Maps G.723_40 code word to ructeconstructed scale factor normalized log
     * magnitude values.
     */
    private static final int[] _dqlntab = {
        -2048, -66, 28, 104, 169, 224, 274, 318,
        358, 395, 429, 459, 488, 514, 539, 566,
        566, 539, 514, 488, 459, 429, 395, 358,
        318, 274, 224, 169, 104, 28, -66, -2048
    };

    /** Maps G.723_40 code word to log of scale factor multiplier. */
    private static final int[] _witab = {
        448, 448, 768, 1248, 1280, 1312, 1856, 3200,
        4512, 5728, 7008, 8960, 11456, 14080, 16928, 22272,
        22272, 16928, 14080, 11456, 8960, 7008, 5728, 4512,
        3200, 1856, 1312, 1280, 1248, 768, 448, 448
    };

    /**
     * Maps G.723_40 code words to a set of values whose long and short
     * term averages are computed and then compared to give an indication
     * how stationary (steady state) the signal is.
     */
    private static final int[] _fitab = {
        0, 0, 0, 0, 0, 0x200, 0x200, 0x200,
        0x200, 0x200, 0x400, 0x600, 0x800, 0xA00, 0xC00, 0xC00,
        0xC00, 0xC00, 0xA00, 0x800, 0x600, 0x400, 0x200, 0x200,
        0x200, 0x200, 0x200, 0, 0, 0, 0, 0
    };

    /** */
    private static final int[] qtab_723_40 = {
        -122, -16, 68, 139, 198, 250, 298, 339,
        378, 413, 445, 475, 502, 528, 553
    };

    /**
     * Encodes a 16-bit linear PCM, A-law or u-law input sample and retuens
     * the resulting 5-bit CCITT G.723 40Kbps code.
     * @return -1 if the input coding value is invalid.
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

        // SUBTA
        int d = sl - se;                                    // d = estimation difference

        // quantize prediction difference
        // MIX
        int y = state.getStepSize();                        // adaptive quantizer step size
        int i = quantize(d, y, qtab_723_40, 15);            // i = ADPCM code

        // quantized diff
        int dq = reconstruct((i & 0x10) != 0, _dqlntab[i], y);

        // ADDB reconstructed signal
        int sr = (dq < 0) ? se - (dq & 0x7fff) : se + dq;

        // ADDC
        int dqsez = sr + sez - se;                          // dqsez = pole prediction diff.

        state.update(5, y, _witab[i], _fitab[i], dq, sr, dqsez);

        return i;
    }

    /**
     * Decodes a 5-bit CCITT G.723 40Kbps code and returns
     * the resulting 16-bit linear PCM, A-law or u-law sample value.
     * -1 is returned if the output coding is unknown.
     */
    @Override
    public int decode(int i) {

        i &= 0x1f;                                          // mask to get proper bits

        // ACCUM
        int sezi = state.getZeroPredictor();
        int sez = sezi >> 1;
        int sei = sezi + state.getPolePredictor();
        int se = sei >> 1;                                  // se = estimated signal

        // MIX
        int y = state.getStepSize();                        // adaptive quantizer step size
        // estimation diff.
        int dq = reconstruct((i & 0x10) != 0, _dqlntab[i], y);

        // ADDB reconst. signal
        int sr = (dq < 0) ? (se - (dq & 0x7fff)) : (se + dq);

        int dqsez = sr - se + sez;                          // pole prediction diff.

        state.update(5, y, _witab[i], _fitab[i], dq, sr, dqsez);

        if (AudioFormat.Encoding.ALAW.equals(encoding)) {
            return adjustAlawTandem(sr, se, y, i, 0x10, qtab_723_40);
        } else if (AudioFormat.Encoding.ULAW.equals(encoding)) {
            return adjustUlawTandem(sr, se, y, i, 0x10, qtab_723_40);
        } else if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
            return sr << 2;                                 // sr was of 14-bit dynamic range
        } else {
            throw new IllegalArgumentException(encoding.toString());
        }
    }

    @Override
    public int getEncodingBits() {
        return 5;
    }
}
