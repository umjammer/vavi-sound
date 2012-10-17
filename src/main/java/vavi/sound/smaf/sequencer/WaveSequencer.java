/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.sequencer;

import vavi.sound.mobile.AudioEngine;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.properties.PrefixedPropertiesFactory;


/**
 * WaveSequencer. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public interface WaveSequencer {

    /** for {@link WaveSequencer} */
    final int META_FUNCTION_ID_SMAF = 0x03;

    /** */
    void sequence() throws InvalidSmafDataException;

    /** */
    class Factory extends PrefixedPropertiesFactory<Integer, AudioEngine> {

        /** */
        private static ThreadLocal<AudioEngine> audioEngineStore = new ThreadLocal<AudioEngine>();

        /**
         * Second time or later. 
         */
        public static AudioEngine getAudioEngine() {
            return audioEngineStore.get();
        }

        /**
         * First time.
         * @return same instance for each format
         * @throws IllegalArgumentException when audio engine not found
         */
        public static AudioEngine getAudioEngine(int format) {
            AudioEngine engine = instance.get(format);
            audioEngineStore.set(engine);
            return engine;
        }

        /** */
        private static Factory instance = new Factory();

        /** */
        private Factory() {
            super("/vavi/sound/smaf/smaf.properties", "audioEngine.format.");
        }
    }
}

/* */
