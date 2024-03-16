/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.rohm;

import java.io.InputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import vavi.sound.adpcm.rohm.RohmInputStream;


/**
 * Converts an Rohm bitstream into a PCM 16bits/sample audio stream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
class Rohm2PcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     *
     * @param in the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Rohm2PcmAudioInputStream(InputStream in, AudioFormat format, long length) {
        super(new RohmInputStream(in, ByteOrder.LITTLE_ENDIAN), format, length);
    }
}
