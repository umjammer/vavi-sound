/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import vavi.sound.mfi.vavi.track.AudioPlayMessage;
import vavi.sound.mfi.vavi.track.PanpotMessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * TrackMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-19 nsano initial version <br>
 */
class TrackMessageTest {

    @Test
    void test() throws Exception {
        TrackMessage tm = TrackMessage.factory( "127.a.0");
        assertInstanceOf(AudioPlayMessage.class, tm);

        tm = TrackMessage.factory( "255.b.227");
        assertInstanceOf(PanpotMessage.class, tm);

//        tm = TrackMessage.factory( "127.e.0");
//        assertInstanceOf(AudioPlayMessage.class, tm);

        tm = TrackMessage.factory( "0.x.0");
        assertNull(tm);
    }
}
