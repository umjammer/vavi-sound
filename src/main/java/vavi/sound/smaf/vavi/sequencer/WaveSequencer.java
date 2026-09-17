/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.sequencer;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import javax.sound.midi.Receiver;

import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.MobileExclusive;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.vavi.message.WaveDataMessage;
import vavi.sound.smaf.vavi.message.WaveMessage;

import static java.lang.System.getLogger;


/**
 * WaveSequencer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public interface WaveSequencer {

    Logger logger = getLogger(WaveSequencer.class.getName());

    /** manufacturer vavi function id for {@link WaveSequencer} */
    int SMAF_SYSEX_FUNCTION_ID_WAVE = 0x03;

    /** undefined */
    class UnknownSequencer implements WaveSequencer {
        int functionId;
        UnknownSequencer init(int functionId) {
            this.functionId = functionId;
            return this;
        }
        @Override
        public void sequence(byte[] data, Receiver receiver) throws InvalidSmafDataException {
            logger.log(Level.WARNING, "function: %02x".formatted(functionId));
        }
    }

    /**
     * @param data 0xf0 0x45 0x03
     */
    static WaveSequencer factory(byte[] data) {
        int functionId = data[2] & 0xff;
        return switch (functionId) { // TODO use service loader?
            case MobileExclusive.WAVE -> new WaveDataMessage();
            case MobileExclusive.ON -> new WaveMessage();
            case MobileExclusive.OFF -> new WaveMessage();
            default -> new UnknownSequencer().init(functionId);
        };
    }

    /** assumed as stateless, don't use instance field in this method */
    void sequence(byte[] data, Receiver receiver) throws InvalidSmafDataException;

    /** audio engine factory */
    class AudioEngineFactory {

        private static final Logger logger = getLogger(AudioEngineFactory.class.getName());

        /** */
        private static final ThreadLocal<AudioEngine> audioEngineStore = new ThreadLocal<>();

        /**
         * Second time or later.
         */
        public static AudioEngine getAudioEngine() {
logger.log(Level.TRACE, "audioEngineStore: " + audioEngineStore.get());
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
