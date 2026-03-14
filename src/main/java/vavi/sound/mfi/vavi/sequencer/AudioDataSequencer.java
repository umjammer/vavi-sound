/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mobile.AudioEngine;


/**
 * AudioData message sequencer.
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
    class Factory {

        /** */
        private static final ThreadLocal<AudioEngine> audioEngineStore = new ThreadLocal<>();

        /**
         * Second time or later.
         * @return nullable
         */
        public static AudioEngine getAudioEngine() {
            return audioEngineStore.get();
        }

        private static final Set<AudioEngine> engines = new HashSet<>();

        /**
         * First time.
         * @return same instance for each format
         * @throws IllegalArgumentException when audio engine not found
         */
        public static AudioEngine getAudioEngine(int format) {
            for (AudioEngine engine : engines) {
                if (engine.accept(format)) {
                    audioEngineStore.set(engine);
                    return engine;
                }
            }
            throw new IllegalArgumentException("format: " + format);
        }

        static {
            for (AudioEngine engine : ServiceLoader.load(AudioEngine.class)) {
                engines.add(engine);
            }
        }
    }
}
