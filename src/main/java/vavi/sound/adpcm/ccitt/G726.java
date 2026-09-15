/*
 * Acknowledgement to Sun Microsystems, Inc. for having released the original
 * ANSI-C source code to the public domain.
 */

package vavi.sound.adpcm.ccitt;


/**
 * Common routines for G.721 and G.723 conversions.
 * <p>
 * This implementation is based on the ANSI-C language reference implementations
 * of the CCITT (International Telegraph and Telephone Consultative Committee)
 * G.711, G.721 and G.723 voice compressions, provided by Sun Microsystems, Inc.
 * <p>
 * Acknowledgement to Sun Microsystems, Inc. for having released the original
 * ANSI-C source code to the public domain.
 */
abstract class G726 extends G711 {

    /** The first 15 values, powers of 2. */
    private static final int[] power2 = {
            1, 2, 4, 8, 0x10, 0x20, 0x40, 0x80, 0x100, 0x200, 0x400, 0x800, 0x1000, 0x2000, 0x4000
    };

    /**
     * Quantizes the input val against the table of size short integers.
     * It returns i if table[i-1]<=val<table[i].
     * <p>
     * Using linear search for simple coding.
     */
    private static int quan(int val, int[] table, int size) {

        int i;
        for (i = 0; i < size; i++) if (val < table[i]) break;
        return i;
    }

    /**
     * Returns reconstructed difference signal 'dq' obtained from
     * codeword 'i' and quantization step size scale factor 'y'.
     * Multiplication is performed in log base 2 domain as addition.
     *
     * @param sign 0 for non-negative value
     * @param dqln G.72x codeword
     * @param y    Step size multiplier
     */
    protected static int reconstruct(int sign, int dqln, int y) {

        // Log of 'dq' magnitude
        /*short*/
        int dql = dqln + (y >> 2); // ADDA

        if (dql < 0) {
            return (sign != 0) ? -0x8000 : 0;
        } else {
            // ANTILOG
            // Integer part of log
            int dex = (dql >> 7) & 15;
            int dqt = 128 + (dql & 127);
            // Reconstructed difference signal sample
            int dq = (dqt << 7) >> (14 - dex);
            return (sign != 0) ? (dq - 0x8000) : dq;
        }
    }

    /**
     * updates the state variables for each output code
     *
     * @param code_size distinguish 723_40 with others
     * @param y         quantizer step size
     * @param wi        scale factor multiplier
     * @param fi        for long/short term energies
     * @param dq        quantized prediction difference
     * @param sr        reconstructed signal
     * @param dqsez     difference from 2-pole predictor
     * @param state     coder state
     */
    protected static void update(int code_size, int y, int wi, int fi, int dq, int sr, int dqsez, G726State state) {

        int cnt;
        int mag, exp, mant; // Adaptive predictor, FLOAT A
        int a2p; // LIMC
        int a1ul; // UPA1
        int ua2, pks1; // UPA2
        int uga2a, fa1;
        int uga2b;
        int tr; // tone/transition detector
        int ylint, thr2, dqthr;
        int ylfrac, thr1;
        int pk0;

        a2p = 0;

        pk0 = (dqsez < 0) ? 1 : 0; // needed in updating predictor poles

        mag = dq & 0x7FFF; // prediction difference magnitude
        // TRANS
        ylint = state.yl >> 15; // exponent part of yl
        ylfrac = (state.yl >> 10) & 0x1F; // fractional part of yl
        thr1 = (32 + ylfrac) << ylint; // threshold
        thr2 = (ylint > 9) ? 31 << 10 : thr1; // limit thr2 to 31<<10
        dqthr = (thr2 + (thr2 >> 1)) >> 1; // dqthr=0.75 * thr2
        if (state.td == 0) // signal supposed voice
            tr = 0;
        else if (mag <= dqthr) // supposed data, but small mag
            tr = 0; // treated as voice
        else // signal is data (modem)
            tr = 1;

        // Quantizer scale factor adaptation.

        // FUNCTW & FILTD & DELAY
        // update non-steady state step size multiplier
        state.yu = y + ((wi - y) >> 5);

        // LIMB
        if (state.yu < 544) // 544 <= yu <= 5120
            state.yu = 544;
        else if (state.yu > 5120)
            state.yu = 5120;

        // FILTE & DELAY
        // update steady state step size multiplier
        state.yl += state.yu + ((-state.yl) >> 6);

        //
        // Adaptive predictor coefficients.
        //
        if (tr == 1) {
            // reset a's and b's for modem signal
            state.a[0] = 0;
            state.a[1] = 0;
            state.b[0] = 0;
            state.b[1] = 0;
            state.b[2] = 0;
            state.b[3] = 0;
            state.b[4] = 0;
            state.b[5] = 0;
        } else {
            // update a's and b's
            pks1 = pk0 ^ state.pk[0]; // UPA2

            // update predictor pole a[1]
            a2p = state.a[1] - (state.a[1] >> 7);
            if (dqsez != 0) {
                fa1 = (pks1 != 0) ? state.a[0] : -state.a[0];
                if (fa1 < -8191) // a2p=function of fa1
                    a2p -= 0x100;
                else if (fa1 > 8191)
                    a2p += 0xFF;
                else
                    a2p += fa1 >> 5;

                if ((pk0 ^ state.pk[1]) != 0) {
                    // LIMC
                    if (a2p <= -12160)
                        a2p = -12288;
                    else if (a2p >= 12416)
                        a2p = 12288;
                    else
                        a2p -= 0x80;
                } else if (a2p <= -12416)
                    a2p = -12288;
                else if (a2p >= 12160)
                    a2p = 12288;
                else
                    a2p += 0x80;
            }

            // TRIGB & DELAY
            state.a[1] = a2p;

            // UPA1
            // update predictor pole a[0]
            state.a[0] -= state.a[0] >> 8;
            if (dqsez != 0)
                if (pks1 == 0)
                    state.a[0] += 192;
                else
                    state.a[0] -= 192;

            // LIMD
            a1ul = 15360 - a2p;
            if (state.a[0] < -a1ul)
                state.a[0] = -a1ul;
            else if (state.a[0] > a1ul)
                state.a[0] = a1ul;

            // UPB : update predictor zeros b[6]
            for (cnt = 0; cnt < 6; cnt++) {

                if (code_size == 5) // for 40Kbps G.723
                    state.b[cnt] -= state.b[cnt] >> 9;
                else // for G.721 and 24Kbps G.723
                    state.b[cnt] -= state.b[cnt] >> 8;
                if ((dq & 0x7FFF) != 0) {
                    // XOR
                    if ((dq ^ state.dq[cnt]) >= 0)
                        state.b[cnt] += 128;
                    else
                        state.b[cnt] -= 128;
                }
            }
        }

        for (cnt = 5; cnt > 0; cnt--) state.dq[cnt] = state.dq[cnt - 1];
        // FLOAT A : convert dq[0] to 4-bit exp, 6-bit mantissa f.p.
        if (mag == 0) {
            state.dq[0] = (dq >= 0) ? 0x20 : (short) 0xFC20; // -992, C short
        } else {
            exp = quan(mag, power2, 15);
            state.dq[0] = (dq >= 0) ? (exp << 6) + ((mag << 6) >> exp) : (exp << 6) + ((mag << 6) >> exp) - 0x400;
        }

        state.sr[1] = state.sr[0];
        // FLOAT B : convert sr to 4-bit exp., 6-bit mantissa f.p.
        if (sr == 0) {
            state.sr[0] = 0x20;
        } else if (sr > 0) {
            exp = quan(sr, power2, 15);
            state.sr[0] = (exp << 6) + ((sr << 6) >> exp);
        } else if (sr > -32768) {
            mag = -sr;
            exp = quan(mag, power2, 15);
            state.sr[0] = (exp << 6) + ((mag << 6) >> exp) - 0x400;
        } else
            state.sr[0] = (short) 0xFC20; // -992, C short

        // DELAY A
        state.pk[1] = state.pk[0];
        state.pk[0] = pk0;

        // TONE
        if (tr == 1) // this sample has been treated as data
            state.td = 0; // next one will be treated as voice
        else if (a2p < -11776) // small sample-to-sample correlation
            state.td = 1; // signal may be data
        else // signal is voice
            state.td = 0;

        /*
         * Adaptation speed control.
         */
        state.dms += (fi - state.dms) >> 5; // FILTA
        state.dml += (((fi << 2) - state.dml) >> 7); // FILTB

        if (tr == 1)
            state.ap = 256;
        else if (y < 1536) // SUBTC
            state.ap += (0x200 - state.ap) >> 4;
        else if (state.td == 1)
            state.ap += (0x200 - state.ap) >> 4;
        else if (Math.abs((state.dms << 2) - state.dml) >= (state.dml >> 3))
            state.ap += (0x200 - state.ap) >> 4;
        else
            state.ap += (-state.ap) >> 4;
    }

    /**
     * At the end of ADPCM decoding, it simulates an encoder which may be receiving
     * the output of this decoder as a tandem process. If the output of the
     * simulated encoder differs from the input to this decoder, the decoder output
     * is adjusted by one level of A-law or u-law codes.
     *
     * @param sr   decoder output linear PCM sample,
     * @param se   predictor estimate sample,
     * @param y    quantizer step size,
     * @param i    decoder input code,
     * @param sign sign bit of code i
     * @return adjusted A-law or u-law compressed sample.
     */
    protected int tandem_adjust_alaw(int sr, int se, int y, int i, int sign, int[] qtab) {

        int sp; // A-law compressed 8-bit code
        int dx; // prediction error
        int id; // quantized prediction error
        int sd; // adjusted A-law decoded sample value
        int im; // biased magnitude of i
        int imx; // biased magnitude of id

        if (sr <= -32768) sr = -1;
        sp = linear2alaw((short) ((sr >> 1) << 3)); // short to A-law compression
        dx = (alaw2linear((byte) sp) >> 2) - se; // 16-bit prediction error
        id = quantize(dx, y, qtab, sign - 1);

        if (id == i) {
            // no adjustment on sp
            return sp;
        } else {
            // sp adjustment needed
            // ADPCM codes : 8, 9, ... F, 0, 1, ... , 6, 7
            im = i ^ sign; // 2's complement to biased unsigned
            imx = id ^ sign;

            if (imx > im) {
                // sp adjusted to next lower value
                if ((sp & 0x80) != 0) {
                    sd = (sp == 0xD5) ? 0x55 : ((sp ^ 0x55) - 1) ^ 0x55;
                } else {
                    sd = (sp == 0x2A) ? 0x2A : ((sp ^ 0x55) + 1) ^ 0x55;
                }
            } else {
                // sp adjusted to next higher value
                if ((sp & 0x80) != 0)
                    sd = (sp == 0xAA) ? 0xAA : ((sp ^ 0x55) + 1) ^ 0x55;
                else
                    sd = (sp == 0x55) ? 0xD5 : ((sp ^ 0x55) - 1) ^ 0x55;
            }
            return sd;
        }
    }

    /**
     * @param sr   decoder output linear PCM sample
     * @param se   predictor estimate sample
     * @param y    quantizer step size
     * @param i    decoder input code
     * @param sign
     * @param qtab
     */
    protected int tandem_adjust_ulaw(int sr, int se, int y, int i, int sign, int[] qtab) {

        int sp; // u-law compressed 8-bit code
        int dx; // prediction error
        int id; // quantized prediction error
        int sd; // adjusted u-law decoded sample value
        int im; // biased magnitude of i
        int imx; // biased magnitude of id

        if (sr <= -32768) sr = 0;
        sp = linear2ulaw((short) (sr << 2)); // short to u-law compression
        dx = ulaw2linear((byte) sp) >> 2 - se; // 16-bit prediction error
        id = quantize(dx, y, qtab, sign - 1);
        if (id == i) {
            return sp;
        } else {
            // ADPCM codes : 8, 9, ... F, 0, 1, ... , 6, 7
            im = i ^ sign; // 2's complement to biased unsigned
            imx = id ^ sign;
            if (imx > im) {
                // sp adjusted to next lower value
                if ((sp & 0x80) != 0)
                    sd = (sp == 0xFF) ? 0x7E : sp + 1;
                else
                    sd = (sp == 0) ? 0 : sp - 1;

            } else {
                // sp adjusted to next higher value
                if ((sp & 0x80) != 0)
                    sd = (sp == 0x80) ? 0x80 : sp - 1;
                else
                    sd = (sp == 0x7F) ? 0xFE : sp + 1;
            }
            return sd;
        }
    }

    /** Converts a byte into an unsigned int. */
    protected static int unsignedInt(byte b) {
        return ((int) b + 0x100) & 0xFF;
    }

    /** Converts 2 little-endian-bytes into an unsigned int. */
    static int unsignedIntLittleEndian(byte hi_b, byte lo_b) {
        return (unsignedInt(hi_b) << 8) + unsignedInt(lo_b);
    }

    /** Converts 2 little-endian-bytes into a signed int. */
    static int signedIntLittleEndian(byte hi_b, byte lo_b) {
        int sign_bit = hi_b >> 7;
        return sign_bit == 0 ? (unsignedInt(hi_b) << 8) + unsignedInt(lo_b) : (-1 ^ 0x7FFF) ^ (((unsignedInt(hi_b) & 0x7F) << 8) + unsignedInt(lo_b));
    }

    /** Encoding state */
    G726State state;

    int type;

    /** Creates a new G726 processor, that can be used to encode from or decode do PCM audio data. */
    public G726(int type) {
        this.type = type;
        state = new G726State();
    }

    public int getType() {
        return type;
    }
}
