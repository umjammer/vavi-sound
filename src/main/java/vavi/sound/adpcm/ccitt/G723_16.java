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
 * These routines comprise an implementation of the CCITT G.726 16 Kbps
 * ADPCM coding algorithm.  Essentially, this implementation is identical to
 * the bit level description except for a few deviations which take advantage
 * of workstation attributes, such as hardware 2's complement arithmetic.
 *
 * 16kbps version created, used 24kbps code and changing as little as possible.
 * G.726 specs are available from ITU's gopher or WWW site (http://www.itu.ch)
 * If any errors are found, please contact me at mrand@tamu.edu
 *      -Marc Randolph
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030713 nsano port to java <br>
 *          0.01 030714 nsano fine tune <br>
 */
class G723_16 extends G711 {

    /**
     * Maps G.723_16 code word to reconstructed scale factor normalized log
     * magnitude values.  Comes from Table 11/G.726
     */
    private static final int[] _dqlntab = { 116, 365, 365, 116 };

    /**
     * Maps G.723_16 code word to log of scale factor multiplier.
     *
     * _witab[4] is actually {-22 , 439, 439, -22}, but FILTD wants it
     * as WI << 5  (multiplied by 32), so we'll do that here
     */
    private static final int[] _witab = { -704, 14048, 14048, -704 };

    /*
     * Maps G.723_16 code words to a set of values whose long and short
     * term averages are computed and then compared to give an indication
     * how stationary (steady state) the signal is.
     */

    /** Comes from FUNCTF */
    private static final int[] _fitab = { 0, 0xe00, 0xe00, 0 };

    /**
     * Comes from quantizer decision level tables (Table 7/G.726)
     */
    private static int[] qtab_723_16 = { 261 };

    /**
     * Encodes a linear PCM, A-law or u-law input sample and returns its 2-bit
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

        //ACCUM
        int sezi = state.getZeroPredictor();
        int sez = sezi >> 1;
        int sei = sezi + state.getPolePredictor();
        int se = sei >> 1;                                  // se = estimated signal

        // SUBTA
        int d = sl - se;                                    // d = estimation diff.

        // quantize prediction difference d
        // MIX
        int y = state.getStepSize();                        // quantizer step size
        int i = quantize(d, y, qtab_723_16, 1);             // i = ADPCM code

        // Since quantize() only produces a three level output
        // (1, 2, or 3), we must create the fourth one on our own
        if (i == 3) {                                       // i code for the zero region
            if ((d & 0x8000) == 0) {                        // If d > 0, i=3 isn't right...
                i = 0;
            }
        }

        // quantized diff.
        int dq = reconstruct((i & 2) != 0, _dqlntab[i], y);

        // ADDB reconstructed signal
        int sr = (dq < 0) ? se - (dq & 0x3fff) : se + dq;

        // ADDC
        int dqsez = sr + sez - se;                          // pole prediction diff.

        state.update(2, y, _witab[i], _fitab[i], dq, sr, dqsez);

        return i;
    }

    /**
     * Decodes a 2-bit CCITT G.723_16 ADPCM code and returns
     * the resulting 16-bit linear PCM, A-law or u-law sample value.
     * -1 is returned if the output coding is unknown.
     */
    @Override
    public int decode(int i) {

        i &= 0x03;                                          // mask to get proper bits

        // ACCUM
        int sezi = state.getZeroPredictor();
        int sez = sezi >> 1;
        int sei = sezi + state.getPolePredictor();
        int se = sei >> 1;                                  // se = estimated signal

        // MIX
        int y = state.getStepSize();                        // adaptive quantizer step size
        // unquantize pred diff
        int dq = reconstruct((i & 0x02) != 0, _dqlntab[i], y);

        // ADDB reconst. signal
        int sr = (dq < 0) ? (se - (dq & 0x3fff)) : (se + dq);

        int dqsez = sr - se + sez;                          // pole prediction diff.

        state.update(2, y, _witab[i], _fitab[i], dq, sr, dqsez);

        if (AudioFormat.Encoding.ALAW.equals(encoding)) {
            return adjustAlawTandem(sr, se, y, i, 2, qtab_723_16);
        } else if (AudioFormat.Encoding.ULAW.equals(encoding)) {
            return adjustUlawTandem(sr, se, y, i, 2, qtab_723_16);
        } else if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
            return sr << 2;                                 // sr was of 14-bit dynamic range
        } else {
            throw new IllegalArgumentException(encoding.toString());
        }
    }

    @Override
    public int getEncodingBits() {
        return 2;
    }
}
