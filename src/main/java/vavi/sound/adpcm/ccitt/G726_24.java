/*
 * Acknowledgement to Sun Microsystems, Inc. for having released the original
 * ANSI-C source code to the public domain.
 */

package vavi.sound.adpcm.ccitt;

import javax.sound.sampled.AudioFormat.Encoding;


/**
 * G726_24 encoder and decoder.
 * <p>
 * These routines comprise an implementation of the CCITT G.726 24kbps
 * ADPCM coding algorithm.  Essentially, this implementation is identical to
 * the bit level description except for a few deviations which take advantage
 * of workstation attributes, such as hardware 2's complement arithmetic.
 * <p>
 * This implementation is based on the ANSI-C language reference implementations
 * of the CCITT (International Telegraph and Telephone Consultative Committee)
 * G.711, G.721 and G.723 voice compressions, provided by Sun Microsystems, Inc.
 * <p>
 * Acknowledgement to Sun Microsystems, Inc. for having released the original
 * ANSI-C source code to the public domain.
 */
class G726_24 extends G726 {

    /**
     * Maps G726_24 code word to reconstructed scale factor normalized log
     * magnitude values.
     */
    private static final int[] _dqlntab = {-2048, 135, 273, 373, 373, 273, 135, -2048};

    /** Maps G726_24 code word to log of scale factor multiplier. */
    private static final int[] _witab = {-128, 960, 4384, 18624, 18624, 4384, 960, -128};

    /**
     * Maps G726_24 code words to a set of values whose long and short
     * term averages are computed and then compared to give an indication
     * how stationary (steady state) the signal is.
     */
    private static final int[] _fitab = {0, 0x200, 0x400, 0xE00, 0xE00, 0x400, 0x200, 0};

    private static final int[] qtab_723_24 = {8, 218, 331};

    /**
     * Encodes a linear PCM, A-law or u-law input sample and returns its 3-bit code.
     * @throws IllegalArgumentException if invalid coding value
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

        d = sl - se; // d = estimation diff.

        // quantize prediction difference d
        y = state.step_size(); // quantizer step size
        i = quantize(d, y, qtab_723_24, 3); // i = ADPCM code
        dq = reconstruct(i & 4, _dqlntab[i], y); // quantized diff.

        sr = (dq < 0) ? se - (dq & 0x3FFF) : se + dq; // reconstructed signal

        dqsez = sr + sez - se; // pole prediction diff.

        update(3, y, _witab[i], _fitab[i], dq, sr, dqsez, state);

        return i;
    }

    /**
     * Decodes a 3-bit CCITT G.726 24kbps ADPCM code and returns
     * the resulting 16-bit linear PCM, A-law or u-law sample value.
     * @throws IllegalArgumentException if the coding is unknown
     */
    private int decode(int i, G726State state) {

        int sezi, sei, sez, se; // ACCUM
        int y; // MIX
        int sr; // ADDB
        int dq;
        int dqsez;

        i &= 0x07; // mask to get proper bits
        sezi = state.predictor_zero();
        sez = sezi >> 1;
        sei = sezi + state.predictor_pole();
        se = sei >> 1; // se = estimated signal

        y = state.step_size(); // adaptive quantizer step size
        dq = reconstruct(i & 0x04, _dqlntab[i], y); // unquantize pred diff

        sr = (dq < 0) ? (se - (dq & 0x3FFF)) : (se + dq); // reconst. signal

        dqsez = sr - se + sez; // pole prediction diff.

        update(3, y, _witab[i], _fitab[i], dq, sr, dqsez, state);

        if (encoding.equals(Encoding.ALAW)) {
            return tandem_adjust_alaw(sr, se, y, i, 4, qtab_723_24);
        } else if (encoding.equals(Encoding.ULAW)) {
            return tandem_adjust_ulaw(sr, se, y, i, 4, qtab_723_24);
        } else if (encoding.equals(Encoding.PCM_SIGNED)) {
            return sr << 2; // sr was of 14-bit dynamic range
        } else
            throw new IllegalArgumentException(encoding.toString());
    }

    /** Creates a new G726_24 processor, that can be used to encode from or decode do PCM audio data. */
    public G726_24() {
        super(24000);
    }

    /**
     * Encodes a linear PCM, A-law or u-law input sample and returns its 3-bit code.
     * @throws IllegalArgumentException invalid encoding value
     */
    @Override
    public int encode(int sl) {
        return encode(sl, state);
    }

    /**
     * Decodes a 3-bit CCITT G.726 24kbps ADPCM code and returns
     * the resulting 16-bit linear PCM, A-law or u-law sample value.
     * @throws IllegalArgumentException the encoding is unknown
     */
    @Override
    public int decode(int i) {
        return decode(i, state);
    }

    @Override
    public int getEncodingBits() {
        return 3;
    }
}
