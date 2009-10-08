/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.rohm;

import java.io.File;

import junit.framework.TestCase;

import vavi.sound.Checksum;


/**
 * RohmInputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060130 nsano initial version <br>
 */
public class RohmInputStreamTest extends TestCase {

    String inFile = "out.adpcm";
    String outFile = "out.vavi.pcm";
    String correctFile = "out.pcm";

    /** */
    public void test1() throws Exception {
        RohmInputStream.main(new String[] { inFile, outFile, "test" });

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }
}

/* */
