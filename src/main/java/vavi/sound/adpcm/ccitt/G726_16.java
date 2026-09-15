/*
 * Acknowledgement to Sun Microsystems, Inc. for having released the original
 * ANSI-C source code to the public domain.
 */

package vavi.sound.adpcm.ccitt;

import javax.sound.sampled.AudioFormat.Encoding;


/**
 * G726_16 encoder and decoder.
 * <p>
 * These routines comprise an implementation of the CCITT G.726 16kbps
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
class G726_16 extends G726 {

    /**
     * Maps G723_16 code word to reconstructed scale factor normalized log
     * magnitude values.
     */
    private static final int[] _dqlntab = {116, 365, 365, 116};

    /** Maps G723_16 code word to log of scale factor multiplier. */
    private static final int[] _witab = {-704, 14048, 14048, -704};

    /**
     * Maps G723_16 code words to a set of values whose long and short
     * term averages are computed and then compared to give an indication
     * how stationary (steady state) the signal is.
     */
    private static final int[] _fitab = {0x000, 0xE00, 0xE00, 0x000};

    private static final int[] qtab_723_16 = {261};

    /**
     * Encodes a 16-bit linear PCM, A-law or u-law input sample and returns
     * the resulting 5-bit CCITT G726 16kbps code.
     * @throws IllegalArgumentException if the encoding is invalid
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
        i = quantize(d, y, qtab_723_16, 1); // i = ADPCM code

        // Since quantize() only produces a three level output
        // (1, 2, or 3), we must create the fourth one on our own
        if (i == 3) { // i code for the zero region
            if ((d & 0x8000) == 0) // If d > 0, i=3 isn't right...
                i = 0;
        }

        dq = reconstruct(i & 0x02, _dqlntab[i], y); // quantized diff

        sr = (dq < 0) ? se - (dq & 0x3FFF) : se + dq; // reconstructed signal

        dqsez = sr + sez - se; // dqsez=pole prediction diff.

        update(2, y, _witab[i], _fitab[i], dq, sr, dqsez, state);

        return i;
    }

    /**
     * Decodes a 5-bit CCITT G.726 40kbps code and returns
     * the resulting 16-bit linear PCM, A-law or u-law sample value.
     * @throws IllegalArgumentException if the encoding is invalid
     */
    private int decode(int i, G726State state) {

        int sezi, sei, sez, se; // ACCUM
        int y; // MIX
        int sr; // ADDB
        int dq;
        int dqsez;

        i &= 0x03; // mask to get proper bits
        sezi = state.predictor_zero();
        sez = sezi >> 1;
        sei = sezi + state.predictor_pole();
        se = sei >> 1; // se = estimated signal

        y = state.step_size(); // adaptive quantizer step size
        dq = reconstruct(i & 0x02, _dqlntab[i], y); // estimation diff.

        sr = (dq < 0) ? (se - (dq & 0x3FFF)) : (se + dq); // reconst. signal

        dqsez = sr - se + sez; // pole prediction diff.

        update(2, y, _witab[i], _fitab[i], dq, sr, dqsez, state);

        if (encoding.equals(Encoding.ALAW)) {
            return tandem_adjust_alaw(sr, se, y, i, 0x02, qtab_723_16);
        } else if (encoding.equals(Encoding.ULAW)) {
            return tandem_adjust_ulaw(sr, se, y, i, 0x02, qtab_723_16);
        } else if (encoding.equals(Encoding.PCM_SIGNED)) {
            return sr << 2; // sr was of 14-bit dynamic range
        } else
            throw new IllegalArgumentException(encoding.toString());
    }

    /** Creates a new G726_16 processor, that can be used to encode from or decode do PCM audio data. */
    public G726_16() {
        super(16000);
    }

    /**
     * Encodes a 16-bit linear PCM, A-law or u-law input sample and retuens
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
        return 2;
    }
}
