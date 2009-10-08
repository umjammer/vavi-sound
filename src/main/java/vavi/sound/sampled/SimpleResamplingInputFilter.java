/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * SimpleResamplingInputFilter. 
 *
 * @require tritonus_remaining-XXX.jar
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060124 nsano initial version <br>
 */
public class SimpleResamplingInputFilter implements InputFilter {
    /** */
    protected int outSamplingRate;

    /** */
    public SimpleResamplingInputFilter(int outSamplingRate) {
        this.outSamplingRate = outSamplingRate;
    }

    /** */
    public AudioInputStream doFilter(AudioInputStream sourceAis) throws IOException, UnsupportedAudioFileException {
        AudioFormat inAudioFormat = sourceAis.getFormat();
System.err.println("IN: " + inAudioFormat);
        // 1: PCM_SIGNED ? Hz, 16 bit, stereo, 2 bytes/frame, little-endian
        AudioFormat outAudioFormat = new AudioFormat(
            inAudioFormat.getEncoding(),
            outSamplingRate,
            inAudioFormat.getSampleSizeInBits(),
            inAudioFormat.getChannels(),
            inAudioFormat.getFrameSize(),
            inAudioFormat.getFrameRate(),
            inAudioFormat.isBigEndian());
System.err.println("OUT: " + outAudioFormat);
System.err.println("OK: " + AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        return AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
    }
}

/* */
