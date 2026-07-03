/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteOrder;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * AudioEngine.
 * <p>
 * Used in {@link vavi.sound.mfi.vavi.sequencer.AudioDataSequencer}.
 * </p>
 * <p>
 * Currently, an implementation class of this interface should be an bean.
 * (means having a contractor without argument)
 * {@link #encode(int, int, byte[])} related should be stateless.
 * </p>
 * system property
 * <ul>
 *  <li>vavi.sound.mobile.AudioEngine.volume ... adpcm volume</li>
 * </ul>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051116 nsano initial version <br>
 */
public interface AudioEngine {

    /** Checks if the format is acceptable by this engine. */
    boolean accept(int format);

    /**
     * Sets adpcm data.
     * @param streamNumber packet id
     * @param channel channel no, -1 is means undefined
     * @param sampleRate adpcm sampling rate
     * @param bits adpcm sampling bits
     * @param channels adpcm channels
     * @param adpcm adpcm data
     * @param continued true data is continued
     */
    void setData(int streamNumber, int channel, int sampleRate, int bits, int channels, byte[] adpcm, boolean continued);

    /**
     * Stops adpcm playing.
     * @param streamNumber packet id
     */
    void stop(int streamNumber);

    /**
     * Starts adpcm playing.
     * @param streamNumber packet id
     */
    void start(int streamNumber);

    /**
     * Starts adpcm playing and stops after gateTime.
     * @param streamNumber packet id
     * @param gateTime playing time [ms], -1 play whole
     */
    void start(int streamNumber, long gateTime);

    /** close the line inside the engine */
    void close();

    /**
     * Encodes pcm data.
     * @param bits adpcm sampling bits
     * @param channels input pcm and output adpcm channels
     * @param pcm pcm data, currently 16bit only
     * @return adpcm data,
     *               when channels = 2, return L, R concatenated byte array,
     *               currently 4 bit only
     * <li> TODO pcm bits
     */
    byte[] encode(int bits, int channels, byte[] pcm);

    /**
     * Synchronization between the midi synthesizer stream and the adpcm line.
     * <p>
     * The midi synthesizer (gervill) buffers its output (~120 ms by default), the adpcm
     * line does not; delaying adpcm start/stop by that latency makes both sound together.
     * <p>
     * system property
     * <ul>
     *  <li>vavi.sound.midi.synthesizer.latency ... explicit synthesizer latency [ms], overrides auto-detection</li>
     *  <li>vavi.sound.mobile.AudioEngine.latency ... adpcm output path latency [ms], subtracted from the delay</li>
     * </ul>
     */
    class Sync {

        private static final Logger logger = System.getLogger(Sync.class.getName());

        /** true when the system property fixes the latency, auto-detection is ignored */
        private static final boolean latencyFixed = System.getProperty("vavi.sound.midi.synthesizer.latency") != null;

        /** the midi synthesizer latency [ms] */
        private static volatile long synthesizerLatencyMillis = Long.getLong("vavi.sound.midi.synthesizer.latency", 0);

        /** auto-detected latency, ignored when the system property is set explicitly */
        public static void setSynthesizerLatency(long millis) {
            if (!latencyFixed) {
                synthesizerLatencyMillis = millis;
            }
        }

        /** @return the midi synthesizer latency [ms] */
        public static long getSynthesizerLatency() {
            return synthesizerLatencyMillis;
        }

        /** @return delay [ms] to apply to adpcm playback */
        public static long getDelay() {
            long lineLatency = Long.getLong("vavi.sound.mobile.AudioEngine.latency", 0);
            return Math.max(0, synthesizerLatencyMillis - lineLatency);
        }

        /**
         * Shared by all adpcm play/stop events. single-threaded on purpose: events of
         * one sequence write to one {@link javax.sound.sampled.SourceDataLine}, so this
         * both applies the latency-compensation delay and serializes the line access,
         * keeping event order (fifo for equal delays).
         */
        private static final ScheduledThreadPoolExecutor scheduler =
                new ScheduledThreadPoolExecutor(1, r -> {
                    Thread thread = new Thread(r, "ADPCM Player");
                    thread.setDaemon(true);
                    return thread;
                });

        static {
            // the midi sequence can end (and the app exit) while a delayed start is
            // still pending: e.g. an adpcm only song fires end of track right after
            // the start event. drain pending plays before the jvm goes away, this
            // also covers System.exit() which does not wait for non daemon threads.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                scheduler.shutdown(); // still executes already queued delayed tasks
                try {
                    if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
logger.log(Level.WARNING, "adpcm still playing at jvm shutdown, cut off");
                    }
                } catch (InterruptedException ignored) {
                }
            }, "ADPCM Player shutdown"));
        }

        /** runs an adpcm play/stop task delayed by {@link #getDelay()} */
        public static void schedule(Runnable task) {
            try {
                scheduler.schedule(() -> {
                    try {
                        task.run();
                    } catch (Throwable t) {
logger.log(Level.ERROR, "adpcm task: " + t, t);
                    }
                }, getDelay(), TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.RejectedExecutionException e) {
logger.log(Level.WARNING, "adpcm task after shutdown, dropped: " + e);
            }
        }
    }

    /** */
    class Data {
        /**
         * channel no
         * TODO how to deal 0 ~ 3
         */
        public int channel;
        /** ADPCM sampling rate */
        public int sampleRate;
        /** ADPCM sampling bits */
        public int bits;
        /** ADPCM number of channels */
        public int channels;
        /** ADPCM data */
        public byte[] adpcm;
        /** ADPCM data is continued */
        public boolean continued = false;
    }

    /** monaural stereo conversion */
    class Util {
        /** left */
        private static final int L = 0;
        /** right */
        private static final int R = 1;

        /**
         * separates interleaved PCM to an array L R order.
         * @param stereo PCM stereo, currently 16bit only
         * @param bits PCM bits, TODO currently unused
         * @param byteOrder PCM 16 bit byte order, TODO currently unused
         * @return PCM monaural L, R channels
         * <pre>
         *          0  1  2  3  4
         *         +--+--+--+--+--+--+--+--+--+
         * stereo  |LH|LR|RH|RL|...
         *         +--+--+--+--+--+--+--+--+--+
         *
         *         +--+--+--+--+--+
         * mono[0] |LH|LR|...
         *         +--+--+--+--+--+
         *         +--+--+--+--+--+
         * mono[1] |RH|RL|...
         *         +--+--+--+--+--+
         * </pre>
         */
        public static byte[][] toMono(byte[] stereo, int bits, ByteOrder byteOrder) {
            byte[][] monos = new byte[2][stereo.length / 2];
            for (int i = 0; i < stereo.length / 4; i++) {
                monos[L][i * 2 + 0] = stereo[i * 4 + 0];
                monos[L][i * 2 + 1] = stereo[i * 4 + 1];
                monos[R][i * 2 + 0] = stereo[i * 4 + 2];
                monos[R][i * 2 + 1] = stereo[i * 4 + 3];
            }
            return monos;
        }

        /**
         * this seems to say interleave.
         * @param monoL ADPCM monaural L, currently 4bit only
         * @param monoR ADPCM monaural R, currently 4bit only
         * @param bits ADPCM bits, TODO currently unused
         * @param byteOrder ADPCM 4bit byte order, TODO currently unused
         * @return ADPCM stereo
         */
        public static byte[] toStereo(byte[] monoL, byte[] monoR, int bits, ByteOrder byteOrder) {
            byte[] stereo = new byte[monoL.length * 2];
            for (int i = 0; i < monoL.length; i++) {
                int l1 = (monoL[i] >> 4) & 0x0f;
                int l2 =  monoL[i] & 0x0f;
                int r1 = (monoR[i] >> 4) & 0x0f;
                int r2 =  monoR[i] & 0x0f;
                stereo[i * 2 + 0] = (byte) (((l1 << 4) | r1) & 0xff);
                stereo[i * 2 + 1] = (byte) (((l2 << 4) | r2) & 0xff);
            }
            return stereo;
        }

        /**
         * not interleaved.
         * @param monoL adpcm data L
         * @param monoR adpcm data R
         * @return L, R concatenated adpcm data
         */
        public static byte[] concatenate(byte[] monoL, byte[] monoR) {
            byte[] lr = new byte[monoL.length + monoR.length];
            System.arraycopy(monoL, 0, lr, 0, monoL.length);
            System.arraycopy(monoR, 0, lr, monoL.length, monoR.length);
            return lr;
        }
    }
}
