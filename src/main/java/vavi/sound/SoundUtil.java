/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;


/**
 * SoundUtil.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/13 umjammer initial version <br>
 */
public final class SoundUtil {

    private SoundUtil() {}

    /**
     * @param gain number between 0 and 1 (loudest)
     * @before {@link DataLine#open()}
     */
    public static void volume(DataLine line, double gain) {
        FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        float dB = (float) (Math.log10(gain) * 20.0);
        gainControl.setValue(dB);
    }
}
