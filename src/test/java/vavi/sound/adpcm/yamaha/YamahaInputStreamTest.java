/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.yamaha;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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

import vavi.sound.Checksum;
import vavi.util.Debug;
import vavi.util.win32.WAVE;

import static org.junit.Assert.assertEquals;


/**
 * YamahaInputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class YamahaInputStreamTest {

    String inFile = "out.adpcm";
    String correctFile = "out.pcm";
    File outFile;

    @Before
    public void setup() throws IOException {
        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
Debug.println("outFile: " + outFile);
    }

    /** */
    @Test
    public void test1() throws Exception {

        int sampleRate = 8000;
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println(format);

        InputStream is = new YamahaInputStream(getClass().getResourceAsStream(inFile), ByteOrder.LITTLE_ENDIAN);
System.err.println("available: " + is.available());
        OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
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
        
        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(outFile));
    }

    /** */
    public static void main(String[] args) throws Exception {

        InputStream in = new BufferedInputStream(new FileInputStream(args[0]));
        WAVE wave = (WAVE) WAVE.readFrom(in);
        in.close();
        WAVE.fmt format = (WAVE.fmt) wave.findChildOf(WAVE.fmt.class);
        if (format.getFormatId() != 0x0062) {
            throw new IllegalArgumentException("not YAMAHA MA ADPCM");
        }
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

        InputStream is = new YamahaInputStream(in, byteOrder);
System.err.println("available: " + is.available());

OutputStream os = new BufferedOutputStream(new FileOutputStream(args[1]));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.addLineListener(new LineListener() {
            public void update(LineEvent ev) {
Debug.println(ev.getType());
                if (LineEvent.Type.STOP == ev.getType()) {
                    System.exit(0);
                }
            }
        });
        line.start();
        byte[] buf = new byte[1024];
        int l = 0;

        while (is.available() > 0) {
            l = is.read(buf, 0, 1024);
            line.write(buf, 0, l);
os.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
os.close();
    }
}

/* */
