/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.sequencer;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import vavi.sound.mobile.AudioEngine;
import vavi.sound.smaf.InvalidSmafDataException;

import static java.lang.System.getLogger;


/**
 * WaveSequencer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public interface WaveSequencer {

    /** for {@link WaveSequencer} */
    int SYSEX_FUNCTION_ID_SMAF = 0x03;

    /** */
    void sequence() throws InvalidSmafDataException;

    /** factory */
    class Factory {

        private static final Logger logger = getLogger(Factory.class.getName());

        /** */
        private static final ThreadLocal<AudioEngine> audioEngineStore = new ThreadLocal<>();

        /**
         * Second time or later.
         */
        public static AudioEngine getAudioEngine() {
logger.log(Level.INFO, "audioEngineStore: " + audioEngineStore.get());
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
