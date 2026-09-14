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


class G726State {

    /** Locked or steady state step size multiplier. */
    int yl;
    /** Unlocked or non-steady state step size multiplier. */
    int yu;
    /** Short term energy estimate. */
    int dms;
    /** Long term energy estimate. */
    int dml;
    /** Linear weighting coefficient of 'yl' and 'yu'. */
    int ap;

    /** Coefficients of pole portion of prediction filter. */
    int[] a;
    /** Coefficients of zero portion of prediction filter. */
    int[] b;
    /**
     * Signs of previous two samples of a partially
     * reconstructed signal.
     */
    int[] pk;
    /**
     * Previous 6 samples of the quantized difference
     * signal represented in an internal floating point
     * format.
     */
    int[] dq;
    /**
     * Previous 2 samples of the quantized difference
     * signal represented in an internal floating point
     * format.
     */
    int[] sr;
    /** delayed tone detect, new in 1988 version */
    int td;

    /** The first 15 values, powers of 2. */
    private static final int[] power2 = {
            1, 2, 4, 8, 0x10, 0x20, 0x40, 0x80, 0x100, 0x200, 0x400, 0x800, 0x1000, 0x2000, 0x4000
    };

    /**
     * Quantizes the input val against the table of size short integers.
     * It returns i if {@code table[i-1] <= val < table[i]}.
     * <p>
     * Using linear search for simple coding.
     */
    private static int quan(int val, int[] table, int size) {

        int i;
        for (i = 0; i < size; i++) if (val < table[i]) break;
        return i;
    }

    /**
     * returns the integer product of the 14-bit integer "an" and
     * "floating point" representation (4-bit exponent, 6-bit mantessa) "srn".
     */
    private static int fmult(int an, int srn) {

        int anmag = (an > 0) ? an : ((-an) & 0x1FFF);
        int anexp = quan(anmag, power2, 15) - 6;
        int anmant = (anmag == 0) ? 32 : (anexp >= 0) ? anmag >> anexp : anmag << -anexp;
        int wanexp = anexp + ((srn >> 6) & 0xF) - 13;

        int wanmant = (anmant * (srn & 077) + 0x30) >> 4;
        int retval = (wanexp >= 0) ? ((wanmant << wanexp) & 0x7FFF) : (wanmant >> -wanexp);

        return ((an ^ srn) < 0) ? -retval : retval;
    }

    /** Creates a new G726State. */
    public G726State() {
        a = new int[2];
        b = new int[6];
        pk = new int[2];
        dq = new int[6];
        sr = new int[2];
        init();
    }

    /**
     * This routine initializes and/or resets the G726State 'state'. <br>
     * All the initial state values are specified in the CCITT G.721 document.
     */
    private void init() {
        yl = 34816;
        yu = 544;
        dms = 0;
        dml = 0;
        ap = 0;
        for (int cnta = 0; cnta < 2; cnta++) {
            a[cnta] = 0;
            pk[cnta] = 0;
            sr[cnta] = 32;
        }
        for (int cnta = 0; cnta < 6; cnta++) {
            b[cnta] = 0;
            dq[cnta] = 32;
        }
        td = 0;
    }

    /** computes the estimated signal from 6-zero predictor. */
    public int predictor_zero() {

        int sezi = fmult(b[0] >> 2, dq[0]);
        // ACCUM
        for (int i = 1; i < 6; i++) sezi += fmult(b[i] >> 2, dq[i]);
        return sezi;
    }

    /** computes the estimated signal from 2-pole predictor. */
    public int predictor_pole() {

        return fmult(a[1] >> 2, sr[1]) + fmult(a[0] >> 2, sr[0]);
    }

    /** computes the quantization step size of the adaptive quantizer. */
    public int step_size() {

        if (ap >= 256) return yu;
        else {
            int y = yl >> 6;
            int dif = yu - y;
            int al = ap >> 2;
            if (dif > 0) y += (dif * al) >> 6;
            else if (dif < 0) y += (dif * al + 0x3F) >> 6;
            return y;
        }
    }
}
