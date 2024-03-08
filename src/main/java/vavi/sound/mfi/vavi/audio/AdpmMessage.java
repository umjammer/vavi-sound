/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.audio;

import vavi.sound.mfi.vavi.SubMessage;


/**
 * ADPCM information MFi Audio Sub Chunk.
 *
 * <pre>
 *  &quot;adpm&quot; 3 bytes
 *  0: sampling rate 0 ~ 255 kHz
 *   in case of {@link vavi.sound.mfi.vavi.AudioDataMessage#FORMAT_ADPCM_TYPE2},
 *    only 32, 16, 8 are available
 *  1: sampling bits 0 ~ 255 bits
 *   in cace of {@link vavi.sound.mfi.vavi.AudioDataMessage#FORMAT_ADPCM_TYPE2},
 *    only 2, 4 are available
 *  2: .... 3 210
 *          ~ ~~~
 *          | +- channels 1: mono, 2: stereo, else: reserved
 *          +- 0: non interleave, 1: interleave
 *
 *    when channels is 1, interleave is 0
 *   in case of {@link vavi.sound.mfi.vavi.AudioDataMessage#FORMAT_ADPCM_TYPE2},
 *    when channels is 2, interleave is 0
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070125 nsano initial version <br>
 * @since MFi 5.0
 */
public class AdpmMessage extends SubMessage {

    /** */
    public static final String TYPE = "adpm";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public AdpmMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /** */
    public AdpmMessage(int samplingRate, int samplingBits, boolean interleaved, int channels) {
        super(TYPE, new byte[] {
            (byte) (samplingRate & 0xff),
            (byte) (samplingBits & 0xff),
            (byte) ((interleaved ? 0x08 : 0x00) | (channels & 0x07))
        });
    }

    /** @return 0 ~ 255 [kHz] */
    public int getSamplingRate() {
        byte[] data = getData();
        return data[0] & 0xff;
    }

    /** */
    public int getSamplingBits() {
        byte[] data = getData();
        return data[1] & 0xff;
    }

    /** */
    public boolean isInterleaved() {
        byte[] data = getData();
        return (data[2] & 0x08) == 0x08;
    }

    /** */
    public int getChannels() {
        byte[] data = getData();
        return data[2] & 0x07;
    }

    /** */
    public String toString() {
        return TYPE + ": " + getDataLength() + ": " +
            getSamplingRate() + ", " +
            getSamplingBits() + ", " +
            isInterleaved() + ", " +
            getChannels();
    }
}
