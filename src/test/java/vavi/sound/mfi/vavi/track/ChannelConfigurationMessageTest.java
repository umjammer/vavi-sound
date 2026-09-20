/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.MidiEvent;

import org.junit.jupiter.api.Test;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.sequencer.MfiValueExclusive;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * ChannelConfigurationMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-18 nsano initial version <br>
 */
class ChannelConfigurationMessageTest {

    /** a percussion channel goes to the midi drum channel, the byte along with it */
    @Test
    void midiEventsPercussion() throws Exception {
        MidiContext context = new MidiContext();
        // channel 10, family mode 1
        MidiEvent[] events = new ChannelConfigurationMessage().init(0, 0xff, 0xba, 0x51).getMidiEvents(context);
        assertEquals(1, events.length);
        assertArrayEquals(new byte[] { (byte) 0xf0, 0x45, MfiValueExclusive.MIDI_SYSEX_FUNCTION_ID_VALUE, MfiValueExclusive.CHANNEL_CONFIGURATION, 9, 0x51, 0, (byte) 0xf7 },
                events[0].getMessage().getMessage());
    }

    /** a melody channel keeps its own */
    @Test
    void midiEventsMelody() throws Exception {
        MidiContext context = new MidiContext();
        // channel 3, family mode 0
        MidiEvent[] events = new ChannelConfigurationMessage().init(0, 0xff, 0xba, 0x18).getMidiEvents(context);
        assertArrayEquals(new byte[] { (byte) 0xf0, 0x45, MfiValueExclusive.MIDI_SYSEX_FUNCTION_ID_VALUE, MfiValueExclusive.CHANNEL_CONFIGURATION, 3, 0x18, 0, (byte) 0xf7 },
                events[0].getMessage().getMessage());
    }
}
