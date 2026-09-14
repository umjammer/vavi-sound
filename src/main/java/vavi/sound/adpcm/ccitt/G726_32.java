/*
 * Acknowledgement to Sun Microsystems, Inc. for having released the original
 * ANSI-C source code to the public domain.
 */

package vavi.sound.adpcm.ccitt;

import javax.sound.sampled.AudioFormat.Encoding;


/**
 * G726_32 encoder and decoder.
 * <p>
 * These routines comprise an implementation of the CCITT G.726 32kbps ADPCM
 * coding algorithm.  Essentially, this implementation is identical to
 * the bit level description except for a few deviations which
 * take advantage of work station attributes, such as hardware 2's
 * complement arithmetic and large memory.  Specifically, certain time
 * consuming operations such as multiplications are replaced
 * with lookup tables and software 2's complement operations are
 * replaced with hardware 2's complement.
 * <p>
 * The deviation from the bit level specification (lookup tables)
 * preserves the bit level performance specifications.
 * <p>
 * As outlined in the G.726 Recommendation, the algorithm is broken
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
class G726_32 extends G726 {

    private static final int[] qtab_721 = {-124, 80, 178, 246, 300, 349, 400};

    /**
     * Maps G726_32 code word to reconstructed scale factor normalized log
     * magnitude values.
     */
    private static final int[] _dqlntab = {
            -2048, 4, 135, 213, 273, 323, 373, 425, 425, 373, 323, 273, 213, 135, 4, -2048
    };

    /** Maps G726_32 code word to log of scale factor multiplier. */
    private static final int[] _witab = {
            -12, 18, 41, 64, 112, 198, 355, 1122, 1122, 355, 198, 112, 64, 41, 18, -12
    };

    /**
     * Maps G726_32 code words to a set of values whose long and short
     * term averages are computed and then compared to give an indication
     * how stationary (steady state) the signal is.
     */
    private static final int[] _fitab = {
            0, 0, 0, 0x200, 0x200, 0x200, 0x600, 0xE00, 0xE00, 0x600, 0x200, 0x200, 0x200, 0, 0, 0
    };

    /**
     * Encodes the input vale of linear PCM, A-law or u-law data sl and returns
     * the resulting code.
     * @throws IllegalArgumentException unknown encoding value
     */
    private int encode(int sl, G726State state) {

        int sezi, se, sez; // ACCUM
        int d; // SUBTA
        int sr; // ADDB
        int y; // MIX
        int dqsez; // ADDC
        int dq, i;

        if (encoding.equals(Encoding.ALAW)) { // linearize input sample to 14-bit PCM
            sl = alaw2linear((byte) sl) >> 2;
        } else if (encoding.equals(Encoding.ULAW)) {
            sl = ulaw2linear((byte) sl) >> 2;
        } else if (encoding.equals(Encoding.PCM_SIGNED)) {
            sl >>= 2; // 14-bit dynamic range
        } else
            throw new IllegalArgumentException(encoding.toString());

        sezi = state.predictor_zero();
        sez = sezi >> 1;
        se = (sezi + state.predictor_pole()) >> 1; // estimated signal

        d = sl - se; // estimation difference

        // quantize the prediction difference
        y = state.step_size(); // quantizer step size
        i = quantize(d, y, qtab_721, 7); // i = ADPCM code

        dq = reconstruct(i & 8, _dqlntab[i], y); // quantized est diff

        sr = (dq < 0) ? se - (dq & 0x3FFF) : se + dq; // reconst. signal

        dqsez = sr + sez - se; // pole prediction diff.

        update(4, y, _witab[i] << 5, _fitab[i], dq, sr, dqsez, state);

        return i;
    }

    /**
     * Decodes a 4-bit code of G726_32 encoded data of i and
     * returns the resulting linear PCM, A-law or u-law value.
     * @throws IllegalArgumentException unknown encoding value
     */
    private int decode(int i, G726State state) {

        int sezi, sei, sez, se; // ACCUM
        int y; // MIX
        int sr; // ADDB
        int dq;
        int dqsez;

        i &= 0x0f; // mask to get proper bits
        sezi = state.predictor_zero();
        sez = sezi >> 1;
        sei = sezi + state.predictor_pole();
        se = sei >> 1; // se = estimated signal

        y = state.step_size(); // dynamic quantizer step size

        dq = reconstruct(i & 0x08, _dqlntab[i], y); // quantized diff.

        sr = (dq < 0) ? (se - (dq & 0x3FFF)) : se + dq; // reconst. signal

        dqsez = sr - se + sez; // pole prediction diff.

        update(4, y, _witab[i] << 5, _fitab[i], dq, sr, dqsez, state);

        if (encoding.equals(Encoding.ALAW)) {
            return tandem_adjust_alaw(sr, se, y, i, 8, qtab_721);
        } else if (encoding.equals(Encoding.ULAW)) {
            return tandem_adjust_ulaw(sr, se, y, i, 8, qtab_721);
        } else if (encoding.equals(Encoding.PCM_SIGNED)) {
            return sr << 2; // sr was 14-bit dynamic range
        } else
            throw new IllegalArgumentException(encoding.toString());
    }

    /** Creates a new G726_32 processor, that can be used to encode from or decode do PCM audio data. */
    public G726_32() {
        super(32000);
    }

    /**
     * Encodes the input vale of linear PCM, A-law or u-law data sl and returns
     * the resulting code.
     * @throws IllegalArgumentException unknown encoding value
     */
    @Override
    public int encode(int sl) {
        return encode(sl, state);
    }

    /**
     * Decodes a 4-bit code of G726_32 encoded data of i and
     * returns the resulting linear PCM, A-law or u-law value.
     * @throws IllegalArgumentException for unknown encoding value
     */
    @Override
    public int decode(int i) {
        return decode(i, state);
    }

    @Override
    public int getEncodingBits() {
        return 4;
    }
}
