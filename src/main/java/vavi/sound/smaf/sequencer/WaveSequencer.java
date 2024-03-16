/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.sequencer;

import vavi.sound.mobile.AudioEngine;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.properties.PrefixedClassPropertiesFactory;


/**
 * WaveSequencer.
 * <pre>
 * properties file ... "/vavi/sound/smaf/smaf.properties"
 * name prefix ... "audioEngine.format."
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public interface WaveSequencer {

    /** for {@link WaveSequencer} */
    int META_FUNCTION_ID_SMAF = 0x03;

    /** */
    void sequence() throws InvalidSmafDataException;

    /** factory */
    class Factory extends PrefixedClassPropertiesFactory<Integer, AudioEngine> {

        /** */
        private static ThreadLocal<AudioEngine> audioEngineStore = new ThreadLocal<>();

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
