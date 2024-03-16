/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import org.junit.jupiter.api.Test;

import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.AudioEngine.Util;
import vavi.sound.mobile.FuetrekAudioEngine;
import vavi.sound.mobile.RohmAudioEngine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * AudioEngineTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
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


    @Test
    public void test3() {
        // exists in /vavi/sound/mfi/vavi/vavi.properties
        AudioEngine audioEngine = AudioDataSequencer.Factory.getAudioEngine(0x80);
        assertInstanceOf(RohmAudioEngine.class, audioEngine);
        audioEngine = AudioDataSequencer.Factory.getAudioEngine(0x81);
        assertInstanceOf(FuetrekAudioEngine.class, audioEngine);
        // not exists
        audioEngine = AudioDataSequencer.Factory.getAudioEngine(0x82);
        assertNull(audioEngine);
    }
}
