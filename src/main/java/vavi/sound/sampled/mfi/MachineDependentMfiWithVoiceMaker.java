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
import vavi.util.properties.PrefixedPropertiesFactory;


/**
 * MachineDependentMfiWithVoiceMaker.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
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

    /** */
    static final PrefixedPropertiesFactory<String, MachineDependentMfiWithVoiceMaker> factory =
        new PrefixedPropertiesFactory<String, MachineDependentMfiWithVoiceMaker>("/vavi/sound/sampled/mfi/MfiWithVoiceMaker.properties", "class.") {

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
