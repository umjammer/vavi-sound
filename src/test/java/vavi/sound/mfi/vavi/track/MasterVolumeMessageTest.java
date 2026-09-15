/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.SysexMessage;

import org.junit.jupiter.api.Test;

import vavi.sound.mfi.vavi.MidiContext;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


/**
 * MasterVolumeMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-15 nsano initial version <br>
 */
class MasterVolumeMessageTest {

    /** the universal master volume as before, marked as the song's just before it */
    @Test
    void midiEvents() throws Exception {
        MidiEvent[] events = new MasterVolumeMessage().init(0, 0xff, 0xb0, 100).getMidiEvents(new MidiContext());

        assertEquals(2, events.length);
        assertArrayEquals(new byte[] { (byte) 0xf0, 0x45, MasterVolumeMessage.SYSEX_FUNCTION_ID_MASTER_VOLUME, 100, (byte) 0xf7 },
                assertInstanceOf(SysexMessage.class, events[0].getMessage()).getMessage());
        assertArrayEquals(new byte[] { (byte) 0xf0, 0x7f, 0x7f, 0x04, 0x01, 0x00, 100, (byte) 0xf7 },
                assertInstanceOf(SysexMessage.class, events[1].getMessage()).getMessage());
    }
}
