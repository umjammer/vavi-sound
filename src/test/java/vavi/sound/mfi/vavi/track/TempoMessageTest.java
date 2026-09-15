/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import org.junit.jupiter.api.Test;

import vavi.sound.mfi.vavi.MidiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * TempoMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-15 nsano initial version <br>
 */
class TempoMessageTest {

    private static int usPerQuarter(MidiEvent event) {
        byte[] data = ((MetaMessage) event.getMessage()).getData();
        return ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
    }

    /** a quarter note is 60 / tempo seconds whatever the time base is */
    @Test
    void midiEvents() throws Exception {
        MidiContext context = new MidiContext();
        // resolution not decided: the time base of the message
        assertEquals(500000, usPerQuarter(new TempoMessage().init(0, 0xff, 0xc3, 120).getMidiEvents(context)[0]));
        assertEquals(472441, usPerQuarter(new TempoMessage().init(0, 0xff, 0xcb, 127).getMidiEvents(context)[0]));
    }

    @Test
    void tempo() {
        // midi 480 ticks a quarter at 120 bpm, written as time base 48 ticks scaled by 10
        assertEquals(120, TempoMessage.tempo(480, 10, 48, 500000));
        // midi 96, not scaled
        assertEquals(120, TempoMessage.tempo(96, 1, 96, 500000));
        // a larger time base declared for the same ticks halves the number
        assertEquals(150, TempoMessage.tempo(48, 1, 96, 200000));
    }
}
