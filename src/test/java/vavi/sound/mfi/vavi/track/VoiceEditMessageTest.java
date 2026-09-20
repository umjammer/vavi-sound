/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.Track;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


/**
 * VoiceEditMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-20 nsano initial version <br>
 */
class VoiceEditMessageTest {

    /** the voice edit data, its length is what says where the next message is */
    static final byte[] voice = {
        0x01,
        0x01, 0x2b, 0x1b, (byte) 0x80, (byte) 0xec, 0x3b, 0x17, 0x00, 0x00, 0x01,
        0x21, 0x0a, (byte) 0xd3, (byte) 0xc9, 0x65, 0x1a, (byte) 0x97, (byte) 0xf8, 0x00, 0x00,
    };

    /**
     * An MFi1 file of one track: a voice edit and, after it, a tempo and a note.
     * @see vavi.sound.mfi.vavi.HeaderChunk
     */
    static byte[] file() throws Exception {
        ByteArrayOutputStream track = new ByteArrayOutputStream();
        DataOutputStream t = new DataOutputStream(track);
        t.write(new byte[] {0x00, (byte) 0xff, (byte) 0xf0});   // Δ, normal, voice edit
        t.writeShort(voice.length);
        t.write(voice);
        t.write(new byte[] {0x00, (byte) 0xff, (byte) 0xc3, (byte) 0x8d}); // tempo, timebase 48, 141
        t.write(new byte[] {0x00, 0x23, 0x30});                 // note, gate time 0x30

        ByteArrayOutputStream sub = new ByteArrayOutputStream();
        DataOutputStream s = new DataOutputStream(sub);
        s.writeBytes("sorc"); s.writeShort(1); s.write(1);
        s.writeBytes("titl"); s.writeShort(4); s.writeBytes("test");
        s.writeBytes("vers"); s.writeShort(4); s.writeBytes("0100");

        ByteArrayOutputStream file = new ByteArrayOutputStream();
        DataOutputStream f = new DataOutputStream(file);
        f.writeBytes("melo");
        f.writeInt(2 + 3 + sub.size() + 4 + 4 + track.size());
        f.writeShort(3 + sub.size());
        f.write(new byte[] {0x01, 0x01, 0x01});                 // ring tone, all, 1 track
        sub.writeTo(f);
        f.writeBytes("trac");
        f.writeInt(track.size());
        track.writeTo(f);
        return file.toByteArray();
    }

    /**
     * The length of a voice edit is what tells where the message after it is, so a track with
     * one in it parses at all only when the length is read.
     */
    @Test
    void whatComesAfterAVoiceEditIsStillParsed() throws Exception {
        Sequence sequence = MfiSystem.getSequence(new ByteArrayInputStream(file()));
        Track track = sequence.getTracks()[0];

        // the sub chunks of the header come first in the track
        int i = 0;
        while (i < track.size() && !(track.get(i).getMessage() instanceof VoiceEditMessage)) i++;

        VoiceEditMessage edit = assertInstanceOf(VoiceEditMessage.class, track.get(i).getMessage());
        assertEquals(5 + voice.length, edit.getLength());
        assertArrayEquals(voice, Arrays.copyOfRange(edit.getMessage(), 5, edit.getLength()));

        TempoMessage tempo = assertInstanceOf(TempoMessage.class, track.get(i + 1).getMessage());
        assertEquals(141, tempo.getTempo());
        assertEquals(48, tempo.getTimeBase());

        MfiEvent note = track.get(i + 2);
        assertEquals(0x23, note.getMessage().getStatus());
    }
}
