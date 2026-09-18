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
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
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
                AudioEngineMixer.render(pcm, 0, frames, RATE);
                for (int i = 0; i < frames * 2; i++) {
                    bytes[i * 2] = (byte) pcm[i];
                    bytes[i * 2 + 1] = (byte) (pcm[i] >> 8);
                }
                line.write(bytes, 0, frames * 4);
            }
        } catch (IOException e) {
            if (running) {
logger.log(Level.WARNING, "mixing line: " + e);
            }
        }
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
