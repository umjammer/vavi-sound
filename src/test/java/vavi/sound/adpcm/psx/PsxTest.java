/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.psx;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import vavi.io.SeekableDataInputStream;
import vavi.sound.adpcm.psx.Psx.VGMStreamChannel;
import vavi.sound.adpcm.psx.Psx.VgmStream;
import vavi.sound.adpcm.psx.Psx.layout_config_t;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.lang.System.getLogger;
import static vavi.sound.SoundUtil.volume;
import static vavi.sound.adpcm.psx.Psx.decode_do_loop;
import static vavi.sound.adpcm.psx.Psx.decode_get_samples_per_frame;
import static vavi.sound.adpcm.psx.Psx.decode_get_samples_to_do;
import static vavi.sound.adpcm.psx.Psx.setup_helper;
import static vavi.sound.adpcm.psx.Psx.update_default_values;
import static vavi.sound.adpcm.psx.Psx.update_loop_values;
import static vavi.sound.adpcm.psx.Psx.update_offsets;


/**
 * PsxTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-04-23 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class PsxTest {

    private static final Logger logger = getLogger(PsxTest.class.getName());

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

    @Property(name = "adpcm.psx")
    String adpcm = "src/test/resources/test.psx";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

Debug.print("volume: " + volume);
    }

    @Test
    @DisplayName("prototype")
    void test1() throws Exception {
        Path path = Path.of(adpcm);
Debug.println("path: " + path);
        SeekableDataInputStream dis = new SeekableDataInputStream(Files.newByteChannel(path));
        int size = Math.toIntExact(Files.size(path));
Debug.println("size: " + size);
//        assertTrue(Psx.checkFormat(dis, size, 0, size));
        int samples = Psx.bytesToSamples(size, 2);
Debug.println("samples: " + samples);
        VGMStreamChannel stream = new VGMStreamChannel(dis, 0, 1, -1);
        short[] buf = new short[samples];
        Psx.decodePSX(stream, buf, 0, 0, samples, true, 0);
    }

    /**
     * flat
     * TODO sounds mostly
     * @see "https://github.com/vgmstream/vgmstream/blob/9db666eb0c2275334ac68a2537a6a689aee792f2/src/layout/flat.c#L22"
     */
    @Test
    @DisplayName("raw api")
    void test2() throws Exception {
        Path path = Path.of(adpcm);
Debug.println("path: " + path + ", " + Files.size(path));

        VgmStream stream = PsHeaderless.initVgmstreamPsHeaderless(path);
Debug.println("sampleRate: " + stream.sampleRate + ", channels: " + stream.channels);

        // for audio playback
        AudioFormat audioFormat = new AudioFormat(stream.sampleRate, 16, stream.channels, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(audioFormat);
        line.open(audioFormat);
        line.start();
        volume(line, volume);

        int filled = 0;
        int samples = stream.numSamples;
Debug.print("samples: " + samples);

        int samples_per_frame = decode_get_samples_per_frame(stream);
        int samples_this_block = stream.numSamples; // do all samples if possible

        /* write samples */
        while (filled < samples) {

            int samples_to_do = decode_get_samples_to_do(samples_this_block, samples_per_frame, stream);
            if (samples_to_do > samples - filled)
                samples_to_do = samples - filled;

            if (samples_to_do <= 0) { // when decoding more than num_samples
                throw new IllegalStateException("FLAT: wrong samples_to_do: " + samples_to_do);
            }

            short[][] buf = new short[stream.channels][samples_to_do];
            for (int ch = 0; ch < stream.channels; ch++) {
//                logger.log(Level.TRACE, "decode: ch: " + ch);
                Psx.decodePSX(stream.ch[ch], buf[ch], 1, stream.samplesIntoBlock, samples_to_do,
                        false, stream.codecConfig);
            }

            // interleave
            byte[] pcm = new byte[samples_to_do * stream.channels * Short.BYTES];
            ByteBuffer bb = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < samples_to_do; i++) {
                for (int ch = 0; ch < stream.channels; ch++) {
                    bb.putShort(buf[ch][i]);
                }
            }
//Debug.print("\n" + StringUtil.getDump(pcm, 128));
            line.write(pcm, 0, pcm.length);

            filled += samples_to_do;
            stream.currentSample += samples_to_do;
            stream.samplesIntoBlock += samples_to_do;
//            logger.log(Level.TRACE, "filled: %d, samples: %d".formatted(filled, samples));
        }

        line.drain();
        line.stop();
        line.close();
    }

    /**
     * interleave
     * @see "https://github.com/vgmstream/vgmstream/blob/9db666eb0c2275334ac68a2537a6a689aee792f2/src/layout/interleave.c#L146"
     */
    @Test
    @DisplayName("raw api")
    void test3() throws Exception {
        Path path = Path.of(adpcm);
Debug.println("path: " + path + ", " + Files.size(path));

        VgmStream stream = PsHeaderless.initVgmstreamPsHeaderless(path);
Debug.println("sampleRate: " + stream.sampleRate + ", channels: " + stream.channels);

        // for audio playback
        AudioFormat audioFormat = new AudioFormat(stream.sampleRate, 16, stream.channels, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(audioFormat);
        line.open(audioFormat);
        line.start();
        volume(line, volume);

        int filled = 0;
        int samples = stream.numSamples;

        layout_config_t layout = new layout_config_t();
        if (!setup_helper(layout, stream)) {
            throw new IllegalStateException("INTERLEAVE: wrong config found");
        }

        int[] samples_per_frame = new int[1], samples_this_block = new int[1];
        update_default_values(layout, stream, samples_per_frame, samples_this_block);
        // mono interleaved stream with no layout set, just behave like flat layout
        if (samples_this_block[0] == 0 && stream.channels == 1)
            samples_this_block[0] = stream.numSamples;
logger.log(Level.DEBUG, "samples_per_frame=%d, samples_this_block=%d".formatted(samples_per_frame[0], samples_this_block[0]));

        // write samples
        while (filled < samples) {

            if (stream.loopFlag && decode_do_loop(stream)) {
                // handle looping, restore standard interleave sizes
                update_loop_values(layout, stream, samples_per_frame, samples_this_block);
                continue;
            }

            int samples_to_do = decode_get_samples_to_do(samples_this_block[0], samples_per_frame[0], stream);
            if (samples_to_do > samples - filled)
                samples_to_do = samples - filled;

            if (samples_to_do <= 0) { // when decoding more than num_samples
                throw new IllegalStateException("FLAT: wrong samples_to_do\n");
            }

            short[][] buf = new short[stream.channels][16 * Short.BYTES * stream.channels];
            for (int ch = 0; ch < stream.channels; ch++) {
                // FIXED: channelSpacing must be 1 here because buf[ch] is a mono buffer for that channel only.
                // We interleave them manually in the loop below.
                Psx.decodePSX(stream.ch[ch], buf[ch], 1, stream.samplesIntoBlock, samples_to_do,
                        false, stream.codecConfig);
            }

            // interleave
            byte[] pcm = new byte[samples_to_do * stream.channels * Short.BYTES];
            ByteBuffer bb = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < samples_to_do; i++) {
                for (int ch = 0; ch < stream.channels; ch++) {
                    bb.putShort(buf[ch][i]);
                }
            }
//Debug.print("\n" + StringUtil.getDump(pcm, 128));
            line.write(pcm, 0, pcm.length);

            filled += samples_to_do;
            stream.currentSample += samples_to_do;
            stream.samplesIntoBlock += samples_to_do;
//            logger.log(Level.TRACE, "filled: %d, samples: %d".formatted(filled, samples));

            // move to next interleaved block when all samples are consumed
            if (stream.samplesIntoBlock == samples_this_block[0]) {
                update_offsets(layout, stream, samples_per_frame, samples_this_block);
            }
        }

        line.drain();
        line.stop();
        line.close();
    }
}
