/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.vavi.sequencer.FuetrekMfiExclusive;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.TrackMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;


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

    /** the fine half goes as the sound source exclusive, no midi pitch bend of its own */
    @Test
    void midiEvents() throws Exception {
        MidiContext context = new MidiContext();
        context.setMfiTrackNumber(2);
        MidiEvent[] events = new PitchBendFineMessage().init(0, 0xff, 0xe9, 0x40 | 0x23).getMidiEvents(context);
        assertEquals(1, events.length);
        assertArrayEquals(new byte[] { (byte) 0xf0, 0x45, FuetrekMfiExclusive.MFi_SYSEX_FUNCTION_ID_FUETREK, FuetrekMfiExclusive.PITCH_BEND_FINE, 9, 0x23, (byte) 0xf7 },
                events[0].getMessage().getMessage());
    }
}
