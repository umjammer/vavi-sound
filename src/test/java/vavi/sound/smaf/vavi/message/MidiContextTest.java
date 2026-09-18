/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message;

import java.util.HashMap;
import java.util.Map;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;
import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.smaf.MetaMessage;
import vavi.sound.smaf.Sequence;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.Track;
import vavi.sound.smaf.vavi.chunk.ChannelStatus;
import vavi.sound.smaf.vavi.chunk.TrackChunk.FormatType;
import vavi.sound.smaf.vavi.message.BankSelectMessage.Significant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


/**
 * MidiContextTest, the percussion channels.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-18 nsano initial version <br>
 */
class MidiContextTest {

    static Track track(Sequence sequence, FormatType formatType, ChannelStatus.Type[] types, SmafMessage... messages) throws Exception {
        Track track = sequence.createTrack();
        Map<String, Object> props = new HashMap<>();
        props.put("formatType", formatType);
        ChannelStatus[] statuses = new ChannelStatus[types.length];
        for (int i = 0; i < types.length; i++) {
            statuses[i] = formatType == FormatType.HandyPhoneStandard ?
                    new ChannelStatus(i, (byte) types[i].ordinal()) : new ChannelStatus(i, types[i].ordinal());
        }
        props.put("channelStatuses", statuses);
        MetaMessage meta = new MetaMessage();
        meta.setMessage(MetaEvent.META_MACHINE_DEPEND.number(), props);
        track.add(new SmafEvent(meta, 0));
        for (SmafMessage message : messages) {
            track.add(new SmafEvent(message, 0));
        }
        return track;
    }

    static ChannelStatus.Type[] all(int n, ChannelStatus.Type type) {
        ChannelStatus.Type[] types = new ChannelStatus.Type[n];
        java.util.Arrays.fill(types, type);
        return types;
    }

    static MidiContext context(Track track, int number) {
        MidiContext context = new MidiContext();
        context.setSmafTrackNumber(number);
        context.setTrack(track);
        return context;
    }

    /** the midi channel the note of a smaf channel goes to */
    static int noteChannel(MidiContext context, int channel) throws Exception {
        MidiEvent[] events = new NoteMessage(0, channel, 40, 10).getMidiEvents(context);
        return ((ShortMessage) events[0].getMessage()).getChannel();
    }

    /** mobile standard: a bank select msb 0x7d before a program change makes the channel a percussion one */
    @Test
    void mobileStandardDrumBank() throws Exception {
        Sequence sequence = new Sequence();
        Track track = track(sequence, FormatType.MobileStandard_NoCompress, all(16, ChannelStatus.Type.NoCare),
                new NoteMessage(0, 8, 40, 10), new NoteMessage(0, 9, 40, 10));
        MidiContext context = context(track, 0);

        assertEquals(8, noteChannel(context, 8));
        assertEquals(9, noteChannel(context, 9));   // 9 is a percussion one until its program change

        new BankSelectMessage(0, 8, 0x7d, Significant.Most).getMidiEvents(context);
        new ProgramChangeMessage(0, 8, 0).getMidiEvents(context);
        assertEquals(9, noteChannel(context, 8));
    }

    /** mobile standard: the channel status is not what makes a percussion channel, as for the ma-3 driver */
    @Test
    void mobileStandardStatusIgnored() throws Exception {
        Sequence sequence = new Sequence();
        ChannelStatus.Type[] types = all(16, ChannelStatus.Type.NoCare);
        types[3] = ChannelStatus.Type.Rhythm;
        Track track = track(sequence, FormatType.MobileStandard_NoCompress, types, new NoteMessage(0, 3, 40, 10));
        MidiContext context = context(track, 0);

        assertEquals(3, noteChannel(context, 3));
    }

    /** mobile standard: a melody on 9 goes to a channel the track plays no note on */
    @Test
    void mobileStandardMelodyOn9() throws Exception {
        Sequence sequence = new Sequence();
        SmafMessage[] notes = new SmafMessage[16];
        for (int c = 0; c < 16; c++) {
            notes[c] = new NoteMessage(0, c == 14 ? 9 : c, 40, 10); // no note on 14
        }
        Track track = track(sequence, FormatType.MobileStandard_NoCompress, all(16, ChannelStatus.Type.NoCare), notes);
        MidiContext context = context(track, 0);

        new BankSelectMessage(0, 9, 0x7c, Significant.Most).getMidiEvents(context);
        new ProgramChangeMessage(0, 9, 5).getMidiEvents(context);
        assertEquals(14, noteChannel(context, 9));
    }

    /**
     * handy phone standard: the 10th channel is not a percussion one of its own, and a percussion
     * channel coming after a melody one has taken 9's place is not played on that one's channel
     */
    @Test
    void handyPhoneStandardNoCollision() throws Exception {
        Sequence sequence = new Sequence();
        MidiContext context = new MidiContext();
        for (int t = 0; t < 4; t++) {
            Track track = track(sequence, FormatType.HandyPhoneStandard, all(4, ChannelStatus.Type.NoCare));
            context.setSmafTrackNumber(t);
            context.setTrack(track);
        }
        // track 2 channel 1 is the 10th channel, a melody one
        context.setSmafTrackNumber(2);
        new BankSelectMessage(0, 1, 0x00).getMidiEvents(context);
        int melody = noteChannel(context, 1);
        assertNotEquals(9, melody);
        // track 3 channel 3 is the last one, a percussion one
        context.setSmafTrackNumber(3);
        new BankSelectMessage(0, 3, 0x80).getMidiEvents(context);
        assertEquals(9, noteChannel(context, 3));
        // and the melody stays where it is
        context.setSmafTrackNumber(2);
        assertEquals(melody, noteChannel(context, 1));
    }
}
