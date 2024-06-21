/*
 * Copyright (C) 1999 Stanley J. Brooks <stabro@megsinet.net>
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package vavi.sound.adpcm.ima;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import static java.lang.System.getLogger;


/**
 * IMA ADPCM
 *
 * @author <a href="mailto:stabro@megsinet.net">Stanley J. Brooks</a>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030715 nsano port to java <br>
 */
class Ima {

    private static final Logger logger = getLogger(Ima.class.getName());

    /** */
    public static final int ISSTMAX = 88;

    /**
     * Lookup tables for IMA ADPCM format
     */
    private static final int[] stepSizeTable = {
        7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34,
        37, 41, 45, 50, 55, 60, 66, 73, 80, 88, 97, 107, 118, 130, 143,
        157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494,
        544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552,
        1707, 1878, 2066, 2272, 2499, 2749, 3024, 3327, 3660, 4026,
        4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442,
        11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623,
        27086, 29794, 32767
    };

    /**
     * +0 - +3, decrease step size <br>
     * +4 - +7, increase step size <br>
     * -0 - -3, decrease step size <br>
     * -4 - -7, increase step size <br>
     */
    private static int adjustState(int c) {
        return (c < 4) ? -1 : 2 * c - 6;
    }

    /** */
    private static final int[][] stateAdjustTable = new int[ISSTMAX + 1][8];

    /* */
    static {
        for (int i = 0; i <= ISSTMAX; i++) {
            for (int j = 0; j < 8; j++) {
                int k = i + adjustState(j);
                if (k < 0) {
                    k = 0;
                } else if (k > ISSTMAX) {
                    k = ISSTMAX;
                }
                stateAdjustTable[i][j] = k;
            }
        }
    }

    /**
     *
     * @param channel channel number to decode, REQUIRE 0 <= ch < chans
     * @param channels total channels
     * @param inBuffer input buffer[blockAlign]
     * @param outBuffer obuff[n] will be output samples
     * @param op outBuffer pointer
     * @param length samples to decode PER channel, REQUIRE n % 8 == 1
     * @param outIncrement index difference between successive output samples
     */
    private static void decode(int channel,
                               int channels,
                               byte[] inBuffer,
                               int[] outBuffer,
                               int op,
                               int length,
                               int outIncrement) {

        // input pointer to 4-byte block state-initializer
        int ip = 4 * channel;
        // amount by which to incr ip after each 4-byte read
        int i_inc = 4 * (channels - 1);
        // need cast for sign-extend
        int val = (inBuffer[ip] & 0xff) + ((inBuffer[ip + 1] & 0xff) << 8);
        if ((val & 0x8000) != 0) {
            val -= 0x10000;
        }
        int state = inBuffer[ip + 2] & 0xff;
        if (state > ISSTMAX) {
logger.log(Level.DEBUG, "IMA_ADPCM block ch" + channel + " initial-state (" + state + ") out of range");
            state = 0;
        }
        // specs say to ignore ip[3] , but write it as 0
        ip += 4 + i_inc;

        outBuffer[op] = val; // 1st output sample for this channel
        op += outIncrement;

        for (int i = 1; i < length; i++) {
            int cm;

            if ((i & 1) != 0) { // 1st of pair
                cm = inBuffer[ip] & 0x0f;
            } else {
                cm = (inBuffer[ip++] & 0xf0) >> 4;
                // ends the 8-sample input block for this channel
                if ((i & 7) == 0) {
                    ip += i_inc; // skip ip for next group
                }
            }

            int step = stepSizeTable[state];
            // Update the state for the next sample
            int c = cm & 0x07;
            state = stateAdjustTable[state][c];

            int dp = 0;
            if ((c & 4) != 0) {
                dp += step;
            }
            step = step >> 1;
            if ((c & 2) != 0) {
                dp += step;
            }
            step = step >> 1;
            if ((c & 1) != 0) {
                dp += step;
            }
            step = step >> 1;
            dp += step;
            if (c != cm) {
                val -= dp;
                if (val < -0x8000) {
//logger.log(Level.DEBUG, String.format("%04x -> %d", val, -0x8000));
                    val = -0x8000;
                }
            } else {
                val += dp;
                if (val > 0x7fff) {
//logger.log(Level.DEBUG, String.format("%04x -> %d", val, 0x7fff));
                    val = 0x7fff;
                }
            }
            outBuffer[op] = val;
            op += outIncrement;
        }
    }

    /**
     * Outputs interleaved samples into one output buffer
     *
     * @param channels total channels
     * @param inBuffer input buffer[blockAlign]
     * @param outBuffer output samples, n*chans
     * @param length samples to decode PER channel, REQUIRE n % 8 == 1
     */
    public static void decodeBlock(int channels, byte[] inBuffer, int[] outBuffer, int length) {
        for (int ch = 0; ch < channels; ch++) {
            decode(ch, channels, inBuffer, outBuffer, ch, length, channels);
        }
    }

    /**
     * Outputs non-interleaved samples into chan separate output buffers
     *
     * @param channels total channels
     * @param inBuffer input buffer[blockAlign]
     * @param outBuffers chan output sample buffers, each takes n samples
     * @param length samples to decode PER channel, REQUIRE n % 8 == 1
     */
    public static void decodeBlocks(int channels, byte[] inBuffer, int[][] outBuffers, int length) {
        for (int ch = 0; ch < channels; ch++) {
            decode(ch, channels, inBuffer, outBuffers[ch], 0, length, 1);
        }
    }

    /**
     * @param ch channel number to encode, REQUIRE 0 <= ch < chans
     * @param chans total channels
     * @param v0 value to use as starting prediction0
     * @param ibuff ibuff[] is interleaved input samples
     * @param n samples to encode PER channel, REQUIRE n % 8 == 1
     * @param st input/output state, REQUIRE 0 <= *st <= ISSTMAX
     * @param obuff output buffer[blockAlign], or null for no output
     */
    private static int encode(int ch, int chans, int v0, int[] ibuff, int n, int[] st, int stp, byte[] obuff) {

        // set 0 only to shut up gcc's 'might be uninitialized'
        int o_inc = 0;

        // point ip to 1st input sample for this channel
        int ip = ch;
        int itop = n * chans;
        int val = ibuff[ip] - v0;
        ip += chans; // 1st input sample for this channel
        // long long is okay also, speed abt the same
        // d2 will be sum of squares of errors, given input v0 and *st
        double d2 = val * val;
        val = v0;

        int op = 0; // output pointer (or null)
        // null means don't output, just compute the rms error
        if (obuff != null) {
            // where to put this channel's 4-byte block state-initializer
            op += 4 * ch;
            // amount by which to incr op after each 4-byte written
            o_inc = 4 * (chans - 1);
            obuff[op++] = (byte) val;
            obuff[op++] = (byte) (val >> 8);
            obuff[op++] = (byte) st[stp];
            // they could have put a mid-block state-correction here
            obuff[op++] = 0;
            // _sigh_ NEVER waste a byte. It's a rule!
            op += o_inc;
        }

        int state = st[stp];

        for (int i = 0; ip < itop; ip += chans) {
            int step, d, dp, c;

            // difference between last prediction and current sample
            d = ibuff[ip] - val;

            step = stepSizeTable[state];
            c = (Math.abs(d) << 2) / step;
            if (c > 7)
                c = 7;
            // Update the state for the next sample
            state = stateAdjustTable[state][c];

            // if we want output, put it in proper place
            if (obuff != null) {
                int cm = c;
                if (d < 0) {
                    cm |= 8;
                }
                if ((i & 1) != 0) { // odd numbered output
                    obuff[op++] |= (cm << 4);
                    // ends the 8-sample output block for this channel
                    if (i == 7) {
                        op += o_inc; // skip op for next group
                    }
                } else {
//logger.log(Level.DEBUG, "op: " + op);
                    obuff[op] = (byte) cm;
                }
                i = (i + 1) & 0x07;
            }

            dp = 0;
            if ((c & 4) != 0) {
                dp += step;
            }
            step = step >> 1;
            if ((c & 2) != 0) {
                dp += step;
            }
            step = step >> 1;
            if ((c & 1) != 0) {
                dp += step;
            }
            step = step >> 1;
            dp += step;
            if (d < 0) {
                val -= dp;
                if (val < -0x8000) {
                    val = -0x8000;
                }
            } else {
                val += dp;
                if (val > 0x7fff) {
                    val = 0x7fff;
                }
            }

            int x = ibuff[ip] - val;
            d2 += x * x;
        }

        d2 /= n; // be sure it's non-negative
        st[stp] = state;

        return (int) Math.sqrt(d2);
    }

    /**
     * mash one channel... if you want to use opt > 0, 9 is a reasonable value
     *
     * @param channel channel number to encode, REQUIRE 0 <= ch < chans
     * @param channels total channels
     * @param inBuffer interleaved input samples
     * @param length samples to encode PER channel, REQUIRE n % 8 == 1
     * @param steps input/output state, REQUIRE 0 <= *st <= ISSTMAX
     * @param sp steps pointer
     * @param outBuffer output buffer[blockAlign]
     * @param option non-zero allows some cpu-intensive code to improve output
     */
    private void encodeChannel(int channel,
                              int channels,
                              int[] inBuffer,
                              int length,
                              int[] steps,
                              int sp,
                              byte[] outBuffer,
                              int option) {

        int[] snext = new int[1];
        int d;

        int s32 = steps[sp];
        int s0 = s32;
        if (option > 0) {
            snext[0] = s0;
            int d32 = encode(channel, channels, inBuffer[0], inBuffer, length, snext, 0, null);
            int d0 = d32;

            boolean w = false;
            int low = s0;
            int hi = low;
            int low0 = low - option;
            if (low0 < 0) {
                low0 = 0;
            }
            int hi0 = hi + option;
            if (hi0 > ISSTMAX) {
                hi0 = ISSTMAX;
            }
            while (low > low0 || hi < hi0) {
                if (!w && low > low0) {
                    snext[0] = --low;
                    d = encode(channel, channels, inBuffer[0], inBuffer, length, snext, 0, null);
                    if (d < d0) {
                        d0 = d;
                        s0 = low;
                        low0 = low - option;
                        if (low0 < 0) {
                            low0 = 0;
                        }
                        hi0 = low + option;
                        if (hi0 > ISSTMAX) {
                            hi0 = ISSTMAX;
                        }
                    }
                }
                if (w && hi < hi0) {
                    snext[0] = ++hi;
                    d = encode(channel, channels, inBuffer[0], inBuffer, length, snext, 0, null);
                    if (d < d0) {
                        d0 = d;
                        s0 = hi;
                        low0 = hi - option;
                        if (low0 < 0) {
                            low0 = 0;
                        }
                        hi0 = hi + option;
                        if (hi0 > ISSTMAX) {
                            hi0 = ISSTMAX;
                        }
                    }
                }
                w = !w;
            }
            steps[sp] = s0;
        }
        d = encode(channel, channels, inBuffer[0], inBuffer, length, steps, sp, outBuffer);
    }

    /**
     * mash one block. if you want to use opt > 0, 9 is a reasonable value
     *
     * @param channels total channels
     * @param inBuffer ip[] is interleaved input samples
     * @param length samples to encode PER channel, REQUIRE n % 8 == 1
     * @param steps input/output state, REQUIRE 0 <= *st <= ISSTMAX
     * @param outBuffer output buffer[blockAlign]
     * @param option non-zero allows some cpu-intensive code to improve output
     */
    public void encodeBlock(int channels, int[] inBuffer, int length, int[] steps, byte[] outBuffer, int option) {
        for (int ch = 0; ch < channels; ch++) {
            encodeChannel(ch, channels, inBuffer, length, steps, ch, outBuffer, option);
        }
    }

    /**
     * Returns the number of samples/channel which would go in the dataLength,
     * given the other parameters ... if input samplesPerBlock is 0,
     * then returns the max samplesPerBlock which would go into a block of size blockAlign
     * Yes, it is confusing.
     */
    public static int getSamplesIn(int dataLength, int channels, int blockAlign, int samplesPerBlock) {
        int m, n;
//logger.log(Level.DEBUG, "dataLength: " + dataLength);
//logger.log(Level.DEBUG, "channels: " + channels);
//logger.log(Level.DEBUG, "blockAlign: " + blockAlign);
//logger.log(Level.DEBUG, "samplesPerBlock: " + samplesPerBlock);

        if (samplesPerBlock != 0) {
            n = (dataLength / blockAlign) * samplesPerBlock;
            m = dataLength % blockAlign;
        } else {
            n = 0;
            m = blockAlign;
        }
//logger.log(Level.DEBUG, "n: " + n);
//logger.log(Level.DEBUG, "m: " + m);
        if (m >= 4 * channels) {
            m -= 4 * channels; // number of bytes beyond block-header
            m /= 4 * channels; // number of 4-byte blocks/channel beyond header
            m = 8 * m + 1; // samples/chan beyond header + 1 in header
            if (samplesPerBlock != 0 && m > samplesPerBlock) {
                m = samplesPerBlock;
            }
            n += m;
        }
        return n;
    }

    /**
     * Returns minimum blocksize which would be required to encode number of channels
     * with given samplesPerBlock
     *
     * @param channels channels
     * @param samplesPerBlock samples par block
     */
    public static int getBytesPerBlock(int channels, int samplesPerBlock) {
        // per channel, ima has blocks of len 4, the 1st has 1st sample,
        // the others up to 8 samples per block,
        // so number of later blocks is (nsamp-1 + 7) / 8,
        // total blocks / chan is (nsamp - 1 + 7) / 8 + 1 = (nsamp + 14) / 8
        return (samplesPerBlock + 14) / 8 * 4 * channels;
    }
}
