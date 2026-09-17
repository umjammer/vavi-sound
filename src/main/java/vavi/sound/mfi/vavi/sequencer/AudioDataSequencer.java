/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.Set;
import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.AudioDataChunk.AudioDataMessage;
import vavi.sound.mfi.vavi.track.AudioChannelPanpotMessage;
import vavi.sound.mfi.vavi.track.AudioChannelVolumeMessage;
import vavi.sound.mfi.vavi.track.AudioPlayMessage;
import vavi.sound.mfi.vavi.track.AudioStopMessage;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.MobileExclusive;


/**
 * AudioData message sequencer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070119 nsano initial version <br>
 * @since MFi 4.0
 */
public interface AudioDataSequencer {

    /** manufacturer vavi function id for {@link AudioDataSequencer} */
    int MFi_SYSEX_FUNCTION_ID_MFi4 = 0x02;

    /**
     * @param data
     */
    static AudioDataSequencer factory(byte[] data) {
        int functionId = data[2] & 0xff;
        return switch (functionId) {
            case MobileExclusive.WAVE -> new AudioDataMessage();
            case MobileExclusive.ON -> new AudioPlayMessage();
            case MobileExclusive.OFF -> new AudioStopMessage();
            case MobileExclusive.PANPOT -> new AudioChannelPanpotMessage();
            case MobileExclusive.VOLUME -> new AudioChannelVolumeMessage();
            default -> throw new NoSuchElementException("function: %02x".formatted(functionId));
        };
    }

    /** assumed as stateless, don't use instance field in this method */
    void sequence(byte[] data, Receiver receiver) throws InvalidMfiDataException;

    /** factory for audio engine */
    class AudioEngineFactory {

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
