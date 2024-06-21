/*
 * Copyright (C) 1999 Stanley J. Brooks &lt;stabro@megsinet.net&gt;
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package vavi.sound.adpcm.ms;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import static java.lang.System.getLogger;


/**
 * Codec for MS_ADPCM data.
 * <p>
 * (hopefully) provides interoperability with
 * Microsoft's ADPCM format, but, as usual,
 * see LACK-OF-WARRANTY information below.
 * </p>
 * <p>
 * November 22, 1999<br>
 * specs I've seen are unclear about ADPCM supporting more than 2 channels,
 * but these routines support more channels in a manner which looks (IMHO)
 * like the most natural extension.
 * </p>
 * <p>
 * Remark: code still turbulent, encoding very new.
 * </p>
 * @author <a href="mailto:stabro@megsinet.net">Stanley J. Brooks</a>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030715 nsano port to java <br>
 */
class Ms {

    private static final Logger logger = getLogger(Ms.class.getName());

    /** */
    private static class State {
        /** step size */
        int step;
        final int[] iCoef = new int[2];
    }

    /**
     * Lookup tables for MS ADPCM format.
     * <p>
     * these are step-size adjust factors, where
     * 1.0 is scaled to 0x100
     * </p>
     */
    private static final int[] stepAdjustTable = {
        230, 230, 230, 230, 307, 409, 512, 614,
        768, 614, 512, 409, 307, 230, 230, 230
    };

    /**
     * TODO The first 7 iCoef sets are always hardcoded and must
     * appear in the actual WAVE file.  They should be read in
     * in case a sound program added extras to the list.
     */
    static final int[][] _iCoef = {
        { 256,    0 },
        { 512, -256 },
        { 0,      0 },
        { 192,   64 },
        { 240,    0 },
        { 460, -208 },
        { 392, -232 }
    };

    /**
     * @param sp state pointer
     */
    private static int decode(int code,
                              State[] state,
                              int sp,
                              int sample1,
                              int sample2) {

        // Compute next step value
        int step = state[sp].step;

        int nstep = (stepAdjustTable[code] * step) >> 8;
        state[sp].step = Math.max(nstep, 16);

        // make linear prediction for next sample
        int vlin = ((sample1 * state[sp].iCoef[0]) +
                    (sample2 * state[sp].iCoef[1])) >> 8;
//logger.log(Level.DEBUG, vlin);
        // then add the code * step adjustment
        code -= (code & 0x08) << 1;
        int sample = (code * step) + vlin;

        if (sample > 0x7fff) {
            sample = 0x7fff;
        } else if (sample < -0x8000) {
            sample = -0x8000;
        }

        return sample;
    }

    /**
     * Outputs interleaved samples into one output buffer.
     * @param channels total channels
     * @param nCoef
     * @param iCoef
     * @param inBuffer input buffer[blockAlign]
     * @param outBuffer output samples, length * channels
     * @param length samples to decode per channel
     */
    public void decodeBlock(int channels,
                            int nCoef,
                            int[][] iCoef,
                            byte[] inBuffer,
                            int[] outBuffer,
                            int length) {

        // One decompressor state for each channel
        State[] state = new State[channels];

        // Read the four-byte header for each channel
        int ip = 0;
        for (int channel = 0; channel < channels; channel++) {
            int bpred = inBuffer[ip++] & 0xff;
            if (bpred >= nCoef) {
logger.log(Level.DEBUG, "MSADPCM bpred >= nCoef, arbitrarily using 0");
                bpred = 0;
            }
            state[channel] = new State();
            state[channel].iCoef[0] = iCoef[bpred][0];
            state[channel].iCoef[1] = iCoef[bpred][1];
        }

        for (int channel = 0; channel < channels; channel++) {
            int value = (inBuffer[ip] & 0xff) + ((inBuffer[ip + 1] & 0xff) << 8);
            if ((value & 0x8000) != 0) {
                value -= 0x10000;
            }
//logger.log(Level.DEBUG, "1: " + value);
            state[channel].step = value;
            ip += 2;
        }

        // sample1's directly into obuff
        for (int channel = 0; channel < channels; channel++) {
            int value = (inBuffer[ip] & 0xff) + ((inBuffer[ip + 1] & 0xff) << 8);
            if ((value & 0x8000) != 0) {
                value -= 0x10000;
            }
//logger.log(Level.DEBUG, "2: " + value);
            outBuffer[channels + channel] = value;
            ip += 2;
        }

        // sample2's directly into obuff
        for (int channel = 0; channel < channels; channel++) {
            int value = (inBuffer[ip] & 0xff) + ((inBuffer[ip + 1] & 0xff) << 8);
            if ((value & 0x8000) != 0) {
                value -= 0x10000;
            }
//logger.log(Level.DEBUG, "3: " + value);
            outBuffer[channel] = value;
            ip += 2;
        }

        // already have 1st 2 samples from block-header
        int op = 2 * channels;
        int top = length * channels;

        int channel = 0;
        while (op < top) {
            int b = inBuffer[ip++] & 0xff;
            int tmp = op;
            outBuffer[op++] = decode((b & 0xf0) >> 4, state, channel, outBuffer[tmp - channels], outBuffer[tmp - 2 * channels]);
            if (++channel == channels) {
                channel = 0;
            }
            tmp = op;
            outBuffer[op++] = decode(b & 0x0f, state, channel, outBuffer[tmp - channels], outBuffer[tmp - 2 * channels]);
            if (++channel == channels) {
                channel = 0;
            }
        }
    }

    /**
     * Encode.
     * @param channel channel number to encode, REQUIRE 0 <= ch < chans
     * @param channels total channels
     * @param v values to use as starting 2
     * @param iCoef lin predictor coeffs
     * @param inBuffer interleaved input samples
     * @param length samples to encode PER channel
     * @param steps input/output step, REQUIRE 16 <= *st <= 0x7fff
     * @param sp steps pointer
     * @param outBuffer output buffer[blockAlign], or NULL for no output
     * @return ???
     */
    private static int encode(int channel,
                              int channels,
                              int[] v,
                              int[] iCoef,
                              int[] inBuffer,
                              int length,
                              int[] steps,
                              int sp,
                              byte[] outBuffer) {

        int ox = 0;                         //

        int ip = channel;                   // point ip to 1st input sample for this channel
        int itop = length * channels;
        int v0 = v[0];
        int v1 = v[1];
        int d = inBuffer[ip] - v1;
        ip += channels;                     // 1st input sample for this channel
        // long long is okay also, speed abt the same
        // d2 will be sum of squares of errors, given input v0 and *st
        double d2 = d * d;
        d = inBuffer[ip] - v0;
        ip += channels;                     // 2nd input sample for this channel
        d2 += d * d;

        int step = steps[sp];

        int op = 0;                         // output pointer (or null)
        // null means don't output, just compute the rms error
        if (outBuffer != null) {
            op += channels;                 // skip bpred indices
            op += 2 * channel;              // channel's stepsize
            outBuffer[op] = (byte) step;
            outBuffer[op + 1] = (byte) (step >> 8);
            op += 2 * channels;             // skip to v0
            outBuffer[op] = (byte) v0;
            outBuffer[op + 1] = (byte) (v0 >> 8);
            op += 2 * channels;             // skip to v1
            outBuffer[op] = (byte) v1;
            outBuffer[op + 1] = (byte) (v1 >> 8);
            op = 7 * channels;              // point to base of output nibbles
            ox = 4 * channel;
        }

        for (; ip < itop; ip += channels) {

            // make linear prediction for next sample
            int vlin = (v0 * iCoef[0] + v1 * iCoef[1]) >> 8;
            // difference between linear prediction and current sample
            d = inBuffer[ip] - vlin;
            int dp = d + (step << 3) + (step >> 1);
//logger.log(Level.DEBUG, "vlin: " + vlin + ", d: " + d + ", dp: " + dp + ", in: " + inBuffer[ip] + ", coef: " + iCoef[0] + ", " + iCoef[1]);
            int c = 0;
            if (dp > 0) {
                c = dp / step;
                if (c > 15) {
                    c = 15;
                }
            }
            c -= 8;
            dp = c * step;                  // quantized estimate of samp - vlin
            c &= 0x0f;                      // mask to 4 bits

            v1 = v0;                        // shift history
            v0 = vlin + dp;
            if (v0 < -0x8000) {
                v0 = -0x8000;
            } else if (v0 > 0x7fff) {
                v0 = 0x7fff;
            }

            d = inBuffer[ip] - v0;
            d2 += d * d;                    // update square-error

            if (outBuffer != null) {                  // if we want output, put it in proper place
                // FIXME does c << 0 work properly ?
                outBuffer[op + (ox >> 3)] |= (byte) ((ox & 4) != 0 ? c : (c << 4));
                ox += 4 * channels;
//logger.log(Level.DEBUG, String.format("%1x", c));
            }

            // Update the step for the next sample
            step = (stepAdjustTable[c] * step) >> 8;
            if (step < 16) {
                step = 16;
            }
        }
//if (outBuffer != null)
// logger.log(Level.DEBUG, "");
        d2 /= length; // be sure it's non-negative
//logger.log(Level.DEBUG, String.format("ch%d: st %d->%d, d %.1f", channel, steps[sp], step, Math.sqrt(d2)));
        steps[sp] = step;

        return (int) Math.sqrt(d2);
    }

    /**
     * Encodes a channel.
     *
     * @param channel channel number to encode, REQUIRE 0 <= ch < chans
     * @param channels total channels
     * @param inBuffer interleaved input samples
     * @param length samples to encode per channel, REQUIRE
     * @param steps input/output steps, 16<=st[i]
     * @param sp steps pointer
     * @param outBuffer output buffer[blockAlign]
     */
    private static void encodeChannel(int channel,
                                      int channels,
                                      int[] inBuffer,
                                      int length,
                                      int[] steps,
                                      int sp,
                                      byte[] outBuffer) {

        int[] v = new int[2];
        int[] ss = new int[1];
        int[] s1 = new int[1];

        int n0 = length / 2;
        if (n0 > 32) {
            n0 = 32;
        }
        if (steps[sp] < 16) {
            steps[sp] = 16;
        }
        v[1] = inBuffer[channel];
        v[0] = inBuffer[channel + channels];

        int dmin = 0;
        int kmin = 0;
        int smin = 0;
        // for each of 7 standard coeff sets, we try compression
        // beginning with last step-value, and with slightly
        // forward-adjusted step-value, taking best of the 14
        for (int k = 0; k < 7; k++) {
            ss[0] = steps[sp];
            int s0 = ss[0];
            // with step s0
            int d0 = encode(channel, channels, v, _iCoef[k], inBuffer, length, ss, 0, null);

            s1[0] = s0;
            encode(channel, channels, v, _iCoef[k], inBuffer, n0, s1, 0, null);
//logger.log(Level.DEBUG, String.format(" s32 %d", s1[0]));

            ss[0] = (3 * s0 + s1[0]) / 4;
            s1[0] = ss[0];
            // with step s1
            int d1 = encode(channel, channels, v, _iCoef[k], inBuffer, length, ss, 0, null);
            if (k == 0 || d0 < dmin || d1 < dmin) {
                kmin = k;
                if (d0 <= d1) {
                    dmin = d0;
                    smin = s0;
                } else {
                    dmin = d1;
                    smin = s1[0];
                }
            }
        }
        steps[sp] = smin;
//logger.log(Level.DEBUG, String.format("kmin %d, smin %5d, ", kmin, smin));
        encode(channel, channels, v, _iCoef[kmin], inBuffer, length, steps, sp, outBuffer);
        outBuffer[channel] = (byte) kmin;
    }

    /**
     * Encode.
     * @param channels total channels
     * @param inBuffer interleaved input samples
     * @param length samples to encode PER channel
     * @param steps input/output steps, 16 <= steps[i]
     * @param outBuffer output buffer[blockAlign]
     * @param blockAlign >= 7 * channels + channels * (n - 2) / 2.0
     */
    public void encodeBlock(int channels,
                            int[] inBuffer,
                            int length,
                            int[] steps,
                            byte[] outBuffer,
                            int blockAlign) {

        for (int p = 7 * channels; p < blockAlign; p++) {
            outBuffer[p] = 0;
        }

        for (int channel = 0; channel < channels; channel++) {
            encodeChannel(channel, channels, inBuffer, length, steps, channel, outBuffer);
        }
    }

    /**
     * Returns the number of samples/channel which would be
     * in the dataLength, given the other parameters ...
     * if input samplesPerBlock is 0, then returns the max
     * samplesPerBlock which would go into a block of size blockAlign
     * Yes, it is confusing usage.
     */
    public static int getSamplesIn(int dataLength,
                                   int channels,
                                   int blockAlign,
                                   int samplesPerBlock) {
        int m, n;

        if (samplesPerBlock > 0) {
            n = (dataLength / blockAlign) * samplesPerBlock;
            m =  dataLength % blockAlign;
        } else {
            n = 0;
            m = blockAlign;
        }
//logger.log(Level.DEBUG, "n: " + n);
//logger.log(Level.DEBUG, "m: " + m);
        if (m >= 7 * channels) {
            m -= 7 * channels;              // bytes beyond block-header
            m = (2 * m) / channels + 2;     // nibbles / channels + 2 in header
            if (samplesPerBlock > 0 && m > samplesPerBlock) {
                m = samplesPerBlock;
            }
            n += m;
        }
        return n;
    }

    /** Returns bytes per block. */
    public static int getBytesPerBlock(int channels, int samplesPerBlock) {
        int n = 7 * channels;               // header
        if (samplesPerBlock > 2) {
            n += ((samplesPerBlock - 2) * channels + 1) / 2;
        }
        return n;
    }
}
