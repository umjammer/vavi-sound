/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.sound.sampled.AudioFormat;

import static java.lang.System.getLogger;


/**
 * Where the {@link AudioEngine}s play when they have no line of their own
 * ({@code vavi.sound.mobile.AudioEngine.output=mixer}).
 * <p>
 * By default an engine writes a stream to a {@link javax.sound.sampled.SourceDataLine}, on a
 * thread of {@link AudioEngine.Sync} and delayed by it by the wall clock. That is right for a
 * player whose midi synthesizer has a line of its own, and of no use to one which renders the
 * synthesizer itself and mixes, records or pauses what it renders: the adpcm is heard beside the
 * song and not in it, and its timing is the wall clock's, not the song's.
 * <p>
 * In this mode an engine opens no line. A start is a voice here from the moment it is asked for,
 * {@link AudioEngine.Sync} runs it at once on the thread that asks, and the player pulls the
 * voices with {@link #render} at its own rate, from its own render loop, after it has sent the
 * messages that fall before the frames it renders. What it pulls is the same pcm the line would
 * have got, the volume ({@code vavi.sound.mobile.AudioEngine.volume}) applied the same way.
 * <p>
 * Nothing else changes: the engines, their messages and {@link AudioEngine.Sync} are used the way
 * they always were, whichever mode is on.
 *
 * <p>
 * system property
 * <ul>
 *  <li>{@code vavi.sound.mobile.AudioEngine.output} ... {@code line} (default) or {@code mixer}</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public final class AudioEngineMixer {

    private static final Logger logger = getLogger(AudioEngineMixer.class.getName());

    /** system property key */
    public static final String OUTPUT_KEY = "vavi.sound.mobile.AudioEngine.output";

    private AudioEngineMixer() {
    }

    /**
     * Whether the engines play here rather than to a line of their own. Read at every use, so a
     * player may switch it on before it starts a song.
     */
    public static boolean isEnabled() {
        return "mixer".equalsIgnoreCase(System.getProperty(OUTPUT_KEY, "line"));
    }

    /** a stream being played, the pcm of which is read as it is mixed */
    private static final class Voice {
        final AudioEngine engine;
        final int streamNumber;
        final InputStream left, right;
        final float sampleRate;
        final int bytes;
        final boolean signed, bigEndian;
        final double gain;
        long framesLeft;
        boolean ended;

        // the voice's rate to the output rate
        double frac;
        int prevL, prevR, curL, curR;
        boolean primed;

        Voice(AudioEngine engine, int streamNumber, InputStream[] iss, int channels, AudioFormat format, long gateFrames, double gain) {
            this.engine = engine;
            this.streamNumber = streamNumber;
            this.left = iss[0];
            this.right = channels == 1 ? null : iss[1];
            this.sampleRate = format.getSampleRate();
            this.bytes = Math.max(1, format.getSampleSizeInBits() / 8);
            this.signed = !AudioFormat.Encoding.PCM_UNSIGNED.equals(format.getEncoding());
            this.bigEndian = format.isBigEndian();
            this.framesLeft = gateFrames;
            this.gain = gain;
        }

        /** one sample of a channel, as 16 bit, or {@link Integer#MIN_VALUE} at the end */
        private int sample(InputStream is) throws IOException {
            if (bytes == 1) {
                int b = is.read();
                if (b < 0) return Integer.MIN_VALUE;
                return signed ? ((byte) b) << 8 : (b - 0x80) << 8;
            }
            int b0 = is.read();
            int b1 = is.read();
            if (b0 < 0 || b1 < 0) return Integer.MIN_VALUE;
            int v = bigEndian ? (short) ((b0 << 8) | b1) : (short) ((b1 << 8) | b0);
            for (int i = 2; i < bytes; i++) is.read(); // wider than 16 bit, the high part is taken
            return v;
        }

        /** steps the voice by one of its own frames */
        private void next() {
            prevL = curL;
            prevR = curR;
            if (ended || framesLeft <= 0) {
                ended = true;
                curL = curR = 0;
                return;
            }
            try {
                int l = sample(left);
                int r = right == null ? l : sample(right);
                if (l == Integer.MIN_VALUE || r == Integer.MIN_VALUE) {
                    ended = true;
                    curL = curR = 0;
                    return;
                }
                curL = l;
                curR = r;
                framesLeft--;
            } catch (IOException e) {
logger.log(Level.WARNING, "stream " + streamNumber + ": " + e);
                ended = true;
                curL = curR = 0;
            }
        }

        /**
         * Adds the voice to the frames, stepping it at {@code step} of its frames a frame.
         *
         * @return false when the voice is over
         */
        boolean mix(int[] left, int[] right, int frames, double step) {
            if (!primed) {
                next();
                primed = true;
            }
            for (int i = 0; i < frames; i++) {
                frac += step;
                while (frac >= 1.0) {
                    frac -= 1.0;
                    next();
                }
                if (ended && curL == 0 && prevL == 0 && curR == 0 && prevR == 0) {
                    return false;
                }
                left[i] += (int) ((prevL + (curL - prevL) * frac) * gain);
                right[i] += (int) ((prevR + (curR - prevR) * frac) * gain);
            }
            return !ended;
        }
    }

    private static final List<Voice> voices = new ArrayList<>();

    /** mixing buffers, grown as they are asked for */
    private static int[] mixL = new int[0], mixR = new int[0];

    /**
     * Starts a stream of an engine, the one of that number already playing is replaced.
     *
     * @param iss the pcm of the stream, [0] left or mono, [1] right
     * @param channels 1 or 2
     * @param format the pcm format of {@code iss}, as the line would have been opened at
     * @param gateFrames frames to play, {@link Long#MAX_VALUE} for the whole stream
     * @param gain linear, what the line's volume would have been
     */
    static synchronized void start(AudioEngine engine, int streamNumber, InputStream[] iss, int channels, AudioFormat format, long gateFrames, double gain) {
        remove(engine, streamNumber);
        voices.add(new Voice(engine, streamNumber, iss, channels, format, gateFrames, gain));
logger.log(Level.DEBUG, "mixer start: no: " + streamNumber + ", " + format + ", gateFrames: " + (gateFrames == Long.MAX_VALUE ? "all" : gateFrames) + ", voices: " + voices.size());
    }

    /** stops a stream of an engine, the part not rendered yet is dropped */
    static synchronized void stop(AudioEngine engine, int streamNumber) {
        if (remove(engine, streamNumber)) {
logger.log(Level.DEBUG, "mixer stop: no: " + streamNumber);
        }
    }

    /** stops every stream of an engine */
    static synchronized void close(AudioEngine engine) {
        voices.removeIf(v -> v.engine == engine);
    }

    private static boolean remove(AudioEngine engine, int streamNumber) {
        return voices.removeIf(v -> v.engine == engine && v.streamNumber == streamNumber);
    }

    /** whether any stream is playing */
    public static synchronized boolean isPlaying() {
        return !voices.isEmpty();
    }

    /** stops every stream, as a player does between songs */
    public static synchronized void clear() {
        voices.clear();
    }

    /**
     * Mixes what is playing into a buffer: adds to it, so the player renders its synthesizer
     * first and the adpcm over it.
     *
     * @param buffer 16 bit stereo interleaved
     * @param offset in shorts
     * @param frames frames to mix
     * @param sampleRate the rate of the buffer
     */
    public static synchronized void render(short[] buffer, int offset, int frames, float sampleRate) {
        if (voices.isEmpty()) return;
        if (mixL.length < frames) {
            mixL = new int[frames];
            mixR = new int[frames];
        }
        java.util.Arrays.fill(mixL, 0, frames, 0);
        java.util.Arrays.fill(mixR, 0, frames, 0);
        for (Iterator<Voice> i = voices.iterator(); i.hasNext(); ) {
            Voice voice = i.next();
            if (!voice.mix(mixL, mixR, frames, voice.sampleRate / sampleRate)) {
                i.remove();
logger.log(Level.DEBUG, "mixer end: no: " + voice.streamNumber);
            }
        }
        for (int i = 0; i < frames; i++) {
            int p = offset + i * 2;
            buffer[p] = (short) Math.clamp(buffer[p] + mixL[i], Short.MIN_VALUE, Short.MAX_VALUE);
            buffer[p + 1] = (short) Math.clamp(buffer[p + 1] + mixR[i], Short.MIN_VALUE, Short.MAX_VALUE);
        }
    }

    /**
     * {@link #render(short[], int, int, float)} for a player of separate channels.
     *
     * @param left added to, 16 bit range
     * @param right added to, 16 bit range
     */
    public static synchronized void render(int[] left, int[] right, int frames, float sampleRate) {
        if (voices.isEmpty()) return;
        for (Iterator<Voice> i = voices.iterator(); i.hasNext(); ) {
            Voice voice = i.next();
            if (!voice.mix(left, right, frames, voice.sampleRate / sampleRate)) {
                i.remove();
            }
        }
    }
}
