/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.mfi;

import java.io.IOException;
import java.util.List;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.util.properties.PrefixedClassPropertiesFactory;
import vavi.util.properties.PrefixedPropertiesFactory;


/**
 * MachineDependentMfiWithVoiceMaker.
 * <pre>
 * properties file ... "/vavi/sound/sampled/mfi/MfiWithVoiceMaker.properties"
 * name prefix ... "class."
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050403 nsano initial version <br>
 */
public interface MachineDependentMfiWithVoiceMaker {

    /**
     * Gets machine depend MFi events for ADPCM.
     * @param data PCM data
     * @param time second
     * @param sampleRate sampling rate of the <code>data</code>
     * @param bits sampling bits for ADPCM
     */
    List<MfiEvent> getEvents(byte[] data, float time, int sampleRate, int bits, int channels, int masterVolume, int adpcmVolume)
        throws InvalidMfiDataException, IOException;

    /** factory */
    PrefixedPropertiesFactory<String, MachineDependentMfiWithVoiceMaker> factory =
        new PrefixedClassPropertiesFactory<String, MachineDependentMfiWithVoiceMaker>("/vavi/sound/sampled/mfi/MfiWithVoiceMaker.properties", "class.") {

        @Override
        protected String getRestoreKey(String key) {
            return key;
        }

        @Override
        protected String getStoreKey(String key) {
            return key.substring(key.indexOf('.') + 1);
        }
    };
}

/* */
