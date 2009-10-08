/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.vox;

import java.io.File;

import junit.framework.TestCase;

import vavi.sound.Checksum;


/**
 * VoxInputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class VoxInputStreamTest extends TestCase {

    String inFile = "out.adpcm";
    String outFile = "out.vavi.pcm";
    String correctFile = "out.pcm";

    /** */
    public void test1() throws Exception {
        VoxInputStream.main(new String[] { inFile, outFile, "test" });

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }
}

/* */
