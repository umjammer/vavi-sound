/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ima;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import org.junit.Before;
import org.junit.Test;

import vavi.io.IOStreamInputEngine;
import vavi.io.IOStreamOutputEngine;
import vavi.io.InputEngineOutputStream;
import vavi.io.LittleEndianDataInputStream;
import vavi.io.OutputEngineInputStream;
import vavi.util.Debug;
import vavix.util.Checksum;

import static org.junit.Assert.assertEquals;


/**
 * ImaOutputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class ImaOutputStreamTest {

    String inFile = "out.pcm";
    String correctFile = "out_sox.adpcm";
    File outFile;

    @Before
    public void setup() throws IOException {
        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
//        outFile = new File("src/test/resources/vavi/sound/adpcm/ima/out_vavi.adpcm");
Debug.println("outFile: " + outFile.getCanonicalPath());
    }
    
    /** */
    @Test
    public void test1() throws Exception {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));
        InputStream in = new BufferedInputStream(getClass().getResourceAsStream(inFile));
        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(in, new IOStreamOutputEngine.OutputStreamFactory() {
            public OutputStream getOutputStream(OutputStream out) throws IOException {
                return new ImaOutputStream(out, 505, 1);
            }
        }));
        byte[] buffer = new byte[505 * 2];
        while (true) {
            int amount = is.read(buffer);
//System.err.println("amount: " + amount);
            if (amount < 0) {
                break;
            }
            os.write(buffer, 0, amount);
            os.flush();
        }
        os.close();
        is.close();

        InputStream is2 = new ImaInputStream(new FileInputStream(outFile),
                                             505,
                                             1,
                                             256);

        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                  8000,
                                                  16,
                                                  1,
                                                  2,
                                                  8000,
                                                  true);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();

        FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        double gain = .2d; // number between 0 and 1 (loudest)
        float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
        gainControl.setValue(dB);

        byte[] buf = new byte[8192];
        while (true) {
            int r = is2.read(buf, 0, 8192);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }

        line.drain();
        line.stop();
        line.close();

Debug.println("outFile: " + outFile.length());
        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(outFile));
    }

    /**
     * TODO input engine と output engine の結合がうまくいかない
     */
    @Test
    public void test2() throws Exception {
//        final String inFile = "out.pcm";
//        final String outFile = "src/test/resources/vavi/sound/adpcm/ima/out_vavi.pcm";

        InputStream ris = getClass().getResourceAsStream(inFile);
        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(ris, new IOStreamOutputEngine.OutputStreamFactory() {
            public OutputStream getOutputStream(OutputStream out) throws IOException {
                return new ImaOutputStream(out, 505, 1);
            }
        }, 1010));
        OutputStream fos = new FileOutputStream(outFile);
        OutputStream os = new InputEngineOutputStream(new IOStreamInputEngine(fos, new IOStreamInputEngine.InputStreamFactory() {
            public InputStream getInputStream(InputStream in) throws IOException {
                return new ImaInputStream(in, 505, 1, 256);
            }
        }, 1010));
        byte[] buffer = new byte[505 * 2];
        while (true) {
            int amount = is.read(buffer);
            if (amount < 0) {
                break;
            }
            os.write(buffer, 0, amount);
        }
        os.flush();
        os.close();
        is.close();

        InputStream is2 = new FileInputStream(outFile);
        
        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                  8000,
                                                  16,
                                                  1,
                                                  2,
                                                  8000,
                                                  true);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();

        FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        double gain = .2d; // number between 0 and 1 (loudest)
        float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
        gainControl.setValue(dB);

        byte[] buf = new byte[8192];
        while (true) {
            int r = is2.read(buf, 0, 8192);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }

        line.drain();
        line.stop();
        line.close();

Debug.println("outFile: " + outFile.length());
        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(inFile)), Checksum.getChecksum(outFile));
    }

    /** */
    @Test
    public void test3() throws Exception {
        final String inFile = "out.pcm";
        final String outFile = "src/test/resources/vavi/sound/adpcm/ima/out_vavi.adpcm";

        InputStream in = new BufferedInputStream(getClass().getResourceAsStream(inFile));
        LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(in);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));

        Ima encoder = new Ima();
        int[] steps = new int[16];

        int length = in.available();
        int spb = 505;
        int ll = 0;
Debug.println("inFile: " + length);
        while (ll < length) {
            int bpb = Ima.getBytesPerBlock(1, spb);
            byte[] buffer = new byte[spb * 2];
            int l = 0;
            while (l < buffer.length) {
                int r = in.read(buffer, l, buffer.length - l);
                if (r < 0) {
                    break;
                }
                l += r;
            }
            if (l > 0) {
                byte[] adpcm = new byte[bpb];
                int[] pcm = new int[l / 2];
                ledis = new LittleEndianDataInputStream(new ByteArrayInputStream(buffer));
                for (int i = 0; i < pcm.length; i++) {
                    pcm[i] = ledis.readShort();
                }
                encoder.encodeBlock(1, pcm, pcm.length, steps, adpcm, 9);
    
                os.write(adpcm);
            }
            
            ll += l;
        }

        os.flush();
        os.close();

        InputStream is = new ImaInputStream(new FileInputStream(outFile),
                                            505,
                                            1,
                                            256);

        AudioFormat audioFormat = new AudioFormat(
                                                  AudioFormat.Encoding.PCM_SIGNED,
                                                  8000,
                                                  16,
                                                  1,
                                                  2,
                                                  8000,
                                                  true);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();

        FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        double gain = .2d; // number between 0 and 1 (loudest)
        float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
        gainControl.setValue(dB);

        byte[] buf = new byte[8192];
        while (true) {
            int r = is.read(buf, 0, 8192);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }

        line.drain();
        line.stop();
        line.close();

Debug.println("outFile: " + new File(outFile).length());
        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(new File(outFile)));
    }
}

/* */
