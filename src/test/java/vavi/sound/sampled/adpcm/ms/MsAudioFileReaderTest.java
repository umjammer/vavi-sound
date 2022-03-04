/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ms;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;

import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * MsAudioFileReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
@Disabled
public class MsAudioFileReaderTest {

    String inFile = "/vavi/sound/adpcm/ms/ms_8k_4_mono.wav";
    String correctFile = "/vavi/sound/adpcm/ms/out_sox.pcm";
    File outFile;

    @BeforeEach
    public void setup() throws IOException {
        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
Debug.println("outFile: " + outFile);
    }

    /** */
    @Test
    public void test1() throws Exception {

        int sampleRate = 8000;
        ByteOrder byteOrder = ByteOrder.nativeOrder();

        AudioFormat outFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println(outFormat);

        AudioFormat inFormat = new AudioFormat(
            MsEncoding.MS,
            sampleRate,
            16,
            1,
            AudioSystem.NOT_SPECIFIED,
            sampleRate,
            false);
System.err.println(inFormat);

        AudioInputStream iais = new AudioInputStream(getClass().getResourceAsStream(inFile), inFormat, AudioSystem.NOT_SPECIFIED);
System.err.println("in available: " + iais.available() + ", " + iais.getFormat());
        AudioInputStream oais = AudioSystem.getAudioInputStream(outFormat, iais);
System.err.println("out available: " + oais.available() + ", " + oais.getFormat());

        OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));

DataLine.Info info = new DataLine.Info(SourceDataLine.class, outFormat);
SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
line.open(outFormat);
line.start();
        byte[] buf = new byte[1024];
        int l = 0;
        volume(line, .2d);

        while (oais.available() > 0) {
            l = oais.read(buf, 0, 1024);
line.write(buf, 0, l);
            os.write(buf, 0, l);
        }
line.drain();
line.stop();
line.close();
        os.close();

        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(outFile));
    }
}

/* */
