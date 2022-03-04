/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.nio.ByteOrder;


/**
 * AudioEngine.
 * <p>
 * Used in {@link vavi.sound.mfi.vavi.sequencer.AudioDataSequencer}.
 * </p>
 * <p>
 * Currently, an implementation class of this interface should be an bean.
 * (means having a contractor without argument)
 * {@link #encode(int, int, byte[])} related should be stateless.
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051116 nsano initial version <br>
 */
public interface AudioEngine {

    /**
     * Sets adpcm data.
     * @param streamNumber packet id
     * @param channel channel no, -1 is means undefined
     * @param sampleRate adpcm sampling rate
     * @param bits adpcm sampling bits
     * @param channels adpcm channels
     * @param adpcm adpcm data
     * @param continued true data is continued
     */
    void setData(int streamNumber, int channel, int sampleRate, int bits, int channels, byte[] adpcm, boolean continued);

    /**
     * Stop adpcm playing.
     * @param streamNumber packet id
     */
    void stop(int streamNumber);

    /**
     * Start adpcm playing.
     * @param streamNumber packet id
     */
    void start(int streamNumber);

    /**
     * Encodes pcm data.
     * @param bits adpcm sampling bits
     * @param channels input pcm and output adpcm channels
     * @param pcm pcm data, currently 16bit only
     * @return adpcm data,
     *               when channels = 2, return L, R concatenated byte array,
     *               currently 4 bit only
     * <li> TODO pcm bits
     */
    byte[] encode(int bits, int channels, byte[] pcm);

    /** */
    class Data {
        /**
         * channel no
         * TODO how to deal 0 ~ 3
         */
        public int channel;
        /** ADPCM sampling rate */
        public int sampleRate;
        /** ADPCM sampling bits */
        public int bits;
        /** ADPCM number of channels */
        public int channels;
        /** ADPCM data */
        public byte[] adpcm;
        /** ADPCM data is continued */
        public boolean continued = false;
    }

    /** monaural stereo conversion */
    class Util {
        /** left */
        private static final int L = 0;
        /** right */
        private static final int R = 1;

        /**
         * separates interleaved PCM to an array L R order.
         * @param stereo PCM stereo, currently 16bit only
         * @param bits PCM bits, TODO currently unused
         * @param byteOrder PCM 16 bit byte order, TODO currently unused
         * @return PCM monaural L, R channels
         * <pre>
         *          0  1  2  3  4
         *         +--+--+--+--+--+--+--+--+--+
         * stereo  |LH|LR|RH|RL|...
         *         +--+--+--+--+--+--+--+--+--+
         *
         *         +--+--+--+--+--+
         * mono[0] |LH|LR|...
         *         +--+--+--+--+--+
         *         +--+--+--+--+--+
         * mono[1] |RH|RL|...
         *         +--+--+--+--+--+
         * </pre>
         */
        public static byte[][] toMono(byte[] stereo, int bits, ByteOrder byteOrder) {
            byte[][] monos = new byte[2][stereo.length / 2];
            for (int i = 0; i < stereo.length / 4; i++) {
                monos[L][i * 2 + 0] = stereo[i * 4 + 0];
                monos[L][i * 2 + 1] = stereo[i * 4 + 1];
                monos[R][i * 2 + 0] = stereo[i * 4 + 2];
                monos[R][i * 2 + 1] = stereo[i * 4 + 3];
            }
            return monos;
        }

        /**
         * this seems to say interleave.
         * @param monoL ADPCM monaural L, currently 4bit only
         * @param monoR ADPCM monaural R, currently 4bit only
         * @param bits ADPCM bits, TODO currently unused
         * @param byteOrder ADPCM 4bit byte order, TODO currently unused
         * @return ADPCM stereo
         */
        public static byte[] toStereo(byte[] monoL, byte[] monoR, int bits, ByteOrder byteOrder) {
            byte[] stereo = new byte[monoL.length * 2];
            for (int i = 0; i < monoL.length; i++) {
                int l1 = (monoL[i] >> 4) & 0x0f;
                int l2 =  monoL[i] & 0x0f;
                int r1 = (monoR[i] >> 4) & 0x0f;
                int r2 =  monoR[i] & 0x0f;
                stereo[i * 2 + 0] = (byte) (((l1 << 4) | r1) & 0xff);
                stereo[i * 2 + 1] = (byte) (((l2 << 4) | r2) & 0xff);
            }
            return stereo;
        }

        /**
         * not interleaved.
         * @param monoL adpcm data L
         * @param monoR adpcm data R
         * @return L, R concatenated adpcm data
         */
        public static byte[] concatenate(byte[] monoL, byte[] monoR) {
            byte[] lr = new byte[monoL.length + monoR.length];
            System.arraycopy(monoL, 0, lr, 0, monoL.length);
            System.arraycopy(monoR, 0, lr, monoL.length, monoR.length);
            return lr;
        }
    }
}

/* */
