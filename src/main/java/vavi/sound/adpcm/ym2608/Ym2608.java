/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ym2608;

import vavi.sound.adpcm.Codec;


/**
 * YAMAHA (YM2608) ADPCM Codec
 * 
 * @author <a href="http://www.memb.jp/~dearna/">Masashi Wada</a> (DEARNA)
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030823 nsano port from c <br>
 */
class Ym2608 implements Codec {

    /** */
    private class State {
        int stepSize = 127;
        int xn;
    }

    /** */
    private State state = new State();

    /** */
    private static final int[] stepsizeTable = {
        57, 57, 57, 57, 77, 102, 128, 153,
        57, 57, 57, 57, 77, 102, 128, 153
    };

//private int ccc = 0;

    /**
     * @param pcm PCM
     * @return ADPCM
     */
    public int encode(int pcm) {

        // エンコード処理 2
        int dn = pcm - state.xn;
//System.err.printf("%05d: %d, %d, %d\n", ccc, dn, pcm, xn);
        // エンコード処理 3, 4
        // I = | dn | / Sn から An を求める。
        // 乗数を使用して整数位で演算する。
        int i = (int) ((((long) Math.abs(dn)) << 16) / (state.stepSize << 14));
//System.err.printf("%05d: %d\n", ccc, i);
        if (i > 7) {
            i = 7;
        }
        int adpcm = i & 0xff;

        // エンコード処理 5
        // L3 + L2 / 2 + L1 / 4 + 1 / 8 * stepSize を 8 倍して整数演算
        i = (adpcm * 2 + 1) * state.stepSize / 8;
//System.err.printf("%05d: %d, %d, %d\n", ccc, i, adpcm, state.stepSize);

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
//System.err.printf("%05d: %d, %d\n", ccc, xn, i);

        // エンコード処理 6
        // ステップサイズの更新
//System.err.printf("%05d: %d, %d, %d\n", ccc, i, adpcm, state.stepSize);
        state.stepSize = (stepsizeTable[adpcm] * state.stepSize) / 64;

        // エンコード処理 7
        if (state.stepSize < 127) {
            state.stepSize = 127;
        } else if (state.stepSize > 24576) {
            state.stepSize = 24576;
        }
//ccc++;
//if (ccc > 300) {
// System.exit(0);
//}

        return adpcm;
    }

    /**
     * @param adpcm ADPCM (LSB 4 bit 有効)
     * @return PCM 
     */
    public int decode(int adpcm) {

        // デコード処理 2, 3
        // L3 + L2 / 2 + L1 / 4 + 1 / 8 * stepSize を 8 倍して整数演算
        int i = ((adpcm & 7) * 2 + 1) * state.stepSize / 8;
        if ((adpcm & 8) != 0) {
            state.xn -= i;
        } else {
            state.xn += i;
        }

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

        // PCM で保存する
        return state.xn;
    }
}

/* */
