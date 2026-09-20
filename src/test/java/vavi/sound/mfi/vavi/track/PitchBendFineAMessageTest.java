/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.TrackMessage;
import vavi.sound.mfi.vavi.sequencer.MfiValueExclusive;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * PitchBendFineAMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
class PitchBendFineAMessageTest {

    @Test
    void factory() {
        assertInstanceOf(PitchBendFineAMessage.class, TrackMessage.factory("255.b.232"));
        // the "B" half keeps its own class
        assertInstanceOf(PitchBendFineMessage.class, TrackMessage.factory("255.b.233"));
    }

    @Test
    void init() {
        PitchBendFineAMessage m = new PitchBendFineAMessage().init(0, 0xff, 0xe8, 0xe0);
        assertEquals(3, m.getVoice());          // 0xe0 >> 6
        assertEquals(32, m.getPitchBendFine()); // the neutral value

        m.setVoice(1);
        m.setPitchBendFine(48);
        assertEquals(1, m.getVoice());
        assertEquals(48, m.getPitchBendFine());
        assertEquals((byte) 0x70, m.getMessage()[3]);
    }

    /** the whole 6 bits are in use, 8402 of the 12660 corpus values are above 31 */
    @Test
    void wholeRange() {
        PitchBendFineAMessage m = new PitchBendFineAMessage().init(0, 0xff, 0xe8, 0x3b);
        assertEquals(0, m.getVoice());
        assertEquals(59, m.getPitchBendFine());
    }

    /** the same sound source exclusive the "B" half sends, and a midi pitch bend of its own */
    @Test
    void midiEvents() throws Exception {
        MidiContext context = new MidiContext();
        context.setMfiTrackNumber(2);
        MidiEvent[] events = new PitchBendFineAMessage().init(0, 0xff, 0xe8, 0x40 | 0x23).getMidiEvents(context);
        assertEquals(2, events.length);
        assertArrayEquals(new byte[] { (byte) 0xf0, 0x45, MfiValueExclusive.MIDI_SYSEX_FUNCTION_ID_VALUE, MfiValueExclusive.PITCH_BEND_FINE, 9, 0x23, (byte) 0xf7 },
                events[0].getMessage().getMessage());

        // the coarse half is still at rest, so ((32 << 5) + 0x23) << 3) - 0x100 = 8216
        ShortMessage bend = (ShortMessage) events[1].getMessage();
        assertEquals(ShortMessage.PITCH_BEND, bend.getCommand());
        assertEquals(9, bend.getChannel());
        assertEquals(8216, (bend.getData2() << 7) | bend.getData1());
    }

    /** a fine only move must reach a plain midi synthesizer, which is the whole point of it */
    @Test
    void fineOnlyMoveIsHeard() throws Exception {
        MidiContext context = new MidiContext();
        context.setMfiTrackNumber(0);

        // the coarse half first, then nothing but 0xe8
        new PitchBendMessage().init(0, 0xff, 0xe4, 20).getMidiEvents(context);
        int before = bendOf(new PitchBendFineAMessage().init(0, 0xff, 0xe8, MidiContext.PITCH_BEND_NEUTRAL).getMidiEvents(context));
        int after = bendOf(new PitchBendFineAMessage().init(0, 0xff, 0xe8, 59).getMidiEvents(context));

        assertEquals(20 * 0x100, before);           // the fine half at rest changes nothing
        assertEquals(before + (59 - 32) * 8, after); // and a fine step is 8 of the 14 bit value
    }

    /** the last midi pitch bend of the events */
    private static int bendOf(MidiEvent[] events) {
        ShortMessage bend = (ShortMessage) events[events.length - 1].getMessage();
        assertEquals(ShortMessage.PITCH_BEND, bend.getCommand());
        return (bend.getData2() << 7) | bend.getData1();
    }

    /**
     * 0xe8 and 0xe9 write one register, so a vibrato that alternates them comes out as a
     * triangle - the 18 tick cycle {@code 67_8981100010347092588F.MLD} voice 0 repeats.
     * <p>
     * 0xe9 shares its tick with the 0xe4 that completes it, so the two are one update;
     * 0xe8 stands alone and is an update by itself. That is the whole difference between
     * the "A" and the "B" half.
     * </p>
     */
    @Test
    void oneRegister() {
        // { fine message, its value, the 0xe4 on the same tick or -1 }, as the file writes them
        int[][] cycle = {
            {0xe8, 19, -1}, {0xe8,  5, -1}, {0xe9, 23, 31},
            {0xe8, 37, -1}, {0xe8, 51, -1}, {0xe9, 32, 32},
            {0xe8, 45, -1}, {0xe8, 59, -1}, {0xe9, 41, 33},
            {0xe8, 27, -1}, {0xe8, 13, -1}, {0xe9, 32, 32},
        };
        int coarse = 32; // the 0xe4 the file sends before the cycle starts
        int fine = 32;
        List<Integer> values = new ArrayList<>();
        for (int round = 0; round < 2; round++) { // it repeats, so run it twice
            for (int[] update : cycle) {
                fine = update[0] == 0xe8
                        ? new PitchBendFineAMessage().init(0, 0xff, 0xe8, update[1]).getPitchBendFine()
                        : new PitchBendFineMessage().init(0, 0xff, 0xe9, update[1]).getPitchBendFine();
                if (update[2] >= 0) {
                    coarse = new PitchBendMessage().init(0, 0xff, 0xe4, update[2]).getPitchBend();
                }
                values.add((coarse << 5) + fine);
            }
        }
        for (int i = 1; i < values.size(); i++) {
            int step = Math.abs(values.get(i) - values.get(i - 1));
            assertTrue(step == 13 || step == 14, "step " + step + " at " + i + " of " + values);
        }
        // and it really is a triangle: it turns exactly twice per cycle
        assertEquals(1015, values.stream().mapToInt(Integer::intValue).min().orElseThrow());
        assertEquals(1097, values.stream().mapToInt(Integer::intValue).max().orElseThrow());
    }
}
