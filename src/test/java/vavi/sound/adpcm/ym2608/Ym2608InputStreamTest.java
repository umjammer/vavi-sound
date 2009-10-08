/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ym2608;

import java.io.File;

import junit.framework.TestCase;

import vavi.sound.Checksum;


/**
 * Ym2608InputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060124 nsano initial version <br>
 */
public class Ym2608InputStreamTest extends TestCase {

    String inFile = "out.vavi.adpcm";
    String outFile = "out.vavi.pcm";
    String correctFile = "out.pcm";

    /** */
    public void test1() throws Exception {
        Ym2608InputStream.main(new String[] { inFile, outFile, "test" });

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }
}

/* */
