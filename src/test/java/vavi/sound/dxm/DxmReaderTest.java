/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.dxm;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import vavi.sound.dxm.DxmEvent.ChannelEvent;
import vavi.sound.dxm.DxmEvent.MetaEvent;
import vavi.util.Debug;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * DxmReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
class DxmReaderTest {

    /** feel sound box */
    static final Path dir = Path.of(System.getProperty("dxm.dir", System.getProperty("user.home") + "/Public/np2/feel sound box (dxm)"));

    static boolean dirExists() {
        return Files.isDirectory(dir);
    }

    /** builds a MCDF with a title and a sequence of one track */
    static byte[] dxm(byte[] track) throws IOException {
        ByteArrayOutputStream seq = new ByteArrayOutputStream();
        DataOutputStream s = new DataOutputStream(seq);
        s.writeBytes("CThd"); s.writeInt(6); s.writeShort(0); s.writeShort(1); s.writeShort(24);
        s.writeBytes("CTrk"); s.writeInt(track.length); s.write(track);
        byte[] title = "テスト".getBytes(Charset.forName("MS932"));

        int indexSize = 3 * 10 + 2;
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        DataOutputStream f = new DataOutputStream(file);
        f.writeBytes("MCDF");
        int offset = 4 + indexSize;
        f.writeShort(Dxm.ID_VERSION); f.writeInt(offset); f.writeInt(4);
        f.writeShort(Dxm.ID_TITLE); f.writeInt(offset + 4); f.writeInt(title.length);
        f.writeShort(Dxm.ID_SEQUENCE); f.writeInt(offset + 4 + title.length); f.writeInt(seq.size());
        f.writeShort(0xffff);
        f.writeBytes("01.0");
        f.write(title);
        f.write(seq.toByteArray());
        return file.toByteArray();
    }

    static byte[] bytes(int... values) {
        byte[] b = new byte[values.length];
        for (int i = 0; i < values.length; i++) b[i] = (byte) values[i];
        return b;
    }

    @Test
    void test() throws Exception {
        byte[] track = bytes(
                0x00, 0xff, 0x51, 0x03, 0x07, 0xa1, 0x20, // tempo 500000
                0x00, 0xc0, 0x49,                         // program
                0x00, 0x90, 0x3c, 0x7f,                   // note on
                0x00, 0x40, 0x60,                         // running status note on
                0x18, 0x80, 0x3c,                         // note off, no velocity
                0x00, 0xe0, 0x50,                         // pitch bend, msb only
                0x81, 0x00, 0x40,                         // running status pitch bend, delta 128
                0x00, 0xff, 0x2f, 0x00);                  // end of track
        Dxm dxm = DxmReader.read(dxm(track));
        assertFalse(dxm.truncated());
        assertEquals("テスト", dxm.title().orElseThrow());
        assertEquals("01.0", dxm.entry(Dxm.ID_VERSION).orElseThrow().text());
        assertEquals(24, dxm.division());
        assertEquals(1, dxm.tracks().size());

        List<DxmEvent> events = dxm.tracks().getFirst().events();
        assertEquals(8, events.size());
        assertEquals(500000, assertInstanceOf(MetaEvent.class, events.get(0)).tempo());
        assertEquals(-1, assertInstanceOf(ChannelEvent.class, events.get(1)).data2());
        assertTrue(assertInstanceOf(ChannelEvent.class, events.get(2)).isNoteOn());
        ChannelEvent running = assertInstanceOf(ChannelEvent.class, events.get(3));
        assertEquals(0x90, running.status());
        assertEquals(0x40, running.data1());
        ChannelEvent off = assertInstanceOf(ChannelEvent.class, events.get(4));
        assertTrue(off.isNoteOff());
        assertEquals(-1, off.data2());
        assertEquals(0x18, off.tick());
        assertEquals(0x50 << 7, assertInstanceOf(ChannelEvent.class, events.get(5)).pitchBend());
        ChannelEvent bend = assertInstanceOf(ChannelEvent.class, events.get(6));
        assertEquals(128, bend.delta());
        assertEquals(0x2000, bend.pitchBend());
        assertEquals(MetaEvent.END_OF_TRACK, assertInstanceOf(MetaEvent.class, events.get(7)).type());
    }

    @Test
    void testTruncated() throws Exception {
        byte[] data = dxm(bytes(0x00, 0x90, 0x3c, 0x7f, 0x18, 0x80, 0x3c, 0x00, 0xff, 0x2f, 0x00));
        Dxm dxm = DxmReader.read(Arrays.copyOf(data, data.length - 5));
        assertTrue(dxm.truncated());
        assertEquals(1, dxm.tracks().getFirst().events().size());
    }

    @Test
    void testNotDxm() {
        assertThrows(InvalidDxmDataException.class, () -> DxmReader.read(bytes('M', 'T', 'h', 'd', 0, 0, 0, 6)));
    }

    @Test
    @EnabledIf("dirExists")
    void testCorpus() throws Exception {
        List<Path> paths;
        try (Stream<Path> s = Files.walk(dir)) {
            paths = s.filter(p -> p.toString().toLowerCase().endsWith(".dxm")).sorted().toList();
        }
        int read = 0;
        List<Path> truncated = new ArrayList<>();
        for (Path path : paths) {
            byte[] data = Files.readAllBytes(path);
            if (!DxmReader.isDxm(data)) {
Debug.println("not MCDF: " + path);
                continue;
            }
            Dxm dxm = DxmReader.read(data);
            read++;
            if (dxm.truncated()) {
                truncated.add(path);
            }
        }
Debug.println("read: " + read + ", truncated: " + truncated.size());
truncated.forEach(p -> Debug.println("truncated: " + p));
        // the corpus has 1047 MCDF files, 3 of them are damaged
        assertTrue(read > 0);
        assertTrue(truncated.size() <= 3, truncated.toString());
    }

    @Test
    void testToMidi() throws Exception {
        byte[] track = bytes(
                0x00, 0x90, 0x3c, 0x7f,                   // note on
                0x18, 0x80, 0x3c,                         // note off, no velocity
                0x00, 0xe0, 0x50,                         // pitch bend, msb only
                0x00, 0xff, 0x2f, 0x00);                  // end of track
        Sequence sequence = DxmMidiConverter.toMidiSequence(DxmReader.read(dxm(track)));
        assertEquals(24, sequence.getResolution());
        Track t = sequence.getTracks()[0];
        List<ShortMessage> shorts = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) if (t.get(i).getMessage() instanceof ShortMessage m) shorts.add(m);
        assertEquals(3, shorts.size());
        assertEquals(ShortMessage.NOTE_OFF, shorts.get(1).getCommand());
        assertEquals(0, shorts.get(1).getData2());
        assertEquals(0, shorts.get(2).getData1());
        assertEquals(0x50, shorts.get(2).getData2());
        assertEquals(0x18, t.ticks());
        assertEquals("テスト", new String(((javax.sound.midi.MetaMessage) t.get(0).getMessage()).getData(), Charset.forName("MS932")));
    }

    @Test
    @EnabledIf("dirExists")
    void testCorpusToMidi() throws Exception {
        List<Path> paths;
        try (Stream<Path> s = Files.walk(dir)) {
            paths = s.filter(p -> p.toString().toLowerCase().endsWith(".dxm")).sorted().toList();
        }
        int converted = 0;
        for (Path path : paths) {
            byte[] data = Files.readAllBytes(path);
            if (!DxmReader.isDxm(data)) continue;
            // through the spi
            Sequence sequence = MidiSystem.getSequence(path.toFile());
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            MidiSystem.write(sequence, MidiSystem.getMidiFileTypes(sequence)[0], baos);
            MidiSystem.getSequence(new java.io.ByteArrayInputStream(baos.toByteArray()));
            converted++;
        }
Debug.println("converted: " + converted);
        assertTrue(converted > 0);
    }
}
