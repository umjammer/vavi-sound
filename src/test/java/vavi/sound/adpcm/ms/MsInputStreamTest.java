/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import org.junit.Before;
import org.junit.Test;

import vavi.io.LittleEndianDataInputStream;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.win32.WAVE;
import vavix.util.Checksum;

import static org.junit.Assert.assertEquals;


/**
 * MsInputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class MsInputStreamTest {

    String inFile = "ms_8k_4_mono.wav";
    String correctFile = "out_sox.pcm";
    File outFile;

    @Before
    public void setup() throws IOException {
        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
Debug.println("outFile: " + outFile);
    }

    /**
     * <pre>
     * MS wave ext
     *  2 bytes samplesPerBlock (little endian)
     *  2 bytes nCoefs
     *  2 * nCoefs iCoefs
     * </pre>
     */
    @Test
    public void test1() throws Exception {

        InputStream in = new BufferedInputStream(getClass().getResourceAsStream(inFile));
        WAVE wave = (WAVE) WAVE.readFrom(in);
        in.close();
        WAVE.fmt format = (WAVE.fmt) wave.findChildOf(WAVE.fmt.class);
        if (format.getFormatId() != 0x0002) {
            throw new IllegalArgumentException("not Microsoft ADPCM");
        }
        LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(new ByteArrayInputStream(format.getExtended()));
Debug.println("ext size: " + ledis.available());
        int samplesPerBlock = ledis.readShort();
        int nCoefs = ledis.readShort();
        int[][] iCoefs = new int[nCoefs][2];
        for (int i = 0; i < nCoefs; i++) {
            for (int j = 0; j < 2; j++) {
                iCoefs[i][j] = ledis.readShort();
Debug.println("iCoef[" + i + "][" + j + "]: " + StringUtil.toHex4(iCoefs[i][j]) + ": " + iCoefs[i][j]);
            }
        }
        ledis.close();
        WAVE.data data = (WAVE.data) wave.findChildOf(WAVE.data.class);
        in = new ByteArrayInputStream(data.getWave());
Debug.println("wave: " + in.available());

        //----

        int sampleRate = format.getSamplingRate();
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println(audioFormat);

Debug.println("samplesPerBlock: " + samplesPerBlock + ", numberChannels: " + format.getNumberChannels() + ", blockSize: " + format.getBlockSize());
        InputStream is = new MsInputStream(in,
                                           samplesPerBlock,
                                           nCoefs,
                                           iCoefs,
                                           format.getNumberChannels(),
                                           format.getBlockSize(),
                                           byteOrder);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));

DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
line.open(audioFormat);
line.addLineListener(new LineListener() {
 public void update(LineEvent ev) {
Debug.println(ev.getType());
  if (LineEvent.Type.STOP == ev.getType()) {
  }
 }
});
line.start();

FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);

        byte[] buf = new byte[1024];
        while (true) {
            int r = is.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
line.write(buf, 0, r);
            os.write(buf, 0, r);
        }
line.drain();
line.stop();
line.close();
        os.close();
        
        is.close();

        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(outFile));
    }
}

/* */
