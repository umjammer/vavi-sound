/*
 * IMA/DVI ADPCM, 2 bits per sample.
 */

package vavi.sound.adpcm.ima;

import vavi.sound.adpcm.Codec;


/** Variable-width IMA ADPCM decoder used by the DoCoMo MFi Type-2 path. */
class Ima2 implements Codec {

    private static final int[] INDEX_TABLE = {-1, 2, -1, 2};

    private static final int[] STEP_TABLE = {
        7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31,
        34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88, 97, 107, 118, 130,
        143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449,
        494, 544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411,
        1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024, 3327, 3660, 4026,
        4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487,
        12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794,
        32767
    };

    private int predictor;
    private int stepIndex;

    @Override
    public int encode(int pcm) {
        throw new UnsupportedOperationException("2-bit IMA encoding is not implemented");
    }

    @Override
    public int decode(int code) {
        code &= 3;
        int index = Math.clamp(stepIndex, 0, 88);
        int step = STEP_TABLE[index];
        int shift = 1;
        int delta = code & 1;
        int diff = step >> shift;
        if ((delta & 1) != 0) {
            diff += step;
        }
        predictor = (code & 2) != 0 ? predictor - diff : predictor + diff;
        predictor = Math.clamp(predictor, -32768, 32767);
        stepIndex = Math.clamp(index + INDEX_TABLE[code], 0, 88);
        return predictor;
    }
}
