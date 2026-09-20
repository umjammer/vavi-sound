/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import org.junit.jupiter.api.Test;

import vavi.sound.mfi.vavi.sequencer.MfiValueExclusive;
import vavi.sound.mfi.vavi.MidiContext;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


/**
 * ChangeBankMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-15 nsano initial version <br>
 */
class ChangeBankMessageTest {

    /** the mfi bank goes as it is by a vavi exclusive, the program keeps its bit 0 as before */
    @Test
    void midiEvents() throws Exception {
        MidiContext context = new MidiContext();
        context.setMfiTrackNumber(1);
        MidiEvent[] events = new ChangeBankMessage().init(0, 0xff, 0xe1, 0x40 | 0x35).getMidiEvents(context);

        assertEquals(2, events.length);
        SysexMessage sysex = assertInstanceOf(SysexMessage.class, events[0].getMessage());
        assertArrayEquals(new byte[] { (byte) 0xf0, 0x45, MfiValueExclusive.MIDI_SYSEX_FUNCTION_ID_VALUE, MfiValueExclusive.BANK, 5, 0x35, (byte) 0xf7 }, sysex.getMessage());
        ShortMessage program = assertInstanceOf(ShortMessage.class, events[1].getMessage());
        assertEquals(ShortMessage.PROGRAM_CHANGE, program.getCommand());
        assertEquals(5, program.getChannel());
        assertEquals(0x40, program.getData1());
    }
}
