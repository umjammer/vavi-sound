/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import vavix.io.Rot13;
import vavix.util.Checksum;

import static org.junit.Assert.assertEquals;


/**
 * InputEngineOutputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060126 nsano initial version <br>
 */
public class InputEngineOutputStreamTest {

    String inFile = "/test.sql";
    String out1File = "tmp/test.r13";
    String out2File = "tmp/test2.sql";

    /** */
    @Test
    public void test1() throws Exception {
        //
        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(getClass().getResourceAsStream(inFile), new IOStreamOutputEngine.OutputStreamFactory() {
            public OutputStream getOutputStream(OutputStream out) throws IOException {
                return new Rot13.OutputStream(out);
            }
        }));
        OutputStream os = new FileOutputStream(out1File);
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
        is = new FileInputStream(out1File);
        os = new InputEngineOutputStream(new IOStreamInputEngine(new FileOutputStream(out2File), new IOStreamInputEngine.InputStreamFactory() {
            public InputStream getInputStream(InputStream in) throws IOException {
                return new Rot13.InputStream(in);
            }
        }));
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
