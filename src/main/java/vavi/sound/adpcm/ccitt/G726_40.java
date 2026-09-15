/*
 * Acknowledgement to Sun Microsystems, Inc. for having released the original
 * ANSI-C source code to the public domain.
 */

package vavi.sound.adpcm.ccitt;

import javax.sound.sampled.AudioFormat.Encoding;


/**
 * G726_40 encoder and decoder.
 * <p>
 * These routines comprise an implementation of the CCITT G.726 40kbps
 * ADPCM coding algorithm.  Essentially, this implementation is identical to
 * the bit level description except for a few deviations which
 * take advantage of workstation attributes, such as hardware 2's
 * complement arithmetic.
 * <p>
 * The deviation from the bit level specification (lookup tables),
 * preserves the bit level performance specifications.
 * <p>
 * As outlined in the G.723 Recommendation, the algorithm is broken
 * down into modules.  Each section of code below is preceded by
 * the name of the module which it is implementing.
 * <p>
 * This implementation is based on the ANSI-C language reference implementations
 * of the CCITT (International Telegraph and Telephone Consultative Committee)
 * G.711, G.721 and G.723 voice compressions, provided by Sun Microsystems, Inc.
 * <p>
 * Acknowledgement to Sun Microsystems, Inc. for having released the original
 * ANSI-C source code to the public domain.
 */
class G726_40 extends G726 {

    /**
     * Maps G723_40 code word to reconstructed scale factor normalized log
     * magnitude values.
     */
    private static final int[] _dqlntab = {
            -2048, -66, 28, 104, 169, 224, 274, 318, 358, 395, 429, 459, 488, 514, 539, 566, 566, 539, 514, 488, 459,
            429, 395, 358, 318, 274, 224, 169, 104, 28, -66, -2048
    };

    /** Maps G723_40 code word to log of scale factor multiplier. */
    private static final int[] _witab = {
            448, 448, 768, 1248, 1280, 1312, 1856, 3200, 4512, 5728, 7008, 8960, 11456, 14080, 16928, 22272, 22272,
            16928, 14080, 11456, 8960, 7008, 5728, 4512, 3200, 1856, 1312, 1280, 1248, 768, 448, 448
    };

    /**
     * Maps G723_40 code words to a set of values whose long and short
     * term averages are computed and then compared to give an indication
     * how stationary (steady state) the signal is.
     */
    private static final int[] _fitab = {
            0, 0, 0, 0, 0, 0x200, 0x200, 0x200, 0x200, 0x200, 0x400, 0x600, 0x800, 0xA00, 0xC00, 0xC00, 0xC00, 0xC00,
            0xA00, 0x800, 0x600, 0x400, 0x200, 0x200, 0x200, 0x200, 0x200, 0, 0, 0, 0, 0
    };

    private static final int[] qtab_723_40 = {
            -122, -16, 68, 139, 198, 250, 298, 339, 378, 413, 445, 475, 502, 528, 553
    };

    /**
     * Encodes a 16-bit linear PCM, A-law or u-law input sample and returns
     * the resulting 5-bit CCITT G726 40kbps code.
     * @throws IllegalArgumentException the encoding value is invalid
     */
    private int encode(int sl, G726State state) {

        int sei, sezi, se, sez; // ACCUM
        int d; // SUBTA
        int y; // MIX
        int sr; // ADDB
        int dqsez; // ADDC
        int dq, i;

        if (encoding.equals(Encoding.ALAW)) { // linearize input sample to 14-bit PCM
            sl = alaw2linear((byte) sl) >> 2;
        } else if (encoding.equals(Encoding.ULAW)) {
            sl = ulaw2linear((byte) sl) >> 2;
        } else if (encoding.equals(Encoding.PCM_SIGNED)) {
            sl >>= 2; // sl of 14-bit dynamic range
        } else
            throw new IllegalArgumentException(encoding.toString());

        sezi = state.predictor_zero();
        sez = sezi >> 1;
        sei = sezi + state.predictor_pole();
        se = sei >> 1; // se = estimated signal

        d = sl - se; // d = estimation difference

        // quantize prediction difference
        y = state.step_size(); // adaptive quantizer step size
        i = quantize(d, y, qtab_723_40, 15); // i = ADPCM code

        dq = reconstruct(i & 0x10, _dqlntab[i], y); // quantized diff

        sr = (dq < 0) ? se - (dq & 0x7FFF) : se + dq; // reconstructed signal

        dqsez = sr + sez - se; // dqsez=pole prediction diff.

        update(5, y, _witab[i], _fitab[i], dq, sr, dqsez, state);

        return i;
    }

    /**
     * Decodes a 5-bit CCITT G.726 40kbps code and returns
     * the resulting 16-bit linear PCM, A-law or u-law sample value.
     * @throws IllegalArgumentException the encoding is unknown
     */
    private int decode(int i, G726State state) {

        int sezi, sei, sez, se; // ACCUM
        int y, dif; // MIX
        int sr; // ADDB
        int dq;
        int dqsez;

        i &= 0x1f; // mask to get proper bits
        sezi = state.predictor_zero();
        sez = sezi >> 1;
        sei = sezi + state.predictor_pole();
        se = sei >> 1; // se=estimated signal

        y = state.step_size(); // adaptive quantizer step size
        dq = reconstruct(i & 0x10, _dqlntab[i], y); // estimation diff.

        sr = (dq < 0) ? (se - (dq & 0x7FFF)) : (se + dq); // reconst. signal

        dqsez = sr - se + sez; // pole prediction diff.

        update(5, y, _witab[i], _fitab[i], dq, sr, dqsez, state);

        if (encoding.equals(Encoding.ALAW)) {
            return tandem_adjust_alaw(sr, se, y, i, 0x10, qtab_723_40);
        } else if (encoding.equals(Encoding.ULAW)) {
            return tandem_adjust_ulaw(sr, se, y, i, 0x10, qtab_723_40);
        } else if (encoding.equals(Encoding.PCM_SIGNED)) {
            return sr << 2; // sr was of 14-bit dynamic range
        } else
            throw new IllegalArgumentException(encoding.toString());
    }

    /** Creates a new G726_40 processor, that can be used to encode from or decode do PCM audio data. */
    public G726_40() {
        super(40000);
    }

    /**
     * Encodes a 16-bit linear PCM, A-law or u-law input sample and returns
     * the resulting 5-bit CCITT G.726 40kbps code.
     * @throws IllegalArgumentException the encoding value is invalid
     */
    @Override
    public int encode(int sl) {
        return encode(sl, state);
    }

    /**
     * Decodes a 5-bit CCITT G.726 40kbps code and returns
     * the resulting 16-bit linear PCM, A-law or u-law sample value.
     * @throws IllegalArgumentException the encoding is unknown
     */
    @Override
    public int decode(int i) {
        return decode(i, state);
    }

    @Override
    public int getEncodingBits() {
        return 5;
    }
}
