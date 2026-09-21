/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Method;
import java.util.Map;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import static java.lang.System.getLogger;


/**
 * Plays a synthesizer which can be rendered as a stream (gervill, {@code com.sun.media.sound.AudioSynthesizer})
 * through a line of its own here, with the adpcm of the {@link AudioEngine}s mixed in, so the two are
 * heard in step: the adpcm used to go to a line of its own, started by the wall clock, a latency
 * guessed from the synthesizer's away from the notes.
 * <p>
 * {@code com.sun.media.sound} is not exported, so this is reached by reflection and only when the
 * jvm exports it ({@code --add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED}); else, or when
 * {@code vavi.sound.mobile.AudioEngine.output=line}, {@link #open} answers null and the synthesizer is
 * to be opened as it always was.
 * <p>
 * The listener's volume is this line's, not the synthesizer's: the streams mixed in here are as
 * much of what is heard as the synthesizer is, so the two have to be scaled together or a song
 * sounds different at two volumes. {@link #receiver} takes the universal master volume for that
 * and a player hands out the receiver it wraps, see {@link #gain}.
 * <p>
 * system property
 * <ul>
 *  <li>{@code vavi.sound.mobile.MixingLine.buffer} ... the line's buffer [ms], default 100</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public final class MixingLine implements AutoCloseable {

    private static final Logger logger = getLogger(MixingLine.class.getName());

    private static final float RATE = 44100;

    /** frames rendered at a time, a message waits at most this long */
    private static final int BLOCK = 256;

    private final Synthesizer synthesizer;
    private final AudioInputStream stream;
    private final SourceDataLine line;

    /** the listener's volume over the whole mix, the synthesizer and the streams alike */
    private volatile double gain = 1;
    private final Thread thread;
    private volatile boolean running = true;
    private boolean closed;

    private MixingLine(Synthesizer synthesizer, AudioInputStream stream, SourceDataLine line) {
        this.synthesizer = synthesizer;
        this.stream = stream;
        this.line = line;
        this.thread = new Thread(this::run, "MixingLine " + synthesizer.getDeviceInfo().getName());
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
    }

    /** {@code AudioSynthesizer#openStream(AudioFormat, Map)} when it can be called, else null */
    private static Method openStream(Synthesizer synthesizer) {
        try {
            Class<?> audioSynthesizer = Class.forName("com.sun.media.sound.AudioSynthesizer");
            if (!audioSynthesizer.isInstance(synthesizer)) return null;
            if (!audioSynthesizer.getModule().isExported(audioSynthesizer.getPackageName(), MixingLine.class.getModule())) {
logger.log(Level.DEBUG, "com.sun.media.sound is not exported, " + synthesizer.getDeviceInfo().getName() + " keeps its own line");
                return null;
            }
            return audioSynthesizer.getMethod("openStream", AudioFormat.class, Map.class);
        } catch (ReflectiveOperationException | RuntimeException e) {
logger.log(Level.DEBUG, "no stream of " + synthesizer.getDeviceInfo().getName() + ": " + e);
            return null;
        }
    }

    /**
     * Opens the synthesizer as a stream played here, the adpcm mixed in.
     *
     * @return null when it cannot be, the synthesizer is not opened then: open it as before
     * @throws MidiUnavailableException the synthesizer or the line would not open
     */
    public static MixingLine open(Synthesizer synthesizer) throws MidiUnavailableException {
        Method openStream = openStream(synthesizer);
        if (openStream == null) return null;
        if (!AudioEngineMixer.attach()) return null;
        AudioFormat format = new AudioFormat(RATE, 16, 2, true, false);
        AudioInputStream stream = null;
        try {
            stream = (AudioInputStream) openStream.invoke(synthesizer, format, null);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            int millis = Integer.getInteger("vavi.sound.mobile.MixingLine.buffer", 100);
            line.open(format, (int) (RATE * millis / 1000) * format.getFrameSize());
            line.start();
            MixingLine mixingLine = new MixingLine(synthesizer, stream, line);
            mixingLine.thread.start();
logger.log(Level.DEBUG, "mixing line: " + synthesizer.getDeviceInfo().getName() + ", buffer: " + line.getBufferSize());
            return mixingLine;
        } catch (ReflectiveOperationException | LineUnavailableException | RuntimeException e) {
            AudioEngineMixer.detach();
            if (stream != null) {
                synthesizer.close();
            }
            Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : e;
            if (cause instanceof MidiUnavailableException mue) throw mue;
            throw (MidiUnavailableException) new MidiUnavailableException(String.valueOf(cause)).initCause(cause);
        }
    }

    private void run() {
        byte[] bytes = new byte[BLOCK * 4];
        short[] pcm = new short[BLOCK * 2];
        try {
            while (running) {
                int n = 0;
                while (n < bytes.length) {
                    int r = stream.read(bytes, n, bytes.length - n);
                    if (r < 0) {
                        running = false;
                        break;
                    }
                    n += r;
                }
                int frames = n / 4;
                for (int i = 0; i < frames * 2; i++) {
                    pcm[i] = (short) ((bytes[i * 2] & 0xff) | (bytes[i * 2 + 1] << 8));
                }
                AudioEngineMixer.render(pcm, 0, frames, RATE, adpcmGain());
                double gain = this.gain;
                for (int i = 0; i < frames * 2; i++) {
                    int v = gain == 1 ? pcm[i] : Math.clamp((int) (pcm[i] * gain), -0x8000, 0x7fff);
                    bytes[i * 2] = (byte) v;
                    bytes[i * 2 + 1] = (byte) (v >> 8);
                }
                line.write(bytes, 0, frames * 4);
            }
        } catch (IOException e) {
            if (running) {
logger.log(Level.WARNING, "mixing line: " + e);
            }
        }
    }

    /**
     * How loud the streams are against the synthesizer. They come at the level they were stored at
     * ({@link AudioEngineMixer}), so this says what they are worth here, and what it says is what
     * the volume of a line of their own would have made of them - the same property and the same
     * default - so that a song sounds as it always has and a setting of it still works.
     * <p>
     * It is not the listener's volume: that one goes to the synthesizer and so misses the streams
     * mixed in here, which is why a song sounds different at two volumes. A player which wants the
     * two to stay together scales the line ({@link #getLine}) instead.
     */
    private static double adpcmGain() {
        return Double.parseDouble(System.getProperty("vavi.sound.mobile.AudioEngine.volume", "0.2"));
    }

    /** @param gain the listener's volume over the whole mix, 0 ~ 1 */
    public void gain(double gain) {
        this.gain = gain;
    }

    /**
     * Wraps the receiver of the synthesizer so that the universal master volume is this line's.
     * <p>
     * It is the listener's volume and the synthesizer is only a part of what the listener hears
     * here, so letting it reach the synthesizer would scale that part alone and leave the streams
     * where they are - a song would not sound the same at two volumes. The message is taken by
     * {@link #gain} instead and does not go on, everything else does.
     *
     * @param delegate what the synthesizer plays, the rest of the messages going to it
     */
    public Receiver receiver(Receiver delegate) {
        return new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (message instanceof SysexMessage sysex) {
                    byte[] data = sysex.getMessage();
                    // f0 7f dd 04 01 ll mm f7
                    if (data.length >= 7 && (data[0] & 0xff) == 0xf0 && data[1] == 0x7f
                            && data[3] == 0x04 && data[4] == 0x01) {
                        gain(((data[5] & 0x7f) | ((data[6] & 0x7f) << 7)) / 16383d);
logger.log(Level.DEBUG, "mixing line: the listener's volume is of the whole mix: %3.0f".formatted(gain * 127));
                        return;
                    }
                }
                delegate.send(message, timeStamp);
            }

            @Override
            public void close() {
                delegate.close();
            }
        };
    }

    /** the line, for its controls (volume) */
    public SourceDataLine getLine() {
        return line;
    }

    /** closes the line and the synthesizer */
    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        running = false;
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        line.stop();
        line.close();
        synthesizer.close(); // the stream with it
        AudioEngineMixer.detach();
    }
}
