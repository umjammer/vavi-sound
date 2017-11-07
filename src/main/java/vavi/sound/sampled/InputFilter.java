/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled;

import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * InputFilter.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060124 nsano initial version <br>
 */
public interface InputFilter {
    /** */
    AudioInputStream doFilter(AudioInputStream audioInputStream) throws IOException, UnsupportedAudioFileException;
}

/* */
