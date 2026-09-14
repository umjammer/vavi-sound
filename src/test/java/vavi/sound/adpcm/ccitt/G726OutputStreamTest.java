/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import vavi.io.OutputEngineInputStream;
import vavi.util.Debug;
import vavix.io.IOStreamOutputEngine;
import vavix.util.Checksum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * G726OutputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260915 nsano initial version <br>
 */
class G726OutputStreamTest {

    String inFile = "pcm_8k_16_mono.pcm";
    String correctFile = "g726.4.adpcm";

    @TempDir
    File tmpDir;

    @Test
    void test1() throws Exception {
        File outFile = new File(tmpDir, "out.adpcm");
        OutputStream os = Files.newOutputStream(outFile.toPath());
        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(getClass().getResourceAsStream(inFile),
                out -> new G726OutputStream(out, ByteOrder.LITTLE_ENDIAN)));
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

    @ParameterizedTest
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    @ValueSource(ints = {2, 3, 4, 5})
    void test2(int bits) throws Exception {

        String filename = "tmp/out_%d.g726".formatted(bits * 8);
Debug.print("filename: " + filename);
        OutputStream os = Files.newOutputStream(Path.of(filename));

        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(getClass().getResourceAsStream(inFile),
                out -> new G726OutputStream(out, bits, ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN)));
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
    }
}
