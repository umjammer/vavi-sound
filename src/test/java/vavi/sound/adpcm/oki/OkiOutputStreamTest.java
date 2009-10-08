/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.oki;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import junit.framework.TestCase;

import vavi.io.IOStreamOutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.sound.Checksum;


/**
 * OkiOutputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class OkiOutputStreamTest extends TestCase {

    String inFile = "pcm_8k_16_mono.pcm";
    String outFile = "out.vavi.adpcm";
    String correctFile = "out.adpcm";

    /** */
    public void test1() throws Exception {
        OutputStream os = new FileOutputStream(outFile);
        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(new FileInputStream(inFile), new IOStreamOutputEngine.OutputStreamFactory() {
            public OutputStream getOutputStream(OutputStream out) throws IOException {
                return new OkiOutputStream(out, ByteOrder.LITTLE_ENDIAN);
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

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }
}

/* */
