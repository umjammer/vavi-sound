/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ima;

import java.io.File;

import junit.framework.TestCase;

import vavi.sound.Checksum;


/**
 * ImaOutputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class ImaInputStreamTest extends TestCase {

    String inFile = "ima_8k_4_mono.wav";
    String outFile = "out.vavi.pcm";
    String correctFile = "linear_8k_16_mono.pcm";

    /** */
    public void test1() throws Exception {
        ImaInputStream.main(new String[] { inFile, outFile, "test" });

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }
}

/* */
