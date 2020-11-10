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
    private State state = new State();

    /** */
    private static final int[] stepsizeTable = {
        57, 57, 57, 57, 77, 102, 128, 153,
        57, 57, 57, 57, 77, 102, 128, 153
    };

    /**
     * @param pcm PCM 16bit
     * @return ADPCM 4bit
     */
    public int encode(int pcm) {

        // エンコード処理 2
        long dn = pcm - state.xn;
//System.err.printf("%05d: %d, %d, %d\n", ccc, dn, pcm, state.xn); // OK
        // エンコード処理 3, 4
        // I = | dn | / Sn から An を求める。
        // 乗数を使用して整数位で演算する。
        long i = (int) (((Math.abs(dn)) << 16) / ((state.stepSize) << 14));
//System.err.printf("%05d: %d\n", ccc, i); // OK
        if (i > 7) {
            i = 7;
        }
        int adpcm = (int) (i & 0xff);

        // エンコード処理 5
        // L3 + L2 / 2 + L1 / 4 + 1 / 8 * stepSize を 8 倍して整数演算
        i = (adpcm * 2L + 1) * state.stepSize / 8;
//System.err.printf("%05d: %d, %d, %d\n", ccc, i, adpcm, state.stepSize); // OK

        // 1 - 2 * L4 -> L4 が 1 の場合は -1 をかけるのと同じ
        if (dn < 0) {
            // - の場合符号ビットを付ける。
            // エンコード処理 5 で ADPCM 符号が邪魔になるので、
            // 予測値更新時まで保留した。
            adpcm |= 0x8;
            state.xn -= i;
        } else {
            state.xn += i;
        }
//System.err.printf("%05d: %d, %d\n", ccc, state.xn, i);

        // エンコード処理 6
        // ステップサイズの更新
        state.stepSize = (stepsizeTable[adpcm] * state.stepSize) / 64;
//System.err.printf("%05d: %d, %d, %d\n", ccc, i, adpcm, state.stepSize); // OK

        // エンコード処理 7
        if (state.stepSize < 127) {
            state.stepSize = 127;
        } else if (state.stepSize > 24576) {
            state.stepSize = 24576;
        }

        state.next();

        return adpcm;
    }

    /**
     * @param adpcm ADPCM (LSB 4 bit 有効)
     * @return PCM
     */
    public int decode(int adpcm) {

        // デコード処理 2, 3
        // L3 + L2 / 2 + L1 / 4 + 1 / 8 * stepSize を 8 倍して整数演算
        long i = ((adpcm & 7) * 2 + 1) * state.stepSize / 8;
        if ((adpcm & 8) != 0) {
            state.xn -= i;
        } else {
            state.xn += i;
        }
//System.err.printf("%05d: %d, %d, %d\n", state.count, state.xn, state.stepSize, adpcm); // OK

        // デコード処理 4
        if (state.xn > 32767) {
            state.xn = 32767;
        } else if (state.xn < -32768) {
            state.xn = -32768;
        }
        // デコード処理 5
        state.stepSize = state.stepSize * stepsizeTable[adpcm] / 64;

        // デコード処理 6
        if (state.stepSize < 127) {
            state.stepSize = 127;
        } else if (state.stepSize > 24576) {
            state.stepSize = 24576;
        }
// System.err.printf("%05d: %d, %d, %d\n", state.count, state.xn, state.stepSize, adpcm); // OK

        // PCM で保存する
        int pcm = (int) state.xn;

        state.next();

        return pcm;
    }
}

/* */
