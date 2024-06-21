/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mobile.AudioEngine;
import vavi.util.properties.PrefixedClassPropertiesFactory;


/**
 * AudioData message sequencer.
 * <pre>
 * properties file ... "/vavi/sound/mfi/vavi/vavi.properties"
 * name prefix ... "audioEngine.format."
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070119 nsano initial version <br>
 * @since MFi 4.0
 */
public interface AudioDataSequencer {

    /** for {@link AudioDataSequencer} */
    int META_FUNCTION_ID_MFi4 = 0x02;

    /** */
    void sequence() throws InvalidMfiDataException;

    /** */
    class Factory extends PrefixedClassPropertiesFactory<Integer, AudioEngine> {

        /** */
        private static final ThreadLocal<AudioEngine> audioEngineStore = new ThreadLocal<>();

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
        private static final Factory instance = new Factory();

        /** */
        private Factory() {
            super("/vavi/sound/mfi/vavi/vavi.properties", "audioEngine.format.");
        }
    }
}
