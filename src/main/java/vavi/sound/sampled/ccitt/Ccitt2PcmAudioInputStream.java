/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.ccitt;

import java.io.InputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import vavi.sound.adpcm.ccitt.G721InputStream;


/**
 * Converts an Flac bitstream into a PCM 16bits/sample audio stream.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050722 nsano initial version <br>
 */
public class Ccitt2PcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     * 
     * @param in the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Ccitt2PcmAudioInputStream(InputStream in, AudioFormat format, long length) {
        super(new G721InputStream(in, ByteOrder.LITTLE_ENDIAN), format, length);
    }
}

/* */
