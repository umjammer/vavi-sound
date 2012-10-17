/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mobile.AudioEngine;
import vavi.util.properties.PrefixedPropertiesFactory;


/**
 * AudioData message sequencer.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 070119 nsano initial version <br>
 * @since MFi 4.0
 */
public interface AudioDataSequencer {

    /** for {@link AudioDataSequencer} */
    final int META_FUNCTION_ID_MFi4 = 0x02;

    /** */
    void sequence() throws InvalidMfiDataException;

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
            super("/vavi/sound/mfi/vavi/vavi.properties", "audioEngine.format.");
        }
    }
}

/* */
