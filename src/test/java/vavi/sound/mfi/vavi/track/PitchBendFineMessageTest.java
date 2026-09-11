/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import org.junit.jupiter.api.Test;

import vavi.sound.mfi.vavi.TrackMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * PitchBendFineMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
class PitchBendFineMessageTest {

    @Test
    void factory() {
        assertInstanceOf(PitchBendFineMessage.class, TrackMessage.factory("255.b.233"));
    }

    @Test
    void init() {
        PitchBendFineMessage m = new PitchBendFineMessage().init(0, 0xff, 0xe9, 0xe0);
        assertEquals(3, m.getVoice());      // 0xe0 >> 6
        assertEquals(32, m.getPitchBendFine());   // the neutral value

        m.setVoice(1);
        m.setPitchBendFine(48);
        assertEquals(1, m.getVoice());
        assertEquals(48, m.getPitchBendFine());
        assertEquals((byte) 0x70, m.getMessage()[3]);
    }

    /** it deliberately emits nothing until the coarse / fine scale is settled */
    @Test
    void noMidiYet() {
        assertNull(new PitchBendFineMessage().init(0, 0xff, 0xe9, 0x20).getMidiEvents(null));
    }
}
