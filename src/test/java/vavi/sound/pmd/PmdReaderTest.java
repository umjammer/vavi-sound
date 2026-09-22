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
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

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

    @Test
    void testToMidi() throws Exception {
        byte[] track = bytes(
                0x00, 0xff, 0xcd, 0x78,             // tempo 480, 120 bpm
                0x00, 0xff, 0xe1, 0x43,             // voice 1 bank 3
                0x00, 0xff, 0xe0, 0x49,             // voice 1 program 9 -> 73
                0x00, 0xff, 0xe2, 0x60,             // voice 1 volume 32 -> 64
                0x10, 0x60, 0x30, 0xff,             // voice 1 key 32 octave -1 -> 65, gate 48, velocity 63 -> 126
                0x00, 0xff, 0x3f, 0xff,             // voice 1 wide pitch bend 0x1fff -> 0x3ffe
                0x30, 0xff, 0xdf, 0x00);            // end of track
        Sequence sequence = PmdMidiConverter.toMidiSequence(PmdReader.read(pmd(true, track)));
        assertEquals(480, sequence.getResolution());
        Track t = sequence.getTracks()[0];
        List<MidiEvent> events = new java.util.ArrayList<>();
        for (int i = 0; i < t.size(); i++) events.add(t.get(i));

        MetaMessage tempo = (MetaMessage) events.get(0).getMessage();
        assertEquals(0x51, tempo.getType());
        assertEquals(500000, ((tempo.getData()[0] & 0xff) << 16) | ((tempo.getData()[1] & 0xff) << 8) | (tempo.getData()[2] & 0xff));

        List<ShortMessage> shorts = events.stream().map(MidiEvent::getMessage)
                .filter(m -> m instanceof ShortMessage).map(m -> (ShortMessage) m).toList();
        ShortMessage program = shorts.stream().filter(m -> m.getCommand() == ShortMessage.PROGRAM_CHANGE).toList().getLast();
        assertEquals(1, program.getChannel());
        assertEquals(73, program.getData1());
        ShortMessage volume = shorts.stream().filter(m -> m.getCommand() == ShortMessage.CONTROL_CHANGE).findFirst().orElseThrow();
        assertEquals(7, volume.getData1());
        assertEquals(64, volume.getData2());
        MidiEvent on = events.stream().filter(e -> e.getMessage() instanceof ShortMessage m && m.getCommand() == ShortMessage.NOTE_ON).findFirst().orElseThrow();
        assertEquals(0x10, on.getTick());
        assertEquals(65, ((ShortMessage) on.getMessage()).getData1());
        assertEquals(126, ((ShortMessage) on.getMessage()).getData2());
        MidiEvent off = events.stream().filter(e -> e.getMessage() instanceof ShortMessage m && m.getCommand() == ShortMessage.NOTE_OFF).findFirst().orElseThrow();
        assertEquals(0x10 + 48, off.getTick());
        ShortMessage bend = shorts.stream().filter(m -> m.getCommand() == ShortMessage.PITCH_BEND).findFirst().orElseThrow();
        assertEquals(0x3ffe, (bend.getData2() << 7) | bend.getData1());
        assertEquals(0x10 + 0x30, t.ticks());
    }

    @Test
    @EnabledIf("dirExists")
    void testCorpusToMidi() throws Exception {
        List<Path> paths;
        try (Stream<Path> s = Files.list(dir)) {
            paths = s.filter(p -> p.toString().toLowerCase().endsWith(".pmd")).sorted().toList();
        }
        for (Path path : paths) {
            // through the spi
            Sequence sequence = MidiSystem.getSequence(path.toFile());
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            MidiSystem.write(sequence, 1, baos);
            Sequence midi = MidiSystem.getSequence(new java.io.ByteArrayInputStream(baos.toByteArray()));
            int notes = 0;
            for (Track t : midi.getTracks()) {
                for (int i = 0; i < t.size(); i++) {
                    if (t.get(i).getMessage() instanceof ShortMessage m && m.getCommand() == ShortMessage.NOTE_ON) notes++;
                }
            }
            long expected = PmdReader.read(Files.readAllBytes(path)).tracks().stream()
                    .flatMap(t -> t.events().stream()).filter(e -> e instanceof NoteEvent).count();
Debug.println(path.getFileName() + ": " + midi.getMicrosecondLength() / 1000 + " ms, notes: " + notes);
            assertEquals(expected, notes, path.toString());
        }
    }
}
