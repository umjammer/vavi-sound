/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.rohm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import org.junit.Before;
import org.junit.Test;

import vavi.io.IOStreamOutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.sound.Checksum;
import vavi.util.Debug;

import static org.junit.Assert.assertEquals;


/**
 * RohmOutputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class RohmOutputStreamTest {

    String inFile = "pcm_8k_16_mono.pcm";
    String correctFile = "out.adpcm";
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
        OutputStream os = new FileOutputStream(outFile);
        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(getClass().getResourceAsStream(inFile), new IOStreamOutputEngine.OutputStreamFactory() {
            public OutputStream getOutputStream(OutputStream out) throws IOException {
                return new RohmOutputStream(out, ByteOrder.LITTLE_ENDIAN);
            }
        }));
        byte[] buffer = new byte[8192];
        while (true) {
            int amount = is.read(buffer);
            if (amount < 0) {
                break;
            }
            os.write(buffer, 0, amount);
        }
        is.close();
        os.flush();
        os.close();

        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(outFile));
    }
}

/* */
