/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import org.junit.Test;

import vavi.sound.mobile.AudioEngine.Util;

import static org.junit.Assert.assertArrayEquals;


/**
 * AudioEngineTest.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 070405 nsano initial version <br>
 */
public class AudioEngineTest {

    @Test
    public void test1() {
        byte[] stereo = new byte[] { 1, 2, 3, 4 };
        byte[][] monos = Util.toMono(stereo, 16, null);
        assertArrayEquals(new byte[] { 1, 2 }, monos[0]);
        assertArrayEquals(new byte[] { 3, 4 }, monos[1]);
    }

    @Test
    public void test2() {
        byte[] monoL = new byte[] { (byte) 0xab, (byte) 0xcd };
        byte[] monoR = new byte[] { (byte) 0x12, (byte) 0x34 };
        byte[] stereo = Util.toStereo(monoL, monoR, 4, null);
        assertArrayEquals(new byte[] { (byte) 0xa1, (byte) 0xb2, (byte) 0xc3, (byte) 0xd4 }, stereo);
    }
}

/* */
