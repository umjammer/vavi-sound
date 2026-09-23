/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import vavi.sound.mfm.MfmEvent.NoteEvent;
import vavi.sound.mfm.MfmEvent.PitchBendEvent;
import vavi.sound.mfm.MfmEvent.ShortEvent;
import vavi.util.Debug;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * MfmReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
class MfmReaderTest {

    /** Faith Ring Tone Authoring Tool samples */
    static final Path dir = Path.of(System.getProperty("mfm.dir",
            System.getProperty("user.home") + "/.wine/drive_c/Program Files (x86)/Faith/Ring Tone Authoring Tool/Sample"));

    static boolean dirExists() {
        return Files.isDirectory(dir);
    }

    static byte[] bytes(int... values) {
        byte[] b = new byte[values.length];
        for (int i = 0; i < values.length; i++) b[i] = (byte) values[i];
        return b;
    }

    /** builds a mfmp with titl and note sub chunks, a wave and a track */
    static byte[] mfm(byte[] track) throws IOException {
        ByteArrayOutputStream sub = new ByteArrayOutputStream();
        DataOutputStream s = new DataOutputStream(sub);
        s.writeBytes("titl"); s.writeShort(4); s.writeBytes("test");
        s.writeBytes("note"); s.writeShort(2); s.writeShort(1);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        DataOutputStream b = new DataOutputStream(body);
        b.write(1); b.write(0); b.write(0); b.write(2);
        b.writeShort(sub.size());
        b.write(sub.toByteArray());
        b.writeBytes("wave"); b.writeInt(2 + 4 + 5); b.writeShort(1); b.writeInt(5); b.write(bytes(0x00, 0x05, 0x00, 0x08, 0x80));
        b.writeBytes("trac"); b.writeInt(track.length); b.write(track);

        ByteArrayOutputStream file = new ByteArrayOutputStream();
        DataOutputStream f = new DataOutputStream(file);
        f.writeBytes("mfmp"); f.writeInt(body.size()); f.write(body.toByteArray());
        return file.toByteArray();
    }

    /** made after rt_smf2mfmp.exe outputs */
    static final byte[] TRACK = bytes(
            0x00, 0xff, 0xbf, 0x64,         // tempo 120
            0x00, 0xff, 0xd1, 0x01,         // bank 2: 1 (program + 64)
            0x00, 0xff, 0xd2, 0x06,         // program 6 -> 70
            0x00, 0x0f, 0xff, 0xc8,         // C4, gate 255, velocity 50
            0xfe, 0xff, 0x00, 0x0f, 0x5c,   // extends C4 by 92 at 254
            0x00, 0x3f, 0x3f, 0xff,         // pitch bend 0x3fff
            0x00, 0x7f, 0x00, 0x32, 0x00,   // audio play wave 0
            0x00, 0x2b, 0x01, 0xc9,         // 108 (base 65)
            0x00, 0xff, 0xb0, 0x01,         // time skip 256
            0x10, 0xff, 0xb1, 0x00);        // end of track

    @Test
    void test() throws Exception {
        Mfm mfm = MfmReader.read(mfm(TRACK));
        assertFalse(mfm.truncated());
        assertEquals("test", mfm.title().orElseThrow());
        assertEquals(1, mfm.noteLength());
        assertEquals(48, mfm.timebase());
        assertEquals(1, mfm.waves().size());
        assertEquals(8000, mfm.waves().getFirst().sampleRate());
        assertEquals(2, mfm.waves().getFirst().data().length);

        List<MfmEvent> events = mfm.tracks().getFirst().events();
        assertEquals(10, events.size());
        NoteEvent c4 = assertInstanceOf(NoteEvent.class, events.get(3));
        assertEquals(60, c4.key());
        assertEquals(255, c4.gateTime());
        assertEquals(100, c4.midiVelocity());
        ShortEvent extension = assertInstanceOf(ShortEvent.class, events.get(4));
        assertTrue(extension.isNoteExtension());
        assertEquals(254, extension.tick());
        assertEquals(0x3fff, assertInstanceOf(PitchBendEvent.class, events.get(5)).value());
        assertEquals(ShortEvent.CLASS_AUDIO, assertInstanceOf(ShortEvent.class, events.get(6)).status());
        assertEquals(108, assertInstanceOf(NoteEvent.class, events.get(7)).key());
        assertEquals(254 + 256 + 0x10, events.getLast().tick());
    }

    @Test
    void testToMidi() throws Exception {
        Sequence sequence = MidiSystem.getSequence(new ByteArrayInputStream(mfm(TRACK)));
        assertEquals(48, sequence.getResolution());
        Track t = sequence.getTracks()[0];
        List<MidiEvent> events = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) events.add(t.get(i));
        ShortMessage program = events.stream().map(MidiEvent::getMessage).filter(m -> m instanceof ShortMessage s && s.getCommand() == ShortMessage.PROGRAM_CHANGE)
                .map(ShortMessage.class::cast).findFirst().orElseThrow();
        assertEquals(70, program.getData1());
        MidiEvent off = events.stream().filter(e -> e.getMessage() instanceof ShortMessage s && s.getCommand() == ShortMessage.NOTE_OFF && s.getData1() == 60)
                .findFirst().orElseThrow();
        assertEquals(254 + 92, off.getTick());
        assertEquals(254 + 256 + 0x10, t.ticks());
        // tempo 120
        assertEquals(500_000L * t.ticks() / 48, sequence.getMicrosecondLength());
    }

    @Test
    void testNotMfm() {
        assertThrows(InvalidMfmDataException.class, () -> MfmReader.read("melo0000000000000".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    @EnabledIf("dirExists")
    void testSamples() throws Exception {
        List<Path> paths;
        try (Stream<Path> s = Files.list(dir)) {
            paths = s.filter(p -> p.toString().endsWith(".mfm")).sorted().toList();
        }
        for (Path path : paths) {
            Mfm mfm = MfmReader.read(Files.readAllBytes(path));
Debug.println(path.getFileName() + ": tracks: " + mfm.tracks().size() + ", waves: " + mfm.waves() + ", voices: " + mfm.voices().size());
            assertFalse(mfm.truncated(), path.toString());
            assertEquals(mfm.chunksCount(), mfm.tracks().size() + (mfm.waves().isEmpty() ? 0 : 1) + (mfm.voices().isEmpty() ? 0 : 1), path.toString());
            for (MfmTrack track : mfm.tracks()) {
                ShortEvent eot = assertInstanceOf(ShortEvent.class, track.events().getLast(), path.toString());
                assertEquals(ShortEvent.END_OF_TRACK, eot.command());
            }
            Sequence sequence = MidiSystem.getSequence(path.toFile());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MidiSystem.write(sequence, 1, baos);
            MidiSystem.getSequence(new ByteArrayInputStream(baos.toByteArray()));
        }
    }

    /** notes (tick, channel, key) of a sequence in 48 ticks per quarter */
    static Map<String, Integer> notes(Sequence sequence) {
        Map<String, Integer> notes = new HashMap<>();
        for (Track t : sequence.getTracks()) {
            for (int i = 0; i < t.size(); i++) {
                if (t.get(i).getMessage() instanceof ShortMessage m && m.getCommand() == ShortMessage.NOTE_ON && m.getData2() > 0) {
                    long tick = Math.round(t.get(i).getTick() * 48.0 / sequence.getResolution());
                    notes.merge(tick + ":" + m.getChannel() + ":" + m.getData1(), 1, Integer::sum);
                }
            }
        }
        return notes;
    }

    @Test
    @EnabledIf("dirExists")
    void testAgainstSource() throws Exception {
        // the sample and its source SMF
        Map<String, Integer> expected = notes(MidiSystem.getSequence(dir.resolve("Type2_SMF2MFMP_SAMPLE.mid").toFile()));
        Map<String, Integer> actual = notes(MidiSystem.getSequence(dir.resolve("Type2_SMF2MFMP_SAMPLE.mfm").toFile()));
        int all = actual.values().stream().mapToInt(Integer::intValue).sum();
        int matched = actual.entrySet().stream().mapToInt(e -> Math.min(e.getValue(), expected.getOrDefault(e.getKey(), 0))).sum();
Debug.println("notes: " + all + ", matched: " + matched);
        // the others are channel 10 notes made into audio, LED and vibration, and 1 tick rounding
        assertTrue(matched >= all - 8, matched + "/" + all);
    }
}
