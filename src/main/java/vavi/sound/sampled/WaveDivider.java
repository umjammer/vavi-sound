/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import static java.lang.System.getLogger;


/**
 * WaveDivider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050401 nsano initial version <br>
 */
public interface WaveDivider {

    Logger logger = getLogger(WaveDivider.class.getName());

    /** Represents divided PCM data */
    class Chunk {
        /** Sequence number */
        public final int sequence;
        /** sampling rate */
        public final int samplingRate;
        /** sampling bits */
        public final int bits;
        /** channel number */
        public final int channels;
        /** PCM dara */
        public final byte[] buffer;
        /** */
        Chunk(int sequence, byte[] buffer, int samplingRate, int bits, int channels) {
            this.sequence = sequence;
            this.buffer = buffer;
            this.samplingRate = samplingRate;
            this.bits = bits;
            this.channels = channels;
        }
    }

    /** */
    class Factory {
        public static WaveDivider getWaveDivider(AudioInputStream audioInputStream) throws IOException, UnsupportedAudioFileException {
            // TODO use property
            if (audioInputStream.getFormat().getEncoding().equals(Encoding.PCM_SIGNED)) {
                if (audioInputStream.getFormat().getChannels() == 1) {
                    return new Pcm16BitMonauralWaveDivider(audioInputStream);
                } else {
                    return new Pcm16BitMonauralWaveDivider(new MonauralInputFilter().doFilter(audioInputStream));
                }
            } else {
logger.log(Level.INFO, "unsupported type: " + audioInputStream.getFormat());
                throw new IllegalArgumentException("only pcm mono is supported.");
            }
        }
    }

    /** */
    interface Event {
        void exec(Chunk chunk) throws IOException;
    }

    /**
     * Divides PCM data in WAVE file.
     * @param seconds time for divide
     * @param event event for each chunks
     */
    void divide(float seconds, Event event) throws IOException;
}
