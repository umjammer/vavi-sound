/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ima;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import vavi.sound.adpcm.ima.ImaInputStream;


/**
 * Converts an IMA bitstream into a PCM 16bits/sample audio stream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
class Ima2PcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     *
     * @param in the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     * @throws IOException
     */
    public Ima2PcmAudioInputStream(InputStream in, AudioFormat format, long length, int samplesPerBlock, int blockSize) throws IOException {
        super(new ImaInputStream(in, samplesPerBlock, format.getChannels(), blockSize, ByteOrder.LITTLE_ENDIAN), format, length);
    }
}

/* */
