/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.ServiceLoader;

import vavi.sound.adpcm.AdpcmInputStreamFactory;
import vavi.sound.adpcm.ccitt.G721InputStream;
import vavi.sound.adpcm.ccitt.G721OutputStream;
import vavi.sound.adpcm.ccitt.G723_16InputStream;
import vavi.sound.adpcm.ima.Ima2InputStream;

import static java.lang.System.getLogger;


/**
 * Fuetrek AudioEngine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020903 nsano initial version <br>
 */
public class FuetrekAudioEngine extends BasicAudioEngine {

    private static final Logger logger = getLogger(FuetrekAudioEngine.class.getName());

    /** */
    private static final int MAX_ID = 16;

    /**
     * The roughness comparison is not statistically useful for very short
     * clips.  In particular, a short 2-bit G.723 resource can look smoother
     * when decoded as 4-bit G.721 simply because there are too few samples.
     * Keep the format-defined G.723 choice for those clips.
     */
    private static final int MIN_AUTO_SAMPLES = 256;

    /**
     * <pre>
     *  (c: continued, e: end)
     *  from Function131, Function134
     *   0 Lc + Lc + Le
     *   1 Rc + Rc + Re
     *  from AudioDataMessage
     *   0 L + R
     * </pre>
     */
    public FuetrekAudioEngine() {
        data = new Data[MAX_ID];
    }

    @Override
    public boolean accept(int format) {
        return format == 0x81; // ADPCM Type2
    }

    @Override
    protected int getChannels(int streamNumber) {
        int channels = 1;
        if (data[streamNumber].channel != -1) {
            // from MachineDependent
            if (streamNumber % 2 == 1 && data[streamNumber].channel % 2 == 1 && (data[streamNumber - 1] != null && data[streamNumber - 1].channel % 2 == 0)) {
logger.log(Level.DEBUG, "always used: no: " + streamNumber + ", ch: " + data[streamNumber].channel);
                return -1;
            }

            if (streamNumber % 2 == 0 && data[streamNumber].channel % 2 == 0 && (data[streamNumber + 1] != null && data[streamNumber + 1].channel % 2 == 1)) {
                channels = 2;
            }
        } else {
            // from AudioData
            // The ADPM subchunk explicitly records whether this stream is mono
            // or stereo.  Consecutive adat chunks are independent streams: in
            // particular, MFi files commonly put a 16 kHz/4-bit stream next to
            // a 32 kHz/2-bit one.  Treating those as an implicit L/R pair both
            // selects the wrong decoder for the right channel and produces
            // audible noise.
            channels = data[streamNumber].channels;
        }
        return channels;
    }

    @Override
    protected InputStream[] getInputStreams(int streamNumber, int channels) {
        InputStream[] iss = new InputStream[2];
        if (data[streamNumber].channels == 1) {
            if (data[streamNumber].bits == 4) {
                InputStream in = new ByteArrayInputStream(data[streamNumber].adpcm);
                iss[0] = get4BitInputStream(in);
                if (channels != 1) {
                    InputStream inR = new ByteArrayInputStream(data[streamNumber + 1].adpcm);
                    iss[1] = get4BitInputStream(inR);
                }
            } else if (data[streamNumber].bits == 2) {
                InputStream in = new ByteArrayInputStream(data[streamNumber].adpcm);
                iss[0] = get2BitInputStream(streamNumber, in);
                if (channels != 1) {
                    InputStream inR = new ByteArrayInputStream(data[streamNumber + 1].adpcm);
                    iss[1] = get2BitInputStream(streamNumber + 1, inR);
                }
            }
        } else {
            if (data[streamNumber].bits == 4) {
                InputStream in = new ByteArrayInputStream(data[streamNumber].adpcm, 0, data[streamNumber].adpcm.length / 2);
                iss[0] = get4BitInputStream(in);
                InputStream inR = new ByteArrayInputStream(data[streamNumber].adpcm, data[streamNumber].adpcm.length / 2, data[streamNumber].adpcm.length / 2);
                iss[1] = get4BitInputStream(inR);
            } else if (data[streamNumber].bits == 2) {
                InputStream in = new ByteArrayInputStream(data[streamNumber].adpcm, 0, data[streamNumber].adpcm.length / 2);
                iss[0] = get2BitInputStream(streamNumber, in);
                InputStream inR = new ByteArrayInputStream(data[streamNumber].adpcm, data[streamNumber].adpcm.length / 2, data[streamNumber].adpcm.length / 2);
                iss[1] = get2BitInputStream(streamNumber + 1, inR);
            }
        }
        return iss;
    }

    @Override
    protected String getDecoderName(int streamNumber, int bits, byte[] adpcm) {
        if (bits == 2) {
            String decoder = configured2BitDecoder(streamNumber);
            if (decoder.equals("auto")) {
                try {
                    return auto2BitDecoder(adpcm);
                } catch (IOException e) {
                    return "auto(error)";
                }
            }
            return decoder;
        }
        if (bits == 4) {
            return System.getProperty("vavi.sound.mobile.FuetrekAudioEngine.decoder", "g721").toLowerCase();
        }
        return "unknown";
    }

    /** Selects the code-word packing used by a two-bit MFi ADPCM stream. */
    private static InputStream get2BitInputStream(int streamNumber, InputStream in) {
        String decoder = configured2BitDecoder(streamNumber);
        if (decoder.equals("ima2") || decoder.equals("ima")) {
            return new Ima2InputStream(in, ByteOrder.LITTLE_ENDIAN);
        }
        if (decoder.equals("auto")) {
            try {
                byte[] compressed = in.readAllBytes();
                String selected = auto2BitDecoder(compressed);
                byte[] decoded = selected.equals("g721") ?
                        decodeAll(new G721InputStream(new ByteArrayInputStream(compressed), ByteOrder.LITTLE_ENDIAN)) :
                        decodeAll(new G723_16InputStream(new ByteArrayInputStream(compressed),
                                ByteOrder.LITTLE_ENDIAN,
                                ByteOrder.LITTLE_ENDIAN));
                // A few DoCoMo Type-2 resources are tagged as 2-bit but are
                // actually 4-bit G.721 packets.  Their G.723 expansion has
                // near-white-noise roughness; retain G.723 for normal streams.
                return new ByteArrayInputStream(decoded);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        String order = System.getProperty("vavi.sound.mobile.FuetrekAudioEngine.g723BitOrder." + streamNumber);
        if (order == null) {
            order = System.getProperty("vavi.sound.mobile.FuetrekAudioEngine.g723BitOrder", "little");
        }
        order = order
                .toLowerCase(Locale.ROOT);
        return switch (order) {
            case "little", "le" -> new G723_16InputStream(in, ByteOrder.LITTLE_ENDIAN, ByteOrder.LITTLE_ENDIAN);
            case "big", "be" -> new G723_16InputStream(in, ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN);
            default -> throw new IllegalArgumentException("unsupported G.723 bit order: " + order);
        };
    }

    private static String configured2BitDecoder(int streamNumber) {
        String decoder = System.getProperty("vavi.sound.mobile.FuetrekAudioEngine.g723Decoder." + streamNumber);
        if (decoder == null) {
            // MFi Type-2's 2-bit ADPCM is the CCITT G.723 format.  Header
            // metadata does not contain a codec tag, so statistical guessing
            // is unsafe (and misclassifies long resources such as Track 5 of
            // WALKURENRITT).  Keep the format-defined decoder by default;
            // callers can still opt into the experimental heuristic with
            // -Dvavi.sound.mobile.FuetrekAudioEngine.g723Decoder=auto.
            decoder = System.getProperty("vavi.sound.mobile.FuetrekAudioEngine.g723Decoder", "g723");
        }
        return decoder.toLowerCase();
    }

    /** Runs the same score used by playback, but without consuming the stored stream. */
    private static String auto2BitDecoder(byte[] compressed) throws IOException {
        byte[] g723 = decodeAll(new G723_16InputStream(new ByteArrayInputStream(compressed),
                ByteOrder.LITTLE_ENDIAN,
                ByteOrder.LITTLE_ENDIAN));
        byte[] g721 = decodeAll(new G721InputStream(new ByteArrayInputStream(compressed),
                ByteOrder.LITTLE_ENDIAN));
        if (g723.length / 2 < MIN_AUTO_SAMPLES) {
            logger.log(Level.DEBUG, "Type-2 ADPCM: short stream ({0} samples), retaining G.723",
                    g723.length / 2);
            return "g723";
        }
        double g723Score = roughness(g723);
        double g721Score = roughness(g721);
        if (g721Score + 0.12 < g723Score) {
            logger.log(Level.DEBUG, "Type-2 ADPCM: selecting G.721 fallback (scores {0}, {1})",
                    g721Score, g723Score);
            return "g721";
        }
        return "g723";
    }

    private static byte[] decodeAll(InputStream in) throws IOException {
        try (InputStream stream = in) {
            return stream.readAllBytes();
        }
    }

    /** Normalized first-difference energy; white-noise-like data scores high. */
    private static double roughness(byte[] pcm) {
        if (pcm.length < 4) return Double.POSITIVE_INFINITY;
        double energy = 0;
        double difference = 0;
        int previous = 0;
        int samples = 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int sample = (short) ((pcm[i] & 0xff) | (pcm[i + 1] << 8));
            energy += (double) sample * sample;
            if (samples++ != 0) difference += Math.abs(sample - previous);
            previous = sample;
        }
        return difference / Math.max(1, samples - 1) /
                Math.sqrt(energy / Math.max(1, samples));
    }

    /**
     * Selects the 4-bit decoder.  MFi Type 2 is normally CCITT G.721; the
     * alternate decoders are intentionally opt-in so anomalous legacy files
     * can be auditioned without changing normal playback.
     */
    private static InputStream get4BitInputStream(InputStream in) {
        String decoder = System.getProperty("vavi.sound.mobile.FuetrekAudioEngine.decoder", "g721")
                .toLowerCase();
        for (AdpcmInputStreamFactory ais : ServiceLoader.load(AdpcmInputStreamFactory.class)) {
            if (ais.getClass().getName().toLowerCase().contains(decoder)) {
                return ais.factory(in);
            }
        }
        throw new IllegalArgumentException("unsupported 4-bit decoder: " + decoder);
    }

    // ----

    @Override
    protected OutputStream getOutputStream(OutputStream os) {
        return new G721OutputStream(os, ByteOrder.LITTLE_ENDIAN);
    }
}
