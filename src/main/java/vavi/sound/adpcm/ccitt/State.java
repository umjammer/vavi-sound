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


/**
 * The following is the definition of the state structure
 * used by the G.721/G.723 encoder and decoder to preserve their internal
 * state between successive calls.  The meanings of the majority
 * of the state structure fields are explained in detail in the
 * CCITT Recommendation G.721.  The field names are essentially identical
 * to variable names in the bit level description of the coding algorithm
 * included in this Recommendation.
 *
 * <li>ISDN u-law
 * <li>ISDN A-law
 * <li>PCM 2's-complement (0-center)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030713 nsano initial version <br>
 *          0.01 030714 nsano fine tune <br>
 */
class State {

    /**
     * This routine initializes and/or resets the g72x_state structure
     * pointed to by 'state'.
     * All the initial state values are specified in the CCITT G.721 document.
     */
    State() {
        this.yl = 34816;
        this.yu = 544;
        this.dms = 0;
        this.dml = 0;
        this.ap = 0;
        for (int i = 0; i < 2; i++) {
            this.a[i] = 0;
            this.pk[i] = 0;
            this.sr[i] = 32;
        }
        for (int i = 0; i < 6; i++) {
            this.b[i] = 0;
            this.dq[i] = 32;
        }
        this.td = 0;
    }

    /** Locked or steady state step size multiplier. */
    private long yl;
    /** Unlocked or non-steady state step size multiplier. */
    private int yu;
    /** Short term energy estimate. */
    private int dms;
    /** Long term energy estimate. */
    private int dml;
    /** Linear weighting coefficient of 'yl' and 'yu'. */
    private int ap;

    /** Coefficients of pole portion of prediction filter. */
    private final int[] a = new int[2];
    /** Coefficients of zero portion of prediction filter. */
    private final int[] b = new int[6];
    /**
     * Signs of previous two samples of a partially
     * reconstructed signal.
     */
    private final int[] pk = new int[2];
    /**
     * Previous 6 samples of the quantized difference
     * signal represented in an internal floating point
     * format.
     */
    private final int[] dq = new int[6];
    /**
     * Previous 2 samples of the quantized difference
     * signal represented in an internal floating point
     * format.
     */
    private final int[] sr = new int[2];
    /** delayed tone detect, new in 1988 version */
    private int td;

    //----

    /** */
    private static final int[] power2 = {
        1, 2, 4, 8, 0x10, 0x20, 0x40, 0x80,
        0x100, 0x200, 0x400, 0x800, 0x1000, 0x2000, 0x4000
    };

    /**
     * Quantizes the input val against the table of size short integers.
     * It returns i if table[i - 1] <= val < table[i].
     *
     * Using linear search for simple coding.
     *
     * TODO package level is not good, but ignore because it's capsuled in G711
     */
    static int quan(int val) {
        int i;
        for (i = 0; i < 15; i++) {
            if (val < power2[i]) {
                break;
            }
        }
        return i;
    }

    /**
     * Returns the integer product of the 14-bit integer "an" and
     * "floating point" representation (4-bit exponent, 6-bit mantissa) "srn".
     */
    private static int fmult(int an, int srn) {

        int anmag = (an > 0) ? an : ((-an) & 0x1fff);
        int anexp = quan(anmag) - 6;
        int anmant = (anmag == 0) ? 32 :
                     (anexp >= 0) ? anmag >> anexp :
                                    anmag << -anexp;
        int wanexp = anexp + ((srn >> 6) & 0xf) - 13;

        int wanmant = (anmant * (srn & 0x3f) + 0x30) >> 4;
        int retval = (wanexp >= 0) ? ((wanmant << wanexp) & 0x7fff) :
                                      (wanmant >> -wanexp);

//logger.log(Level.DEBUG, an + ", " + srn + ": " + ((an ^ srn) < 0 ? -retval : retval));
        return (an ^ srn) < 0 ? -retval : retval;
    }

    /**
     * Computes the estimated signal from 6-zero predictor.
     */
    public int getZeroPredictor() {
        int sezi = fmult(b[0] >> 2, dq[0]);
        for (int i = 1; i < 6; i++) { // ACCUM
            sezi += fmult(b[i] >> 2, dq[i]);
        }
        return sezi;
    }

    /**
     * Computes the estimated signal from 2-pole predictor.
     */
    public int getPolePredictor() {
        return fmult(a[1] >> 2, sr[1]) + fmult(a[0] >> 2, sr[0]);
    }

    /**
     * Computes the quantization step size of the adaptive quantizer.
     */
    public int getStepSize() {

        int y;
        int dif;
        int al;

        if (ap >= 256) {
            return yu;
        } else {
            y = (int) (yl >> 6);
            dif = yu - y;
            al = ap >> 2;
            if (dif > 0) {
                y += (dif * al) >> 6;
            } else if (dif < 0) {
                y += (dif * al + 0x3f) >> 6;
            }
            return y;
        }
    }

//private int ccc = 0;

    /**
     * Updates the state variables for each output code.
     *
     * @param code_size distinguish 723_40 with others
     * @param y quantizer step size
     * @param wi scale factor multiplier
     * @param fi for long/short term energies
     * @param _dq quantized prediction difference
     * @param _sr reconstructed signal
     * @param dqsez difference from 2-pole predictor
     */
    public void update(int code_size, int y, int wi, int fi, int _dq, int _sr, int dqsez) {

//logger.log(Level.DEBUG, "y:\t" + y);
//logger.log(Level.DEBUG, "dq:\t" + _dq);
//logger.log(Level.DEBUG, "sr:\t" + _sr);
//logger.log(Level.DEBUG, "dqsez:\t" + dqsez);

        // needed in updating predictor poles
        int pk0 = (dqsez < 0) ? 1 : 0;

        // prediction difference magnitude
        int mag = _dq & 0x7fff;

        // TRANS
        // exponent part of yl
        int ylint = (int) (yl >> 15);
        // fractional part of yl
        int ylfrac = (int) ((yl >> 10) & 0x1f);
        // threshold
        int thr1 = (32 + ylfrac) << ylint;
        // limit thr2 to 31 << 10
        int thr2 = (ylint > 9) ? 31 << 10 : thr1;
        // dqthr = 0.75 * thr2
        int dqthr = (thr2 + (thr2 >> 1)) >> 1;

        int tr;                     // tone/transition detector
        if (td == 0) {              // signal supposed voice
            tr = 0;
        } else if (mag <= dqthr) {  // supposed data, but small mag
            tr = 0;                 // treated as voice
        } else {                    // signal is data (modem)
            tr = 1;
        }

        // Quantizer scale factor adaptation.

        // FUNCTW & FILTD & DELAY
        // update non-steady state step size multiplier
        yu = y + ((wi - y) >> 5);

        // LIMB
        if (yu < 544) { // 544 <= yu <= 5120
            yu = 544;
        } else if (yu > 5120) {
            yu = 5120;
        }

        // FILTE & DELAY
        // update steady state step size multiplier
        yl += yu + ((-yl) >> 6);

        // Adaptive predictor coefficients.
        int a2p = 0;
        int exp;                            // Adaptive predictor, FLOAT A
        if (tr == 1) {                      // reset a's and b's for modem signal
            a[0] = 0;
            a[1] = 0;
            b[0] = 0;
            b[1] = 0;
            b[2] = 0;
            b[3] = 0;
            b[4] = 0;
            b[5] = 0;
        } else {                            // update a's and b's
            int pks1 = pk0 ^ pk[0];         // UPA2

            // update predictor pole a[1]
            a2p = a[1] - (a[1] >> 7);       // LIMC
            if (dqsez != 0) {
                int fa1 = (pks1 != 0) ? a[0] : -a[0];
                if (fa1 < -8191) {          // a2p = function of fa1
                    a2p -= 0x100;
                } else if (fa1 > 8191) {
                    a2p += 0xff;
                } else {
                    a2p += fa1 >> 5;
                }

                if ((pk0 ^ pk[1]) != 0) {   // LIMC
                    if (a2p <= -12160) {
                        a2p = -12288;
                    } else if (a2p >= 12416) {
                        a2p = 12288;
                    } else {
                        a2p -= 0x80;
                    }
                } else if (a2p <= -12416) {
                    a2p = -12288;
                } else if (a2p >= 12160) {
                    a2p = 12288;
                } else {
                    a2p += 0x80;
                }
            }

            // TRIGB & DELAY
            a[1] = a2p;

            // UPA1
            // update predictor pole a[0]
            a[0] -= a[0] >> 8;
            if (dqsez != 0) {
                if (pks1 == 0) {
                    a[0] += 192;
                } else {
                    a[0] -= 192;
                }
            }

            // LIMD
            int a1ul = 15360 - a2p;         // UPA1
            if (a[0] < -a1ul) {
                a[0] = -a1ul;
            } else if (a[0] > a1ul) {
                a[0] = a1ul;
            }

            // UPB : update predictor zeros b[6]
            for (int i = 0; i < 6; i++) {
                if (code_size == 5) {       // for 40Kbps G.723
                    b[i] -= b[i] >> 9;
                } else {                    // for G.721 and 24Kbps G.723
                    b[i] -= b[i] >> 8;
                }
                if ((_dq & 0x7fff) != 0) {  // XOR
                    if ((_dq ^ dq[i]) >= 0) {
                        b[i] += 128;
                    } else {
                        b[i] -= 128;
                    }
                }
            }
        }

        for (int i = 5; i > 0; i--) {
            dq[i] = dq[i-1];
        }
        // FLOAT A : convert dq[0] to 4-bit exp, 6-bit mantissa f.p.
        if (mag == 0) {
            dq[0] = (_dq >= 0) ? 0x20 : -992;
//logger.log(Level.DEBUG, "dq[0]:1: " + dq[0]);
        } else {
            exp = quan(mag);
            dq[0] = (_dq >= 0) ?
                (exp << 6) + ((mag << 6) >> exp) :
                (exp << 6) + ((mag << 6) >> exp) - 0x400;
//logger.log(Level.DEBUG, "dq[0]:2: " + dq[0] + ", " + _dq + ", " + exp + ", " + mag);
//logger.log(Level.DEBUG, "dq[0]:-: " + (exp << 6) + ", " + ((mag << 6) >> exp));
        }

        sr[1] = sr[0];
        // FLOAT B : convert sr to 4-bit exp., 6-bit mantissa f.p.
        if (_sr == 0) {
            sr[0] = 0x20;
//logger.log(Level.DEBUG, "sr[0]:1: " + sr[0]);
        } else if (_sr > 0) {
            exp = quan(_sr);
            sr[0] = (exp << 6) + ((_sr << 6) >> exp);
//logger.log(Level.DEBUG, "sr[0]:2: " + sr[0]);
        } else if (_sr > -32768) {
            mag = -_sr;
            exp = quan(mag);
            sr[0] = (exp << 6) + ((mag << 6) >> exp) - 0x400;
//logger.log(Level.DEBUG, "sr[0]:3: " + sr[0]);
        } else {
            sr[0] = -992;
//logger.log(Level.DEBUG, "sr[0]:4: " + sr[0]);
        }

        // DELAY A
        pk[1] = pk[0];
        pk[0] = pk0;

        // TONE
        if (tr == 1) {              // this sample has been treated as data
            td = 0;                 // next one will be treated as voice
        } else if (a2p < -11776) {    // small sample-to-sample correlation
            td = 1;                 // signal may be data
        } else {                    // signal is voice
            td = 0;
        }

        // Adaptation speed control.
        dms += (fi - dms) >> 5;             // FILTA
        dml += (((fi << 2) - dml) >> 7);    // FILTB

        if (tr == 1) {
            ap = 256;
        } else if (y < 1536) {              // SUBTC
            ap += (0x200 - ap) >> 4;
        } else if (td == 1) {
            ap += (0x200 - ap) >> 4;
        } else if (Math.abs((dms << 2) - dml) >= (dml >> 3)) {
            ap += (0x200 - ap) >> 4;
        } else {
            ap += (-ap) >> 4;
        }
    }
}
