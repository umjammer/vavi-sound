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

import vavi.sound.adpcm.Codec;


/**
 * u-law, A-law and linear PCM conversions.
 *
 * @author <a href="mailto:bli@cpk.auc.dk">Borge Lindberg, Center for
 *         PersonKommunikation, Aalborg University.</a>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 941230 bli Functions linear2alaw, linear2ulaw have been updated
 *                      to correctly convert unquantized 16 bit values. <br>
 *                      Tables for direct u- to A-law and A- to u-law conversions have been
 *                      corrected. <br>
 *          0.10 030713 nsano port to java <br>
 *          0.11 030714 nsano fine tune <br>
 */
abstract class G711 implements Codec {

    /** */
    protected final State state = new State();

    /** */
    protected AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;

    /** Sign bit for a A-law byte. */
    private static final int SIGN_BIT = 0x80;
    /** Quantization field mask. */
    private static final int QUANT_MASK = 0xf;
//    /** Number of A-law segments. */
//    private static final int NSEGS = 8;
    /** Left shift for segment number. */
    private static final int SEG_SHIFT = 4;
    /** Segment field mask. */
    private static final int SEG_MASK = 0x70;

    /** */
    private static final int[] seg_aend = {
        0x1f, 0x3f, 0x7f, 0xff,
        0x1ff, 0x3ff, 0x7ff, 0xfff
    };

    /** */
    private static final int[] seg_uend = {
        0x3f, 0x7f, 0xff, 0x1ff,
        0x3ff, 0x7ff, 0xfff, 0x1fff
    };

    /* copy from CCITT G.711 specifications */

    /** u- to A-law conversions */
    private static final int[] _u2a = {
          1,   1,   2,   2,   3,   3,   4,   4,
          5,   5,   6,   6,   7,   7,   8,   8,
          9,  10,  11,  12,  13,  14,  15,  16,
         17,  18,  19,  20,  21,  22,  23,  24,
         25,  27,  29,  31,  33,  34,  35,  36,
         37,  38,  39,  40,  41,  42,  43,  44,
         46,  48,  49,  50,  51,  52,  53,  54,
         55,  56,  57,  58,  59,  60,  61,  62,
         64,  65,  66,  67,  68,  69,  70,  71,
         72,  73,  74,  75,  76,  77,  78,  79,
/* corrected:
         81,  82,  83,  84,  85,  86,  87,  88,
  should be: */
         80,  82,  83,  84,  85,  86,  87,  88,
         89,  90,  91,  92,  93,  94,  95,  96,
         97,  98,  99, 100, 101, 102, 103, 104,
        105, 106, 107, 108, 109, 110, 111, 112,
        113, 114, 115, 116, 117, 118, 119, 120,
        121, 122, 123, 124, 125, 126, 127, 128
    };

    /** A- to u-law conversions */
    private static final int[] _a2u = {
          1,   3,   5,   7,   9,  11,  13,  15,
         16,  17,  18,  19,  20,  21,  22,  23,
         24,  25,  26,  27,  28,  29,  30,  31,
         32,  32,  33,  33,  34,  34,  35,  35,
         36,  37,  38,  39,  40,  41,  42,  43,
         44,  45,  46,  47,  48,  48,  49,  49,
         50,  51,  52,  53,  54,  55,  56,  57,
         58,  59,  60,  61,  62,  63,  64,  64,
         65,  66,  67,  68,  69,  70,  71,  72,
/* corrected:
         73,  74,  75,  76,  77,  78,  79,  79,
   should be: */
         73,  74,  75,  76,  77,  78,  79,  80,

         80,  81,  82,  83,  84,  85,  86,  87,
         88,  89,  90,  91,  92,  93,  94,  95,
         96,  97,  98,  99, 100, 101, 102, 103,
        104, 105, 106, 107, 108, 109, 110, 111,
        112, 113, 114, 115, 116, 117, 118, 119,
        120, 121, 122, 123, 124, 125, 126, 127
    };

    /** */
    private static int search(int val, int[] table, int size) {

        for (int i = 0; i < size; i++) {
            if (val <= table[i]) {
                return i;
            }
        }
        return size;
    }

    /**
     * Convert a 16-bit linear PCM value to 8-bit A-law.
     *
     * accepts an 16-bit integer and encodes it as A-law data.
     * <pre>
     *        Linear Input Code        Compressed Code
     *    ------------------------    ---------------
     *        0000000wxyza              000wxyz
     *        0000001wxyza              001wxyz
     *        000001wxyzab              010wxyz
     *        00001wxyzabc              011wxyz
     *        0001wxyzabcd              100wxyz
     *        001wxyzabcde              101wxyz
     *        01wxyzabcdef              110wxyz
     *        1wxyzabcdefg              111wxyz
     * </pre>
     * For further information see John C. Bellamy's Digital Telephony, 1982,
     * John Wiley & Sons, pps 98-111 and 472-476.
     *
     * @param pcm_val 2's complement (16-bit range)
     */
    protected int linear2alaw(int pcm_val) {

        int mask;
        int aval;

        pcm_val = pcm_val >> 3;

        if (pcm_val >= 0) {
            mask = 0xd5; // sign (7th) bit = 1
        } else {
            mask = 0x55; // sign bit = 0
            pcm_val = -pcm_val - 1;
        }

        // Convert the scaled magnitude to segment number.
        int seg = search(pcm_val, seg_aend, 8);

        // Combine the sign, segment, and quantization bits.
        if (seg >= 8) { // out of range, return maximum value.
            return 0x7f ^ mask;
        } else {
            aval = seg << SEG_SHIFT;
            if (seg < 2) {
                aval |= (pcm_val >> 1) & QUANT_MASK;
            } else {
                aval |= (pcm_val >> seg) & QUANT_MASK;
            }
            return aval ^ mask;
        }
    }

    /**
     * Convert an A-law value to 16-bit linear PCM.
     */
    protected int alaw2linear(int a_val) {

        a_val ^= 0x55;

        int t = (a_val & QUANT_MASK) << 4;
        int seg = (a_val & SEG_MASK) >> SEG_SHIFT;
        switch (seg) {
        case 0:
            t += 8;
            break;
        case 1:
            t += 0x108;
            break;
        default:
            t += 0x108;
            t <<= seg - 1;
        }

        return (a_val & SIGN_BIT) != 0 ? t : -t;
    }

    /** Bias for linear code. */
    private static final int BIAS = 0x84;
    /** */
    private static final int CLIP = 8159;

    /**
     * Convert a linear PCM value to u-law.
     *
     * In order to simplify the encoding process, the original linear magnitude
     * is biased by adding 33 which shifts the encoding range from (0 - 8158)
     * to (33 - 8191). The result can be seen in the following encoding table:
     * <pre>
     *    Biased Linear Input Code    Compressed Code
     *    ------------------------    ---------------
     *       00000001wxyza              000wxyz
     *       0000001wxyzab              001wxyz
     *       000001wxyzabc              010wxyz
     *       00001wxyzabcd              011wxyz
     *       0001wxyzabcde              100wxyz
     *       001wxyzabcdef              101wxyz
     *       01wxyzabcdefg              110wxyz
     *       1wxyzabcdefgh              111wxyz
     * </pre>
     * Each biased linear code has a leading 1 which identifies the segment
     * number. The value of the segment number is equal to 7 minus the number
     * of leading 0's. The quantization interval is directly available as the
     * four bits wxyz. The trailing bits (a - h) are ignored.
     *
     * Ordinarily the complement of the resulting code word is used for
     * transmission, and so the code word is complemented before it is
     * returned.
     *
     * For further information see John C. Bellamy's Digital Telephony, 1982,
     * John Wiley & Sons, pps 98-111 and 472-476.
     *
     * @param pcm_val 2's complement (16-bit range)
     */
    protected int linear2ulaw(int pcm_val) {

        int mask;
        int uval;

        // Get the sign and the magnitude of the value.
        pcm_val = pcm_val >> 2;
        if (pcm_val < 0) {
            pcm_val = -pcm_val;
            mask = 0x7f;
        } else {
            mask = 0xff;
        }
        if (pcm_val > CLIP) {
            pcm_val = CLIP; // clip the magnitude
        }
        pcm_val += (BIAS >> 2);

        // Convert the scaled magnitude to segment number.
        int seg = search(pcm_val, seg_uend, 8);

        // Combine the sign, segment, quantization bits;
        // and complement the code word.
        if (seg >= 8) { // out of range, return maximum value.
            return 0x7f ^ mask;
        } else {
            uval = (seg << 4) | ((pcm_val >> (seg + 1)) & 0xf);
            return uval ^ mask;
        }
    }

    /**
     * Convert a u-law value to 16-bit linear PCM.
     *
     * First, a biased linear code is derived from the code word. An unbiased
     * output can then be obtained by subtracting 33 from the biased code.
     *
     * Note that this function expects to be passed the complement of the
     * original code word. This is in keeping with ISDN conventions.
     */
    protected int ulaw2linear(int u_val) {

        // Complement to obtain normal u-law value.
        u_val = ~u_val;

        // Extract and bias the quantization bits. Then
        // shift up by the segment number and subtract out the bias.
        int t = ((u_val & QUANT_MASK) << 3) + BIAS;
        t <<= (u_val & SEG_MASK) >> SEG_SHIFT;

        return (u_val & SIGN_BIT) != 0 ? BIAS - t : t - BIAS;
    }

    /** A-law to u-law conversion */
    protected int alaw2ulaw(int aval) {
        return (aval & 0x80) != 0 ? 0xff ^ _a2u[aval ^ 0xd5] :
                                    0x7f ^ _a2u[aval ^ 0x55];
    }

    /** u-law to A-law conversion */
    protected int ulaw2alaw(int uval) {
        return (uval & 0x80) != 0 ? 0xd5 ^ (_u2a[0xff ^ uval] - 1) :
                                    0x55 ^ (_u2a[0x7f ^ uval] - 1);
    }

    // ----

    /**
     * Quantizes the input val against the table of size short integers.
     * It returns i if table[i - 1] <= val < table[i].
     *
     * Using linear search for simple coding.
     */
    private static int quan(int val, int[] table, int size) {
        int i;
        for (i = 0; i < size; i++) {
            if (val < table[i]) {
                break;
            }
        }
        return i;
    }

    /**
     * Returns reconstructed difference signal 'dq' obtained from
     * codeword 'i' and quantization step size scale factor 'y'.
     * Multiplication is performed in log base 2 domain as addition.
     *
     * @param sign 0 for non-negative value
     * @param dqln G.72x codeword
     * @param y Step size multiplier
     */
    protected int reconstruct(boolean sign, int dqln, int y) {

        // Log of 'dq' magnitude
        int dql = dqln + (y >> 2);      // ADDA

        if (dql < 0) {
// logger.log(Level.DEBUG, "1: " + (sign ? -0x8000 : 0));
            return sign ? -0x8000 : 0;
        } else {                        // ANTILOG
            // Integer part of log
            int dex = (dql >> 7) & 15;
            int dqt = 128 + (dql & 127);
            // Reconstructed difference signal sample
            int dq = (dqt << 7) >> (14 - dex);
// logger.log(Level.DEBUG, "2: " + sign + ", " + dqln + ", " + y);
// logger.log(Level.DEBUG, "-: " + (sign ? dq - 0x8000 : dq) + ", " + (dq - 0x8000) + ", " + dq);
            return sign ? dq - 0x8000 : dq;
        }
    }

    /**
     * Given a raw sample, 'd', of the difference signal and a
     * quantization step size scale factor, 'y', this routine returns the
     * ADPCM codeword to which that sample gets quantized.  The step
     * size scale factor division operation is done in the log base 2 domain
     * as a subtraction.
     *
     * @param d Raw difference signal sample
     * @param y Step size multiplier
     * @param table quantization table
     * @param size table size of short integers
     */
    protected int quantize(int d, int y, int[] table, int size) {

        // LOG
        //
        // Compute base 2 log of 'd', and store in 'dl'.
        //
        // Magnitude of 'd'
        int dqm = Math.abs(d);
// logger.log(Level.DEBUG, "d:\t" + d);
// logger.log(Level.DEBUG, "dqm:\t" + dqm);
        // Integer part of base 2 log of 'd'
        int exp = State.quan(dqm >> 1);
// logger.log(Level.DEBUG, "exp:\t" + exp);
        // Fractional part of base 2 log
        int mant = ((dqm << 7) >> exp) & 0x7f; // Fractional portion.
        // Log of magnitude of 'd'
        int dl = (exp << 7) + mant;

        // SUBTB
        //
        // "Divide" by step size multiplier.
        //
        // Step size scale factor normalized log
        int dln = dl - (y >> 2);

        // QUAN
        //
        // Obtain codeword i for 'd'.
        //
        int i = quan(dln, table, size);
        if (d < 0) { // take 1's complement of i
            return (size << 1) + 1 - i;
        } else if (i == 0) { // take 1's complement of 0
            return (size << 1) + 1; // new in 1988
        } else {
            return i;
        }
    }

    /**
     * At the end of ADPCM decoding, it simulates an encoder which may be
     * receiving the output of this decoder as a tandem process. If the output
     * of the simulated encoder differs from the input to this decoder, the
     * decoder output is adjusted by one level of A-law or u-law codes.
     *
     * @param sr decoder output linear PCM sample,
     * @param se predictor estimate sample,
     * @param y quantizer step size,
     * @param i decoder input code,
     * @param sign sign bit of code i
     *
     * @return adjusted A-law or u-law compressed sample.
     */
    protected int adjustAlawTandem(int sr, int se, int y, int i, int sign, int[] qtab) {

        if (sr <= -32768) {
            sr = -1;
        }

        // short to A-law compressed 8-bit code
        int sp = linear2alaw((sr >> 1) << 3);
        // 16-bit prediction error
        int dx = (alaw2linear(sp) >> 2) - se;
        // quantized prediction error
        int id = quantize(dx, y, qtab, sign - 1);

        if (id == i) {              // no adjustment on sp
            return sp;
        } else {                    // sp adjustment needed
            // ADPCM codes : 8, 9, ... F, 0, 1, ... , 6, 7
            int sd;                 // adjusted A-law decoded sample value
            int im = i ^ sign;      // 2's complement to biased unsigned
            int imx = id ^ sign;    // biased magnitude of id
            if (imx > im) {         // sp adjusted to next lower value
                if ((sp & 0x80) != 0) {
                    sd = (sp == 0xd5) ? 0x55 : ((sp ^ 0x55) - 1) ^ 0x55;
                } else {
                    sd = (sp == 0x2a) ? 0x2a : ((sp ^ 0x55) + 1) ^ 0x55;
                }
            } else {                // sp adjusted to next higher value
                if ((sp & 0x80) != 0) {
                    sd = (sp == 0xaa) ? 0xaa : ((sp ^ 0x55) + 1) ^ 0x55;
                } else {
                    sd = (sp == 0x55) ? 0xd5 : ((sp ^ 0x55) - 1) ^ 0x55;
                }
            }
            return sd;
        }
    }

    /**
     *
     * @param sr decoder output linear PCM sample,
     * @param se predictor estimate sample,
     * @param y quantizer step size,
     * @param i decoder input code,
     * @param sign sign bit of code i
     *
     * @return adjusted A-law or u-law compressed sample.
     */
    protected int adjustUlawTandem(int sr, int se, int y, int i, int sign, int[] qtab) {

        if (sr <= -32768) {
            sr = 0;
        }

        // u-law compressed 8-bit code
        int sp = linear2ulaw(sr << 2);
        // 16-bit prediction error
        int dx = (ulaw2linear(sp) >> 2) - se;
        // quantized prediction error
        int id = quantize(dx, y, qtab, sign - 1);

        if (id == i) {
            return sp;
        } else {
            // ADPCM codes : 8, 9, ... F, 0, 1, ... , 6, 7
            int sd;                 // adjusted u-law decoded sample value
            int im = i ^ sign;      // 2's complement to biased unsigned
            int imx = id ^ sign;    // biased magnitude of id
            if (imx > im) {         // sp adjusted to next lower value
                if ((sp & 0x80) != 0) {
                    sd = (sp == 0xff) ? 0x7e : sp + 1;
                } else {
                    sd = (sp == 0) ? 0 : sp - 1;
                }
            } else {                // sp adjusted to next higher value
                if ((sp & 0x80) != 0) {
                    sd = (sp == 0x80) ? 0x80 : sp - 1;
                } else {
                    sd = (sp == 0x7f) ? 0xfe : sp + 1;
                }
            }
            return sd;
        }
    }

    // ----

    /** */
    public abstract int getEncodingBits();

    /** */
    public void setEncoding(AudioFormat.Encoding encoding) {
        this.encoding = encoding;
    }
}
