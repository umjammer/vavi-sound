/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.ssrc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vavi.io.LittleEndianDataInputStream;
import vavi.io.LittleEndianDataOutputStream;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        int f = format.getFrameSize() * format.getChannels();
//Debug.println("frame: " + f);
outer:
        while (true) {
            int l = 0, a, b = 0;
            // SSRCInputStream is async class
            // so we need to wait data buffer will be filled.
            while (l < 4096) {
                 int r = in.read(buf, l + b, buf.length - (l + b));
                 if (r < 0)
                     break outer;
                 l += r;
            }
//Debug.println(l);
            // we need to keep line.write buffer size is multiply of "f"
            a = l / f * f;
            b = l % f;
            if (onIde)
                line.write(buf, 0, a);
            System.arraycopy(buf, 0, buf, a, b);
        }
        in.close();
        line.drain();
        line.stop();
        line.close();
    }
}
