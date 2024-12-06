/*
 * http://hackipedia.org/Platform/Sega/Genesis/hardware,%20FM%20synthesis,%20YM2608/html/adpcm.html
 */

package vavi.sound.adpcm.ym2608;

import vavi.sound.adpcm.Codec;


/**
 * YAMAHA (YM2608) ADPCM Codec
 *
 * @author <a href="http://www.memb.jp/~dearna/">Masashi Wada</a> (DEARNA)
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030823 nsano port to java <br>
 */
class Ym2608 implements Codec {

    /** */
    private class State {
        long stepSize = 127;
        long xn;
        int count = 0;
        void next() {
            count++;
            if (count % 1024 == 0) {
                state.stepSize = 127;
                state.xn = 0;
            }
        }
    }

    /** */
    private final State state = new State();

    /** */
    private static final int[] stepsizeTable = {
        57, 57, 57, 57, 77, 102, 128, 153,
        57, 57, 57, 57, 77, 102, 128, 153
    };

    /**
     * @param pcm PCM 16bit
     * @return ADPCM 4bit
     */
    @Override
    public int encode(int pcm) {

        // encoding process 2
        long dn = pcm - state.xn;
//logger.log(Level.TRACE, "%05d: %d, %d, %d".formatted(ccc, dn, pcm, state.xn)); // OK
        // encoding process 3, 4
        // calc An from "I = | dn | / Sn"
        // calc using integer part of production.
        long i = (int) (((Math.abs(dn)) << 16) / ((state.stepSize) << 14));
//logger.log(Level.TRACE, "%05d: %d".formatted(ccc, i)); // OK
        if (i > 7) {
            i = 7;
        }
        int adpcm = (int) (i & 0xff);

        // encoding process 5
        // L3 + L2 / 2 + L1 / 4 + 1 / 8 * stepSize multiply 8 times and calc as integer
        i = (adpcm * 2L + 1) * state.stepSize / 8;
//logger.log(Level.TRACE, "%05d: %d, %d, %d".formatted(ccc, i, adpcm, state.stepSize)); // OK

        // if "1 - 2 * L4 -> L4" is 1 equals multiply -1
        if (dn < 0) {
            // when - case, add a sign bit
            // at encode process 5, sign of ADPCM is not necessary
            // so holding it until prediction updating
            adpcm |= 0x8;
            state.xn -= i;
        } else {
            state.xn += i;
        }
//logger.log(Level.TRACE, "%05d: %d, %d".formatted(ccc, state.xn, i));

        // encode process 6
        // update step size
        state.stepSize = (stepsizeTable[adpcm] * state.stepSize) / 64;
//logger.log(Level.TRACE, "%05d: %d, %d, %d".formatted(ccc, i, adpcm, state.stepSize)); // OK

        // encode process 7
        if (state.stepSize < 127) {
            state.stepSize = 127;
        } else if (state.stepSize > 24576) {
            state.stepSize = 24576;
        }

        state.next();

        return adpcm;
    }

    /**
     * @param adpcm ADPCM (LSB 4 bit available)
     * @return PCM
     */
    @Override
    public int decode(int adpcm) {

        // decode process 2, 3
        // L3 + L2 / 2 + L1 / 4 + 1 / 8 * stepSize multiply 8 times and calc as integer.
        long i = ((adpcm & 7) * 2 + 1) * state.stepSize / 8;
        if ((adpcm & 8) != 0) {
            state.xn -= i;
        } else {
            state.xn += i;
        }
//logger.log(Level.TRACE, "%05d: %d, %d, %d".formatted(state.count, state.xn, state.stepSize, adpcm)); // OK

        // decode process 4
        if (state.xn > 32767) {
            state.xn = 32767;
        } else if (state.xn < -32768) {
            state.xn = -32768;
        }
        // decode process 5
        state.stepSize = state.stepSize * stepsizeTable[adpcm] / 64;

        // decode process 6
        if (state.stepSize < 127) {
            state.stepSize = 127;
        } else if (state.stepSize > 24576) {
            state.stepSize = 24576;
        }
//logger.log(Level.TRACE, "%05d: %d, %d, %d".formatted(state.count, state.xn, state.stepSize, adpcm)); // OK

        // store PCM
        int pcm = (int) state.xn;

        state.next();

        return pcm;
    }
}
