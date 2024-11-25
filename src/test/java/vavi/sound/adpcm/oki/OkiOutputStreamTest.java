/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.oki;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.io.OutputEngineInputStream;
import vavi.util.Debug;
import vavix.io.IOStreamOutputEngine;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * OkiOutputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class OkiOutputStreamTest {

    String inFile = "pcm_8k_16_mono.pcm";
    String correctFile = "out.adpcm";
    File outFile;

    @BeforeEach
    public void setup() throws IOException {
        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
Debug.println(Level.FINE, "outFile: " + outFile);
    }

    @Test
    public void test1() throws Exception {
        OutputStream os = Files.newOutputStream(outFile.toPath());
        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(getClass().getResourceAsStream(inFile),
                out -> new OkiOutputStream(out, ByteOrder.LITTLE_ENDIAN)));
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
