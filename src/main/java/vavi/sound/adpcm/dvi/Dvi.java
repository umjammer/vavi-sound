/*
 * Copyright 1992 by Stichting Mathematisch Centrum, Amsterdam, The
 * Netherlands.
 *
 *                      All Rights Reserved
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted,
 * provided that the above copyright notice appear in all copies and that
 * both that copyright notice and this permission notice appear in
 * supporting documentation, and that the names of Stichting Mathematisch
 * Centrum or CWI not be used in advertising or publicity pertaining to
 * distribution of the software without specific, written prior permission.
 *
 * STICHTING MATHEMATISCH CENTRUM DISCLAIMS ALL WARRANTIES WITH REGARD TO
 * THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS, IN NO EVENT SHALL STICHTING MATHEMATISCH CENTRUM BE LIABLE
 * FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
 * OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package vavi.sound.adpcm.dvi;

import vavi.sound.adpcm.Codec;


/**
 * Intel/DVI ADPCM coder/decoder.
 *
 * The algorithm for this coder was taken from the IMA Compatibility Project
 * proceedings, Vol 2, Number 2; May 1992.
 *
 * @version    1.20    921218
 *
 * Change log:
 * - Fixed a stupid bug, where the delta was computed as
 *   stepsize*code/4 in stead of stepsize*(code+0.5)/4.
 * - There was an off-by-one error causing it to pick
 *   an incorrect delta once in a blue moon.
 * - The NODIVMUL define has been removed. Computations are now always done
 *   using shifts, adds and subtracts. It turned out that, because the standard
 *   is defined using shift/add/subtract, you needed bits of fixup code
 *   (because the div/mul simulation using shift/add/sub made some rounding
 *   errors that real div/mul don't make) and all together the resultant code
 *   ran slower than just using the shifts all the time.
 * - Changed some of the variable names to be more meaningful.
 *
 * @author Stichting Mathematisch Centrum
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020707 nsano initial version <br>
 */
class Dvi implements Codec {

    /** Intel ADPCM step variation table */
    private static final int indexTable[] = {
        -1, -1, -1, -1, 2, 4, 6, 8,
        -1, -1, -1, -1, 2, 4, 6, 8,
    };

    /** */
    private static final int stepSizeTable[] = {
        7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
        19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
        50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
        130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
        337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
        876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
        2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
        5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
        15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
    };

    /** */
    private class State {
        /** */
        int valPrev;
        /** */
        int index;
    }

    /** */
    private State state = new State();

    /** */
    public State getState() {
        return state;
    }

    /**
     * @param input pcm
     * @return adpcm
     */
    public int encode(int input) {

        //
        int valPrev = state.valPrev;
        // Current step change index
        int index = state.index;

        // Stepsize
        int step = stepSizeTable[index];

        // Step 1 - compute difference with previous value
        int diff = input - valPrev;
        // Current adpcm sign bit
        int sign = (diff < 0) ? 8 : 0;
        if (0 != sign) {
            diff = (-diff);
        }

        // Step 2 - Divide and clamp
        //
        // Note:
        // This code *approximately* computes:
        //    delta = diff * 4 / step;
        //    vpdiff = (delta + 0.5) * step / 4;
        // but in shift step bits are dropped. The net result of this is
        // that even if you have fast mul/div hardware you cannot put it to
        // good use since the fixup would be too expensive.

        // Current adpcm output value
        int delta = 0;
        // Current change to valpred
        int vpDiff = (step >> 3);

        if (diff >= step) {
            delta = 4;
            diff -= step;
            vpDiff += step;
        }
        step >>= 1;
        if (diff >= step) {
            delta |= 2;
            diff -= step;
            vpDiff += step;
        }
        step >>= 1;
        if (diff >= step) {
            delta |= 1;
            vpDiff += step;
        }

        // Step 3 - Update previous value
        if (0 != sign) {
            valPrev -= vpDiff;
        } else {
            valPrev += vpDiff;
        }

        // Step 4 - Clamp previous value to 16 bits
        if (valPrev > 32767) {
            valPrev = 32767;
        } else if (valPrev < -32768) {
            valPrev = -32768;
        }

        // Step 5 - Assemble value, update index and step values
        delta |= sign;

        index += indexTable[delta];
        if (index < 0) { index = 0; }
        if (index > 88) { index = 88; }
        step = stepSizeTable[index];

        //
        state.valPrev = valPrev;
        state.index = index;

        return delta;
    }

    /**
     * @param input adpcm
     */
    public int decode(int input) {

        //
        int valPrev = state.valPrev;
//System.err.printf("%d\n", valPrev);
        // Current step change index
        int index = state.index;

        if (index < 0) {
            index = 0;
        } else if (index > 88) {
            index = 88;
        }

        int step = stepSizeTable[index];

        // Current adpcm output value
        int delta = input & 0xf;

        // Step 2 - Find new index value (for later)
        index += indexTable[delta];
        if (index < 0) {
            index = 0;
        } else if (index > 88) {
            index = 88;
        }

        // Step 3 - Separate sign and magnitude
        // Current adpcm sign bit
        int sign = delta & 8;
        delta = delta & 7;

        // Step 4 - Compute difference and new predicted value
        //
        // Computes 'vpdiff = (delta+0.5)*step/4', but see comment
        // in adpcm_coder.
        //
        // Current change to valPrev
        int vpDiff = step >> 3;
        if ((delta & 4) == 4) { vpDiff += step; }
        if ((delta & 2) == 2) { vpDiff += (step >> 1); }
        if ((delta & 1) == 1) { vpDiff += (step >> 2); }

        if (0 != sign) {
            valPrev -= vpDiff;
        } else {
            valPrev += vpDiff;
        }

        // Step 5 - clamp output value
        if (valPrev > 32767) {
            valPrev = 32767;
        } else if (valPrev < -32768) {
            valPrev = -32768;
        }

        // Step 6 - Update step value
        step = stepSizeTable[index];

        //
        state.valPrev = valPrev;
        state.index = index;

        // Step 7 - Output value
        return valPrev;
    }
}

/* */
