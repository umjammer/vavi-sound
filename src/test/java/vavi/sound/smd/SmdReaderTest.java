/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import vavi.sound.smd.SmdEvent.CommandEvent;
import vavi.sound.smd.SmdEvent.NoteEvent;
import vavi.sound.smd.SmdEvent.RestEvent;
import vavi.sound.smd.SmdEvent.TempoEvent;
import vavi.util.Debug;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * SmdReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
class SmdReaderTest {

    /** J-SKY melodies */
    static final Path dir = Path.of(System.getProperty("smd.dir", System.getProperty("user.home") + "/Public/np2/smd"));

    static boolean dirExists() {
        return Files.isDirectory(dir);
    }

    static byte[] bytes(Object... values) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Object o : values) {
            if (o instanceof String s) baos.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
            else baos.write((Integer) o);
        }
        return baos.toByteArray();
    }

    static final byte[] SAMPLE = bytes(
            0x1b, "$B", 0x24, 0x4c, 0x1b, "(B", "A", 0x0e, 0x31, 0x0f,  // "ぬAｱ"
            0x1b, "$D",
            "!(",                       // tempo 150
            0x2a, "\"", 0x2b,           // A4 quarter tied with A4 half
            "$",                        // 8th rest
            "!#", "!&",                 // octave -1, triplet
            0x28, 0x2c, 0x30,           // A3, A#3, B3 16th
            0x0f,
            0x1b, "$D", "!%", 0x29, "%", 0x0f); // A4 8th three times as a triplet, quarter rest

    @Test
    void test() throws Exception {
        Smd smd = SmdReader.read(SAMPLE);
        assertEquals("ぬAｱ", smd.title());
        assertEquals(2, smd.parts().size());
        assertFalse(smd.truncated());

        List<SmdEvent> events = smd.parts().getFirst().events();
        assertEquals(150, assertInstanceOf(TempoEvent.class, events.get(0)).bpm());
        NoteEvent note = assertInstanceOf(NoteEvent.class, events.get(1));
        assertEquals(69, note.key());
        assertEquals(36, note.length());
        RestEvent rest = assertInstanceOf(RestEvent.class, events.get(2));
        assertEquals(36, rest.tick());
        assertEquals(6, rest.length());
        assertEquals('#', assertInstanceOf(CommandEvent.class, events.get(3)).command());
        List<NoteEvent> triplet = events.stream().skip(5).map(NoteEvent.class::cast).toList();
        assertEquals(List.of(57, 58, 59), triplet.stream().map(NoteEvent::key).toList());
        assertEquals(List.of(42L, 44L, 46L), triplet.stream().map(NoteEvent::tick).toList());
        assertEquals(48, smd.parts().getFirst().length());

        List<SmdEvent> second = smd.parts().get(1).events();
        List<NoteEvent> repeated = second.stream().filter(e -> e instanceof NoteEvent).map(NoteEvent.class::cast).toList();
        assertEquals(3, repeated.size());
        assertEquals(List.of(0L, 4L, 8L), repeated.stream().map(NoteEvent::tick).toList());
        assertEquals(12, assertInstanceOf(RestEvent.class, second.getLast()).tick());
        assertEquals(24, smd.parts().get(1).length());
    }

    @Test
    void testNotSmd() {
        assertThrows(InvalidSmdDataException.class, () -> SmdReader.read(bytes("MMMD", 0, 0, 0, 0, 0x1b, "$D")));
        assertThrows(InvalidSmdDataException.class, () -> SmdReader.read(bytes("title only")));
    }

    @Test
    void testToMidi() throws Exception {
        Sequence sequence = MidiSystem.getSequence(new ByteArrayInputStream(SAMPLE));
        assertEquals(Smd.RESOLUTION, sequence.getResolution());
        assertEquals(2, sequence.getTracks().length);
        Track t = sequence.getTracks()[1];
        List<ShortMessage> ons = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).getMessage() instanceof ShortMessage m && m.getCommand() == ShortMessage.NOTE_ON) ons.add(m);
        }
        assertEquals(3, ons.size());
        assertEquals(1, ons.getFirst().getChannel());
        // tempo 150
        assertEquals(sequence.getTickLength() * 400_000 / Smd.RESOLUTION, sequence.getMicrosecondLength());
    }

    @Test
    @EnabledIf("dirExists")
    void testCorpus() throws Exception {
        List<Path> paths;
        try (Stream<Path> s = Files.list(dir)) {
            paths = s.filter(p -> p.toString().toLowerCase().matches(".+\\.sm[dz]")).sorted().toList();
        }
        int read = 0;
        int unequal = 0;
        for (Path path : paths) {
            byte[] data = Files.readAllBytes(path);
            if (!SmdReader.isSmd(data)) continue;
            Smd smd = SmdReader.read(data);
            read++;
            assertFalse(smd.parts().isEmpty(), path.toString());
            long max = smd.parts().stream().mapToLong(SmdPart::length).max().orElse(0);
            long min = smd.parts().stream().mapToLong(SmdPart::length).min().orElse(0);
            if (max - min > Smd.RESOLUTION) unequal++;
            // through the spi and SMF
            Sequence sequence = MidiSystem.getSequence(path.toFile());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MidiSystem.write(sequence, 1, baos);
            MidiSystem.getSequence(new ByteArrayInputStream(baos.toByteArray()));
        }
// parts may end earlier than others or be empty, as written
Debug.println("read: " + read + ", parts of unequal length: " + unequal);
        assertTrue(read > 0);
    }
}
