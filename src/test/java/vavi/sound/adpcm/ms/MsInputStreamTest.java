/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ms;

import java.io.File;

import junit.framework.TestCase;

import vavi.sound.Checksum;


/**
 * MsInputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class MsInputStreamTest extends TestCase {

    String inFile = "ms_8k_4_mono.wav";
    String outFile = "out.vavi.pcm";
    String correctFile = "out_sox.pcm";

    /** */
    public void test1() throws Exception {
        MsInputStream.main(new String[] { inFile, outFile, "test" });

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }
}

/* */
