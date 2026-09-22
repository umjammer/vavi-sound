/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pmd;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import vavi.sound.pmd.PmdEvent.ControlEvent;
import vavi.sound.pmd.PmdEvent.LongEvent;
import vavi.sound.pmd.PmdEvent.NoteEvent;
import vavi.util.Debug;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * PmdReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
class PmdReaderTest {

    /** SPH-A920 ringtones */
    static final Path dir = Path.of(System.getProperty("pmd.dir", System.getProperty("user.home") + "/Public/np2/SPH-A920"));

    static boolean dirExists() {
        return Files.isDirectory(dir);
    }

    /** builds a cmid with the sub chunks and a track */
    static byte[] pmd(boolean extendedNote, byte[] track) throws IOException {
        ByteArrayOutputStream sub = new ByteArrayOutputStream();
        DataOutputStream s = new DataOutputStream(sub);
        s.writeBytes("vers"); s.writeShort(4); s.writeBytes("0500");
        s.writeBytes("note"); s.writeShort(2); s.writeShort(extendedNote ? 1 : 0);
        s.writeBytes("cnts"); s.writeShort(4); s.writeBytes("SONG");

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        DataOutputStream b = new DataOutputStream(body);
        b.writeShort(3 + sub.size());
        b.write(2); b.write(1); b.write(1);
        b.write(sub.toByteArray());
        b.writeBytes("trac"); b.writeInt(track.length); b.write(track);

        ByteArrayOutputStream file = new ByteArrayOutputStream();
        DataOutputStream f = new DataOutputStream(file);
        f.writeBytes("cmid"); f.writeInt(body.size()); f.write(body.toByteArray());
        return file.toByteArray();
    }

    static byte[] bytes(int... values) {
        byte[] b = new byte[values.length];
        for (int i = 0; i < values.length; i++) b[i] = (byte) values[i];
        return b;
    }

    @Test
    void testExtendedNote() throws Exception {
        byte[] track = bytes(
                0x00, 0xff, 0xcd, 0x78,             // tempo 480, 120 bpm
                0x00, 0xff, 0xe2, 0x7f,             // voice 1 volume 63
                0x10, 0x60, 0x30, 0xff,             // voice 1 key 32, gate 48, velocity 63, octave -1
                0x00, 0xff, 0x30, 0x00,             // voice 1 wide pitch bend 0x1000 (center)
                0x05, 0xff, 0xf1, 0x00, 0x02, 0x00, 0x44, // wave
                0x30, 0xff, 0xdf, 0x00);            // end of track
        Pmd pmd = PmdReader.read(pmd(true, track));
        assertFalse(pmd.truncated());
        assertEquals("0500", pmd.version().orElseThrow());
        assertEquals("SONG", pmd.contents().orElseThrow());
        assertTrue(pmd.isExtendedNote());
        assertEquals(1, pmd.tracks().size());

        List<PmdEvent> events = pmd.tracks().getFirst().events();
        assertEquals(6, events.size());

        ControlEvent tempo = assertInstanceOf(ControlEvent.class, events.get(0));
        assertTrue(tempo.isTempo());
        assertEquals(480, tempo.timebase());
        assertEquals(120, tempo.data());

        ControlEvent volume = assertInstanceOf(ControlEvent.class, events.get(1));
        assertEquals(ControlEvent.VOLUME, volume.status());
        assertEquals(1, volume.voice());
        assertEquals(63, volume.value());

        NoteEvent note = assertInstanceOf(NoteEvent.class, events.get(2));
        assertEquals(0x10, note.tick());
        assertEquals(1, note.voice());
        assertEquals(32, note.key());
        assertEquals(48, note.gateTime());
        assertEquals(63, note.velocity());
        assertEquals(-1, note.octaveShift());
        assertEquals(32 + 45 - 12, note.midiNote());

        ControlEvent bend = assertInstanceOf(ControlEvent.class, events.get(3));
        assertTrue(bend.isWidePitchBend());
        assertEquals(1, bend.voice());
        assertEquals(0x1000, bend.value());

        LongEvent wave = assertInstanceOf(LongEvent.class, events.get(4));
        assertEquals(LongEvent.WAVE, wave.status());
        assertEquals(2, wave.data().length);

        ControlEvent eot = assertInstanceOf(ControlEvent.class, events.get(5));
        assertEquals(ControlEvent.END_OF_TRACK, eot.status());
        assertEquals(0x10 + 5 + 0x30, eot.tick());
    }

    @Test
    void testShortNote() throws Exception {
        byte[] track = bytes(
                0x00, 0x0c, 0x18,                   // voice 0 key 12, gate 24
                0x18, 0xff, 0xdf, 0x00);
        Pmd pmd = PmdReader.read(pmd(false, track));
        assertFalse(pmd.isExtendedNote());
        NoteEvent note = assertInstanceOf(NoteEvent.class, pmd.tracks().getFirst().events().getFirst());
        assertEquals(-1, note.velocity());
        assertEquals(127, note.midiVelocity());
        assertEquals(57, note.midiNote());
        assertEquals(2, pmd.tracks().getFirst().events().size());
    }

    @Test
    void testTruncated() throws Exception {
        byte[] data = pmd(true, bytes(0x00, 0x0c, 0x18, 0x00, 0x18, 0xff, 0xdf, 0x00));
        Pmd pmd = PmdReader.read(java.util.Arrays.copyOf(data, data.length - 3));
        assertTrue(pmd.truncated());
        assertEquals(1, pmd.tracks().getFirst().events().size());
    }

    @Test
    void testNotPmd() {
        assertThrows(InvalidPmdDataException.class, () -> PmdReader.read("melo0000000000000".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    @EnabledIf("dirExists")
    void testCorpus() throws Exception {
        List<Path> paths;
        try (Stream<Path> s = Files.list(dir)) {
            paths = s.filter(p -> p.toString().toLowerCase().endsWith(".pmd")).sorted().toList();
        }
        for (Path path : paths) {
            Pmd pmd = PmdReader.read(Files.readAllBytes(path));
Debug.println(path.getFileName() + ": " + pmd.contents().orElse("?") + ", tracks: " + pmd.tracks().size() + ", events: " + pmd.tracks().stream().mapToInt(t -> t.events().size()).sum());
            assertFalse(pmd.truncated(), path.toString());
            assertEquals(pmd.tracksCount(), pmd.tracks().size(), path.toString());
            for (PmdTrack track : pmd.tracks()) {
                assertInstanceOf(ControlEvent.class, track.events().getLast(), path.toString());
                assertEquals(ControlEvent.END_OF_TRACK, ((ControlEvent) track.events().getLast()).status(), path.toString());
            }
        }
    }
}
