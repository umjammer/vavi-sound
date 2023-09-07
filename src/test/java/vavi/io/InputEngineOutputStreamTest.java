/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import vavix.io.IOStreamInputEngine;
import vavix.io.IOStreamOutputEngine;
import vavix.util.Checksum;
import vavix.util.Rot13;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * InputEngineOutputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060126 nsano initial version <br>
 */
class InputEngineOutputStreamTest {

    String inFile = "/test.sql";
    String out1File = "tmp/test.r13";
    String out2File = "tmp/test2.sql";

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(Paths.get("tmp"));
    }

    @Test
    public void test1() throws Exception {
        //
        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(getClass().getResourceAsStream(inFile), Rot13.OutputStream::new));
        OutputStream os = Files.newOutputStream(Paths.get(out1File));
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

        //
        is = Files.newInputStream(Paths.get(out1File));
        os = new InputEngineOutputStream(new IOStreamInputEngine(Files.newOutputStream(Paths.get(out2File)), Rot13.InputStream::new));
        buffer = new byte[8192];
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

        //
        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(inFile)), Checksum.getChecksum(new File(out2File)));
    }
}

/* */
