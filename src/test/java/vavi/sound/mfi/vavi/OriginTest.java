/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;
import vavi.sound.mfi.vavi.sequencer.MfiValueExclusive;
import vavi.sound.mfi.vavi.track.ChangeVoiceMessage;
import vavi.sound.mfi.vavi.track.ChannelConfigurationMessage;
import vavi.sound.mfi.vavi.track.PanpotMessage;
import vavi.sound.mfi.vavi.track.VolumeMessage;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.Track;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * The exclusives telling a synthesizer of an mfi sound source where a moved message is from.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-18 nsano initial version <br>
 */
class OriginTest {

    static byte[] exclusive(int sub, int... data) throws Exception {
        return MfiValueExclusive.message(sub, data).getMessage();
    }

    /** a percussion channel's notes go to 9, each right after where it is from */
    @Test
    void note() throws Exception {
        MidiContext context = new MidiContext();
        context.setMfiTrackNumber(2);
        new ChannelConfigurationMessage().init(0, 0xff, 0xba, 0x51).getMidiEvents(context); // 10: drum
        MidiEvent[] events = new VaviNoteMessage(0, 0x80 | 5, 12, 0x20 << 2).getMidiEvents(context); // voice 2 → 10
        assertEquals(4, events.length);
        assertArrayEquals(exclusive(MfiValueExclusive.CHANNEL, 9, 10), events[0].getMessage().getMessage());
        assertEquals(9, ((ShortMessage) events[1].getMessage()).getChannel());
        assertArrayEquals(exclusive(MfiValueExclusive.CHANNEL, 9, 10), events[2].getMessage().getMessage());
        assertEquals(events[3].getTick(), events[2].getTick());
    }

    /** a percussion note is 10 down wherever 9 went, 9 being declared a melody one here */
    @Test
    void percussionPitch() throws Exception {
        MidiContext context = new MidiContext();
        context.setMfiTrackNumber(2);
        new ChannelConfigurationMessage().init(0, 0xff, 0xba, 0x48).getMidiEvents(context); // 9: melody
        new ChannelConfigurationMessage().init(0, 0xff, 0xba, 0x51).getMidiEvents(context); // 10: drum
        MidiEvent[] events = new VaviNoteMessage(0, 0x80 | 5, 12, 0x20 << 2).getMidiEvents(context); // voice 2 → 10
        assertEquals(9, ((ShortMessage) events[1].getMessage()).getChannel());
        assertEquals(5 + 35, ((ShortMessage) events[1].getMessage()).getData1());
    }

    /** a channel of its own has nothing more */
    @Test
    void noteAsItIs() throws Exception {
        MidiContext context = new MidiContext();
        MidiEvent[] events = new VaviNoteMessage(0, 0x40 | 5, 12, 0x20 << 2).getMidiEvents(context); // voice 1
        assertEquals(2, events.length);
    }

    /** a percussion program is made 0, the mfi one goes as the exclusive */
    @Test
    void program() throws Exception {
        MidiContext context = new MidiContext();
        new ChannelConfigurationMessage().init(0, 0xff, 0xba, 0x09).getMidiEvents(context); // 1: drum
        ChangeVoiceMessage message = new ChangeVoiceMessage();
        message.init(0, 0xff, 0xe0, 0x40 | 61);
        MidiEvent[] events = message.getMidiEvents(context);
        assertEquals(4, events.length);
        assertArrayEquals(exclusive(MfiValueExclusive.CHANNEL, 9, 1), events[0].getMessage().getMessage());
        assertArrayEquals(exclusive(MfiValueExclusive.PROGRAM, 9, 61), events[1].getMessage().getMessage());
        assertArrayEquals(exclusive(MfiValueExclusive.CHANNEL, 9, 1), events[2].getMessage().getMessage());
        assertEquals(0, ((ShortMessage) events[3].getMessage()).getData1());
    }

    /** the volume of a percussion channel goes where its notes go */
    @Test
    void volume() throws Exception {
        MidiContext context = new MidiContext();
        new ChannelConfigurationMessage().init(0, 0xff, 0xba, 0x51).getMidiEvents(context); // 10: drum
        context.setMfiTrackNumber(2);
        MidiEvent[] events = new VolumeMessage().init(0, 0xff, 0xe2, 0x80 | 40).getMidiEvents(context); // voice 2 → 10
        assertEquals(2, events.length);
        assertArrayEquals(exclusive(MfiValueExclusive.CHANNEL, 9, 10), events[0].getMessage().getMessage());
        assertEquals(9, ((ShortMessage) events[1].getMessage()).getChannel());
    }

    /**
     * a melody on 9 and all of its messages go to a channel the sequence has none on, not to one
     * that is merely not configured
     */
    @Test
    void melodyOn9() throws Exception {
        vavi.sound.mfi.Sequence sequence = new vavi.sound.mfi.Sequence();
        Track[] tracks = new Track[4];
        for (int t = 0; t < 4; t++) {
            tracks[t] = sequence.createTrack();
            for (int v = 0; v < 4; v++) {
                if (t * 4 + v != 14) { // nothing on 14
                    tracks[t].add(new MfiEvent(new VaviNoteMessage(0, v << 6 | 5, 12, 0x20 << 2), 0));
                }
            }
        }
        MidiContext context = new MidiContext();
        context.setTracks(tracks);
        new ChannelConfigurationMessage().init(0, 0xff, 0xba, 0x48).getMidiEvents(context); // 9: melody
        context.setMfiTrackNumber(2);
        MidiEvent[] notes = new VaviNoteMessage(0, 0x40 | 5, 12, 0x20 << 2).getMidiEvents(context); // voice 1 → 9
        assertEquals(14, ((ShortMessage) notes[1].getMessage()).getChannel());
        assertEquals(5 + 45, ((ShortMessage) notes[1].getMessage()).getData1());
        MidiEvent[] pan = new PanpotMessage().init(0, 0xff, 0xe3, 0x40 | 10).getMidiEvents(context);
        assertEquals(14, ((ShortMessage) pan[1].getMessage()).getChannel());
        // 15, configured by nothing but played on, stays itself
        context.setMfiTrackNumber(3);
        MidiEvent[] others = new VaviNoteMessage(0, 0xc0 | 5, 12, 0x20 << 2).getMidiEvents(context);
        assertEquals(15, ((ShortMessage) others[0].getMessage()).getChannel());
    }
}
