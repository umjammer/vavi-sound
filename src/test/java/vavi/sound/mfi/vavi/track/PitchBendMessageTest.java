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
import vavi.sound.mfi.vavi.TrackMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


/**
 * PitchBendMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
class PitchBendMessageTest {

    @Test
    void factory() {
        assertInstanceOf(PitchBendMessage.class, TrackMessage.factory("255.b.228"));
    }

    /**
     * The fine halves now reach midi through this message, so what it sends when nothing
     * bends finely has to be what it always sent: LSB 0, MSB {@code pitchBend * 2}.
     */
    @Test
    void neutralFineIsUnchanged() throws Exception {
        for (int pitchBend = 0; pitchBend < 64; pitchBend++) {
            MidiContext context = new MidiContext();
            context.setMfiTrackNumber(0);
            ShortMessage bend = bendOf(new PitchBendMessage().init(0, 0xff, 0xe4, pitchBend).getMidiEvents(context));
            assertEquals(0, bend.getData1(), "LSB at pitchBend " + pitchBend);
            assertEquals(pitchBend * 2, bend.getData2(), "MSB at pitchBend " + pitchBend);
        }
        // and the rest of both halves is the midi rest
        MidiContext context = new MidiContext();
        context.setMfiTrackNumber(0);
        ShortMessage bend = bendOf(new PitchBendMessage().init(0, 0xff, 0xe4, MidiContext.PITCH_BEND_NEUTRAL).getMidiEvents(context));
        assertEquals(MidiContext.MIDI_PITCH_BEND_NEUTRAL, (bend.getData2() << 7) | bend.getData1());
    }

    /** the fine half 0xe9 leaves behind is picked up by the 0xe4 that follows it */
    @Test
    void fineHalfIsCommitted() throws Exception {
        MidiContext context = new MidiContext();
        context.setMfiTrackNumber(0);

        new PitchBendFineMessage().init(0, 0xff, 0xe9, 41).getMidiEvents(context);
        ShortMessage bend = bendOf(new PitchBendMessage().init(0, 0xff, 0xe4, 33).getMidiEvents(context));
        assertEquals((((33 << 5) + 41) << 3) - 0x100, (bend.getData2() << 7) | bend.getData1());

        // the "A" half writes the same register, so a 0xe4 after it picks that one up
        new PitchBendFineAMessage().init(0, 0xff, 0xe8, 13).getMidiEvents(context);
        bend = bendOf(new PitchBendMessage().init(0, 0xff, 0xe4, 33).getMidiEvents(context));
        assertEquals((((33 << 5) + 13) << 3) - 0x100, (bend.getData2() << 7) | bend.getData1());
    }

    /** the last midi pitch bend of the events */
    private static ShortMessage bendOf(MidiEvent[] events) {
        ShortMessage bend = (ShortMessage) events[events.length - 1].getMessage();
        assertEquals(ShortMessage.PITCH_BEND, bend.getCommand());
        return bend;
    }
}
