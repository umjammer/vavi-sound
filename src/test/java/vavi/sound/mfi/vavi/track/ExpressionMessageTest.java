/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.sequencer.MfiValueExclusive;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * ExpressionMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-18 nsano initial version <br>
 */
class ExpressionMessageTest {

    /** 0x00 and 0x20 are the same midi expression, the exclusive tells them apart */
    @Test
    void midiEvents() throws Exception {
        MidiContext context = new MidiContext();
        context.setMfiTrackNumber(1);
        MidiEvent[] a = new ExpressionMessage().init(0, 0xff, 0xe6, 0x80 | 0x00).getMidiEvents(context);
        MidiEvent[] b = new ExpressionMessage().init(0, 0xff, 0xe6, 0x80 | 0x20).getMidiEvents(context);
        assertEquals(2, a.length);
        assertEquals(((ShortMessage) a[1].getMessage()).getData2(), ((ShortMessage) b[1].getMessage()).getData2());
        assertArrayEquals(new byte[] { (byte) 0xf0, 0x45, MfiValueExclusive.MIDI_SYSEX_FUNCTION_ID_VALUE, MfiValueExclusive.EXPRESSION, 6, 0x00, (byte) 0xf7 },
                a[0].getMessage().getMessage());
        assertArrayEquals(new byte[] { (byte) 0xf0, 0x45, MfiValueExclusive.MIDI_SYSEX_FUNCTION_ID_VALUE, MfiValueExclusive.EXPRESSION, 6, 0x20, (byte) 0xf7 },
                b[0].getMessage().getMessage());
    }
}
