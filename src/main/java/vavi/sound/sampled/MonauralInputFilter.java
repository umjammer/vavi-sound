/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled;

import java.io.IOException;
import java.util.logging.Level;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import vavi.util.Debug;


/**
 * MonauralInputFilter.
 *
 * @require tritonus_remaining-XXX.jar
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060124 nsano initial version <br>
 */
public class MonauralInputFilter implements InputFilter {

    @Override
    public AudioInputStream doFilter(AudioInputStream sourceAis) throws IOException, UnsupportedAudioFileException {
        AudioFormat inAudioFormat = sourceAis.getFormat();
        if (inAudioFormat.getChannels() != 2) {
            throw new IllegalArgumentException("illegal channels: " + inAudioFormat.getChannels());
        }
        // target: PCM_SIGNED ? Hz, 16 bit, mono, 2 bytes/frame, little-endian
        AudioFormat outAudioFormat = new AudioFormat(
            inAudioFormat.getEncoding(),
            inAudioFormat.getSampleRate(),
            inAudioFormat.getSampleSizeInBits(),
            1,
            inAudioFormat.getFrameSize(),
            inAudioFormat.getFrameRate(),
            inAudioFormat.isBigEndian());
Debug.println(Level.FINE, "OUT: " + outAudioFormat);
Debug.println(Level.FINE, "OK: " + AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        return AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
    }
}
