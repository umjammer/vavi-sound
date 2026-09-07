/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.sshd;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import vavi.sound.adpcm.sshd.Sshd;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static vavi.sound.SoundUtil.volume;


/**
 * SshdAudioFileReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-11 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class SshdAudioFileReaderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

    @Property(name = "adpcm.sshd")
    String adpcm = "src/test/resources/test.ss2";

    @Property(name = "sshd.dir")
    String sshdDir;

    @TempDir
    Path dir;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

Debug.print("volume: " + volume);
    }

    @Test
    @DisplayName("detection by file and by stream")
    void test1() throws Exception {
        Path path = write(sshd());

        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(path.toFile());
        AudioFormat format = fileFormat.getFormat();
Debug.println("file: " + format);
        assertEquals(SshdEncoding.SSHD, format.getEncoding());
        assertEquals(SshdFileFormatType.SSHD, fileFormat.getType());
        assertEquals(44100, (int) format.getSampleRate());
        assertEquals(2, format.getChannels());
        assertEquals(Sshd.Codec.PCM16LE, format.getProperty("codec"));
        assertEquals(0x10, format.getProperty("interleave"));
        assertEquals(Sshd.HEADER_SIZE, format.getProperty("startOffset"));
        assertEquals(16, format.getProperty("numSamples"));
        assertEquals(16, fileFormat.getFrameLength());

        // by stream, the header tells everything but the file size, which available() stands in for
        AudioFormat format2;
        try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(path))) {
            format2 = AudioSystem.getAudioFileFormat(stream).getFormat();
        }
Debug.println("stream: " + format2);
        assertEquals(SshdEncoding.SSHD, format2.getEncoding());
        assertEquals(format.getChannels(), format2.getChannels());
        assertEquals(format.getProperty("codec"), format2.getProperty("codec"));
        assertEquals(format.getProperty("streamSize"), format2.getProperty("streamSize"));
    }

    @Test
    @DisplayName("spi conversion to pcm")
    void test2() throws Exception {
        Path path = write(sshd());

        AudioInputStream ais = AudioSystem.getAudioInputStream(path.toFile());
        AudioFormat inFormat = ais.getFormat();
Debug.println("in: " + inFormat);

        AudioFormat outFormat = new AudioFormat(inFormat.getSampleRate(), 16, inFormat.getChannels(), true, false);
        AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outFormat, ais);
Debug.println("out: " + pcmAis.getFormat());
        assertEquals(AudioFormat.Encoding.PCM_SIGNED, pcmAis.getFormat().getEncoding());

        byte[] pcm = pcmAis.readAllBytes();
        pcmAis.close();
        assertEquals(16 * 2 * 2, pcm.length);

        short[] expected = new short[16 * 2];
        for (int i = 0; i < 16; i++) {
            expected[i * 2] = (short) i;
            expected[i * 2 + 1] = (short) (0x100 + i);
        }
        short[] actual = new short[pcm.length / 2];
        ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    @DisplayName("non sshd contents are not detected")
    void test3() throws Exception {
        SshdAudioFileReader reader = new SshdAudioFileReader();
        File vox = new File("src/test/resources/vavi/sound/adpcm/vox/out.adpcm");
        assertThrows(UnsupportedAudioFileException.class, () -> reader.getAudioFileFormat(vox));
        assertThrows(UnsupportedAudioFileException.class, () ->
                reader.getAudioFileFormat(new ByteArrayInputStream(new byte[0x100])));
    }

    /** a pcm16 SShd file of 2 channels and 2 interleave block sets of 8 samples each */
    private static byte[] sshd() {
        ByteBuffer body = ByteBuffer.allocate(2 * 0x10 * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int set = 0; set < 2; set++) {
            for (int ch = 0; ch < 2; ch++) {
                for (int i = 0; i < 8; i++) {
                    body.putShort((short) (ch * 0x100 + set * 8 + i));
                }
            }
        }

        ByteBuffer bb = ByteBuffer.allocate(Sshd.HEADER_SIZE + body.capacity()).order(ByteOrder.LITTLE_ENDIAN);
        bb.put("SShd".getBytes());
        bb.putInt(0x18);            // header size
        bb.putInt(0x01);            // codec: pcm16le
        bb.putInt(44100);
        bb.putInt(2);               // channels
        bb.putInt(0x10);            // interleave
        bb.putInt(0xFFFFFFFF);      // loop start
        bb.putInt(0xFFFFFFFF);      // loop end
        bb.put("SSbd".getBytes());
        bb.putInt(body.capacity());
        bb.put(body.array());
        return bb.array();
    }

    /** */
    private Path write(byte[] data) throws Exception {
        Path path = dir.resolve("test.ads");
        Files.write(path, data);
        return path;
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    @DisplayName("spi playback")
    void test4() throws Exception {
        Path path = Path.of(adpcm);
        assumeTrue(Files.exists(path), "no sshd test file: " + path);
Debug.println("path: " + path + ", " + Files.size(path));

        AudioInputStream ais = AudioSystem.getAudioInputStream(path.toFile());
        AudioFormat inFormat = ais.getFormat();
Debug.println("in: " + inFormat);

        AudioFormat outFormat = new AudioFormat(inFormat.getSampleRate(), 16, inFormat.getChannels(), true, false);
        AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outFormat, ais);
Debug.println("out: " + pcmAis.getFormat());

        SourceDataLine line = AudioSystem.getSourceDataLine(outFormat);
        line.open(outFormat);
        line.start();
        volume(line, volume);

        byte[] buf = new byte[8192];
        int l;
        while ((l = pcmAis.read(buf, 0, buf.length)) != -1) {
            line.write(buf, 0, l);
        }

        line.drain();
        line.stop();
        line.close();
        pcmAis.close();
    }

    /**
     * @param dir separated by ';'
     * @param ext separated by ','
     */
    static List<Path> listFilesUnderDirFilteredByExt(String dir, String ext) {
Debug.println("dir: " + dir);
Debug.println("ext: " + ext);
        Predicate<Path> x = p -> Arrays.stream(ext.split(",")).anyMatch(e -> p.getFileName().toString().toUpperCase().endsWith(e));
        return Arrays.stream(dir.split(File.pathSeparator)).flatMap(d -> {
            try {
                return Files.walk(Paths.get(d)).filter(x);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).toList();
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    @DisplayName("spi playback dir recursive")
    void test5() throws Exception {

        List<Path> list = listFilesUnderDirFilteredByExt(sshdDir, ".SS2");
Debug.print("sshd: " + list.size());

        list.forEach(p -> {
            try {
                assumeTrue(Files.exists(p), "no sshd test file: " + p);
Debug.println("path: " + p + ", " + Files.size(p));

                AudioInputStream ais = AudioSystem.getAudioInputStream(p.toFile());
                AudioFormat inFormat = ais.getFormat();
Debug.println("in: " + inFormat);

                AudioFormat outFormat = new AudioFormat(inFormat.getSampleRate(), 16, inFormat.getChannels(), true, false);
                AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outFormat, ais);
Debug.println("out: " + pcmAis.getFormat());

                SourceDataLine line = AudioSystem.getSourceDataLine(outFormat);
                line.open(outFormat);
                line.start();
                volume(line, volume);

                byte[] buf = new byte[8192];
                int l;
                while ((l = pcmAis.read(buf, 0, buf.length)) != -1) {
                    line.write(buf, 0, l);
                }

                line.drain();
                line.stop();
                line.close();
                pcmAis.close();
            } catch (Exception e) {
                Debug.printStackTrace(e);
            }
        });
    }
}
