/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import static java.lang.System.getLogger;


/**
 * MonauralInputFilter.
 *
 * @require tritonus_remaining-XXX.jar
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060124 nsano initial version <br>
 */
public class MonauralInputFilter implements InputFilter {

    private static final Logger logger = getLogger(MonauralInputFilter.class.getName());

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
logger.log(Level.DEBUG, "OUT: " + outAudioFormat);
logger.log(Level.DEBUG, "OK: " + AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        return AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
    }
}
