/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * FilterChain. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060124 nsano initial version <br>
 */
public class FilterChain {

    /** */
    private List<InputFilter> inputFilters = new ArrayList<>();

    /** */
    public AudioInputStream doFilter(AudioInputStream audioInputStream) throws IOException, UnsupportedAudioFileException {
        for (InputFilter inputFilter : inputFilters) {
            audioInputStream = inputFilter.doFilter(audioInputStream);
        }
        return audioInputStream;
    }

    static {
        // TODO implement
    }
}

/* */
