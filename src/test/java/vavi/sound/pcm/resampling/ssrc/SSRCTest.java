/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.ssrc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import vavi.io.LittleEndianDataInputStream;
import vavi.io.LittleEndianDataOutputStream;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.sound.SoundUtil.volume;


/**
 * SSRCTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060127 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class SSRCTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property
    String ssrc = "src/test/resources/vavi/sound/pcm/resampling/ssrc/44100.wav";

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

    static final Path outPath = Paths.get("tmp/out.vavi.wav");
    static final String inFile = "src/test/resources/vavi/sound/pcm/resampling/ssrc/44100.wav";
    static final String correctDownFile = "src/test/resources/vavi/sound/pcm/resampling/ssrc/down.wav";
    static final String correctUpFile = "src/test/resources/vavi/sound/pcm/resampling/ssrc/up.wav";

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");

    @BeforeAll
    static void setUp() throws Exception {
        Path tmp = outPath.getParent();
        if (!Files.exists(tmp)) {
            Files.createDirectory(tmp);
        }
    }

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @DisplayName("down sample, call by main")
    public void test1() throws Exception {
        SSRC.main(new String[] {"--rate", "8000", "--twopass", "--normalize", inFile, outPath.toString()});

        if (onIde) {
            AudioInputStream ais = AudioSystem.getAudioInputStream(outPath.toFile());
            AudioFormat format = ais.getFormat();
Debug.println(format);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            volume(line, volume);
            line.start();
            byte[] buf = new byte[1024];
            int l;
            while (true) {
                l = ais.read(buf, 0, buf.length);
                if (l < 0)
                    break;
                line.write(buf, 0, l);
            }
            line.drain();
            line.stop();
            line.close();
        }

        assertEquals(Checksum.getChecksum(Paths.get(correctDownFile)), Checksum.getChecksum(outPath));
    }

    @Test
    @DisplayName("up sample, call by main")
    public void test3() throws Exception {
        SSRC.main(new String[] { "--rate", "48000", "--twopass", "--normalize", ssrc, outPath.toString() });

        if (onIde) {
            AudioInputStream ais = AudioSystem.getAudioInputStream(outPath.toFile());
            AudioFormat format = ais.getFormat();
Debug.println(format);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            volume(line, volume);
            line.start();
            byte[] buf = new byte[1024];
            int l;
            while (true) {
                l = ais.read(buf, 0, buf.length);
                if (l < 0)
                    break;
                line.write(buf, 0, l);
            }
            line.drain();
            line.stop();
            line.close();
        }

        assertEquals(Checksum.getChecksum(Paths.get(correctUpFile)), Checksum.getChecksum(outPath));
    }

    @Test
    public void test2() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianDataOutputStream leos = new LittleEndianDataOutputStream(baos);
        leos.writeDouble(0.123456789);
        leos.close();
Debug.println("1:\n" + StringUtil.getDump(baos.toByteArray()));
        //
        byte[] buf = new byte[8];
        writeDouble(buf, 0, 0.123456789);
Debug.println("2:\n" + StringUtil.getDump(buf));
        assertArrayEquals(baos.toByteArray(), buf);
        //
        LittleEndianDataInputStream leis = new LittleEndianDataInputStream(new ByteArrayInputStream(buf));
        double d = leis.readDouble();
        leis.close();
Debug.printf("3: %f\n", d);
        assertEquals(0.123456789, d, 0.000000001);
    }

    /** */
    private static void writeDouble(byte[] buffer, int offset, double value) {
        long l = Double.doubleToLongBits(value);
        buffer[offset * 8 + 0] = (byte)  (l & 0x00000000000000ffL);
        buffer[offset * 8 + 1] = (byte) ((l & 0x000000000000ff00L) >>  8);
        buffer[offset * 8 + 2] = (byte) ((l & 0x0000000000ff0000L) >> 16);
        buffer[offset * 8 + 3] = (byte) ((l & 0x00000000ff000000L) >> 24);
        buffer[offset * 8 + 4] = (byte) ((l & 0x000000ff00000000L) >> 32);
        buffer[offset * 8 + 5] = (byte) ((l & 0x0000ff0000000000L) >> 40);
        buffer[offset * 8 + 6] = (byte) ((l & 0x00ff000000000000L) >> 48);
        buffer[offset * 8 + 7] = (byte) ((l & 0xff00000000000000L) >> 56);
    }

    @Test
    @DisplayName("call by stream")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    public void test4() throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(Paths.get(ssrc).toFile());
        AudioFormat format = ais.getFormat();
        AudioFormat outFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            8000,
            16,
            format.getChannels(),
            format.getFrameSize(),
            format.getFrameRate(),
            format.isBigEndian());
Debug.println(format);
Debug.println(outFormat);

        InputStream in = new SSRCInputStream(format, outFormat, ais);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, outFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(outFormat);
        // volume
        volume(line, volume);
        line.start();
        byte[] buf = new byte[0x10000];
        int f = outFormat.getFrameSize();
        int l = 0;
        while (true) {
            int r = in.read(buf, l, buf.length - l);
            if (r < 0)
                break;
            l += r;
            // we need to keep line.write buffer size is multiply of "f"
            int a = l / f * f;
            line.write(buf, 0, a);
            System.arraycopy(buf, a, buf, 0, l - a);
            l -= a;
        }
        in.close();
        line.drain();
        line.stop();
        line.close();
    }

    @Test
    @DisplayName("stream gives the same result as the filter")
    public void test5() throws Exception {
        // 16 bit stereo, 3 sec and a few frames
        byte[] pcm = Arrays.copyOfRange(Files.readAllBytes(Paths.get(inFile)), 44, 44 + 4 * 44100 * 3 + 12);
        for (int rate : new int[] {8000, 48000}) {
            for (boolean twopass : new boolean[] {true, false}) {
                Map<String, Object> props = Map.of("twopass", twopass, "profile", "fast");

                ByteArrayOutputStream expected = new ByteArrayOutputStream();
                new SSRC().io(Channels.newChannel(new ByteArrayInputStream(pcm)), Channels.newChannel(expected), pcm.length,
                        2, 44100, 2, rate, 2, props);

                AudioFormat inFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false, props);
                AudioFormat outFormat = new AudioFormat(rate, 16, 2, true, false);
                ByteArrayOutputStream actual = new ByteArrayOutputStream();
                try (InputStream in = new SSRCInputStream(inFormat, outFormat, new ByteArrayInputStream(pcm))) {
                    byte[] buf = new byte[1001];
                    int r;
                    while ((r = in.read(buf)) >= 0) {
                        actual.write(buf, 0, r);
                    }
                }

Debug.printf("%d, %b: %d bytes", rate, twopass, actual.size());
                assertTrue(expected.size() > 0);
                assertArrayEquals(expected.toByteArray(), actual.toByteArray());
            }
        }
    }

    /** 16 bit stereo, 3 sec */
    static byte[] pcm16() throws Exception {
        return Arrays.copyOfRange(Files.readAllBytes(Paths.get(inFile)), 44 + 4 * 44100 * 30, 44 + 4 * 44100 * 33);
    }

    static Map<String, Object> props(boolean twopass, int dither, int pdf) {
        Map<String, Object> props = new HashMap<>();
        props.put("twopass", twopass);
        props.put("dither", dither);
        props.put("pdf", pdf);
        props.put("profile", "fast");
        return props;
    }

    /** filter with 44100Hz stereo input */
    static byte[] io(ReadableByteChannel in, long length, int bps, int rate, int dbps, Map<String, Object> props) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SSRC().io(in, Channels.newChannel(out), length, 2, 44100, bps, rate, dbps, props);
        return out.toByteArray();
    }

    static byte[] io(byte[] pcm, int bps, int rate, int dbps, Map<String, Object> props) throws Exception {
        return io(Channels.newChannel(new ByteArrayInputStream(pcm)), pcm.length, bps, rate, dbps, props);
    }

    static short[] toShorts(byte[] b) {
        short[] s = new short[b.length / 2];
        for (int i = 0; i < s.length; i++) {
            s[i] = (short) ((b[i * 2] & 0xff) | (b[i * 2 + 1] << 8));
        }
        return s;
    }

    @Test
    @DisplayName("dither works for all types")
    public void test6() throws Exception {
        byte[] pcm = pcm16();
        for (boolean twopass : new boolean[] {true, false}) {
            short[] expected = toShorts(io(pcm, 2, 48000, 2, props(twopass, 0, 0)));
            for (int dither = 1; dither <= 4; dither++) {
                for (int pdf = 0; pdf <= 2; pdf++) {
                    short[] actual = toShorts(io(pcm, 2, 48000, 2, props(twopass, dither, pdf)));
                    assertEquals(expected.length, actual.length);
                    int max = 0;
                    for (int i = 0; i < expected.length; i++) {
                        max = Math.max(max, Math.abs(expected[i] - actual[i]));
                    }
Debug.printf("twopass: %b, dither: %d, pdf: %d, max diff: %d", twopass, dither, pdf, max);
                    assertTrue(max < 64, "dither " + dither + ", pdf " + pdf + ": " + max);
                }
            }
        }
    }

    @Test
    @DisplayName("same rate one pass keeps samples, 8 and 24 bit input")
    public void test7() throws Exception {
        byte[] pcm = pcm16();
        short[] src = toShorts(pcm);
        assertArrayEquals(pcm, io(pcm, 2, 44100, 2, props(false, 0, 0)));

        byte[] u8 = new byte[src.length];
        byte[] s24 = new byte[src.length * 3];
        for (int i = 0; i < src.length; i++) {
            u8[i] = (byte) ((src[i] >> 8) + 128);
            int v = src[i] << 8;
            s24[i * 3] = (byte) v;
            s24[i * 3 + 1] = (byte) (v >> 8);
            s24[i * 3 + 2] = (byte) (v >> 16);
        }
        assertArrayEquals(u8, io(u8, 1, 44100, 1, props(false, 0, 0)));
        assertArrayEquals(s24, io(s24, 3, 44100, 3, props(false, 0, 0)));

        // negative 24 bit samples are same as 16 bit ones
        short[] expected = toShorts(io(pcm, 2, 8000, 2, props(false, 0, 0)));
        short[] actual = toShorts(io(s24, 3, 8000, 2, props(false, 0, 0)));
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertTrue(Math.abs(expected[i] - actual[i]) <= 1, i + ": " + expected[i] + ", " + actual[i]);
        }
    }

    @Test
    @DisplayName("output is aligned with input, and its length is floor(n * dfrq / sfrq) + 2")
    public void test8() throws Exception {
        byte[] pcm = pcm16();
        short[] src = toShorts(pcm);
        int frames = src.length / 2;
        for (int rate : new int[] {8000, 48000}) {
            short[] out = toShorts(io(pcm, 2, rate, 2, props(false, 0, 0)));
            assertEquals((int) Math.floor((double) frames * rate / 44100) + 2, out.length / 2);

            int best = 0;
            double max = Double.NEGATIVE_INFINITY;
            for (int lag = -200; lag <= 200; lag++) {
                double c = 0;
                for (int n = out.length / 8; n < out.length / 2 - out.length / 8; n += 3) {
                    int j = (int) Math.round((n - lag) * 44100.0 / rate);
                    c += out[n * 2] * (double) src[j * 2];
                }
                if (c > max) {
                    max = c;
                    best = lag;
                }
            }
Debug.printf("rate: %d, lag: %d", rate, best);
            // the delay is truncated to a sample, before the fix it was lost for 45 ~ 10000 samples
            assertTrue(Math.abs(best) <= 1, "lag: " + best);
        }
    }

    @Test
    @DisplayName("short reads and unknown length give the same result")
    public void test9() throws Exception {
        byte[] pcm = pcm16();
        for (int rate : new int[] {8000, 48000, 44100}) {
            for (boolean twopass : new boolean[] {true, false}) {
                byte[] expected = io(pcm, 2, rate, 2, props(twopass, 0, 0));

                ReadableByteChannel in = Channels.newChannel(new ByteArrayInputStream(pcm));
                Random random = new Random(1);
                ReadableByteChannel shortReads = new ReadableByteChannel() {
                    @Override public int read(ByteBuffer dst) throws java.io.IOException {
                        ByteBuffer b = ByteBuffer.allocate(Math.min(dst.remaining(), 1 + random.nextInt(7)));
                        int r = in.read(b);
                        dst.put(b.flip());
                        return r;
                    }
                    @Override public boolean isOpen() { return true; }
                    @Override public void close() {}
                };
                assertArrayEquals(expected, io(shortReads, -1, 2, rate, 2, props(twopass, 0, 0)), rate + ", " + twopass);
            }
        }
    }

    @Test
    @DisplayName("stream, signed 8 bit and unsupported formats")
    public void test10() throws Exception {
        byte[] pcm = pcm16();
        short[] src = toShorts(pcm);
        byte[] u8 = new byte[src.length];
        byte[] s8 = new byte[src.length];
        for (int i = 0; i < src.length; i++) {
            u8[i] = (byte) ((src[i] >> 8) + 128);
            s8[i] = (byte) (src[i] >> 8);
        }
        Map<String, Object> props = props(true, 0, 0);
        byte[] expected = io(u8, 1, 8000, 1, props);

        AudioFormat inFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 8, 2, 2, 44100, false, props);
        AudioFormat outFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 8, 2, 2, 8000, false);
        // trailing bytes out of the frame length are not converted
        byte[] extra = Arrays.copyOf(s8, s8.length + 1000);
        try (InputStream in = new SSRCInputStream(inFormat, outFormat, new AudioInputStream(new ByteArrayInputStream(extra), inFormat, s8.length / 2))) {
            byte[] actual = in.readAllBytes();
            for (int i = 0; i < actual.length; i++) {
                actual[i] ^= (byte) 0x80;
            }
            assertArrayEquals(expected, actual);
        }

        AudioFormat bigEndian = new AudioFormat(44100, 16, 2, true, true);
        assertThrows(IllegalArgumentException.class, () -> new SSRCInputStream(bigEndian, new AudioFormat(8000, 16, 2, true, true), new ByteArrayInputStream(pcm)));
        AudioFormat bits32 = new AudioFormat(8000, 32, 2, true, false);
        assertThrows(IllegalArgumentException.class, () -> new SSRCInputStream(new AudioFormat(44100, 32, 2, true, false), bits32, new ByteArrayInputStream(pcm)));
    }

    @Test
    @DisplayName("close closes the source once, and the stream can not be read after that")
    public void test11() throws Exception {
        byte[] pcm = pcm16();
        for (boolean twopass : new boolean[] {true, false}) {
            Map<String, Object> props = props(twopass, 0, 0);
            AudioFormat inFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false, props);
            AudioFormat outFormat = new AudioFormat(8000, 16, 2, true, false);
            int[] closed = {0};
            InputStream source = new ByteArrayInputStream(pcm) {
                @Override public void close() {
                    closed[0]++;
                }
            };
            // as a spi conversion chain does
            AudioInputStream ais = new AudioInputStream(new SSRCInputStream(inFormat, outFormat, source), outFormat, AudioSystem.NOT_SPECIFIED);
            byte[] buf = new byte[100];
            assertEquals(100, ais.read(buf));
            ais.close();
            ais.close();
            assertEquals(1, closed[0]);
            assertThrows(java.io.IOException.class, () -> ais.read(buf));
            assertThrows(java.io.IOException.class, ais::available);
        }
    }
}
