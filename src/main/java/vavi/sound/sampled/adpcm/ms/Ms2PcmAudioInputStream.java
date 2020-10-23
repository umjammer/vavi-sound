/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ms;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import vavi.sound.adpcm.ms.MsInputStream;


/**
 * Converts an MS bitstream into a PCM 16bits/sample audio stream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
class Ms2PcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     *
     * @param in the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Ms2PcmAudioInputStream(InputStream in, AudioFormat format, long length,
                                  int samplesPerBlock, int nCoefs, int[][] iCoefs, int blockSize) throws IOException {
        super(new MsInputStream(in, samplesPerBlock,  nCoefs, iCoefs, format.getChannels(), blockSize, ByteOrder.LITTLE_ENDIAN), format, length);
    }
}

/* */
