/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.smaf.SmafSystem;
import vavi.sound.smaf.Track;
import vavi.sound.smaf.vavi.VaviSmafFileFormat;
import vavi.sound.smaf.vavi.message.NoteMessage;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * SmafContextTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class SmafContextTest {

    static Path dir;

    @BeforeAll
    static void setup() throws Exception {
        dir = Paths.get("tmp");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    @Test
    @DisplayName("converts a midi sequence to a smaf sequence and writes it")
    public void test() throws Exception {
        Path inPath = Paths.get(SmafContextTest.class.getResource("/test.mid").toURI());
        javax.sound.midi.Sequence midiSequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(inPath)));
        MidiFileFormat midiFileFormat = MidiSystem.getMidiFileFormat(new BufferedInputStream(Files.newInputStream(inPath)));
        int type = midiFileFormat.getType();
Debug.println(Level.FINE, "type: " + type);
        vavi.sound.smaf.Sequence smafSequence = SmafSystem.toSmafSequence(midiSequence, type);

        Path outPath = dir.resolve("SmafContextTest.mmf");
        int r = SmafSystem.write(smafSequence, VaviSmafFileFormat.FILE_TYPE, Files.newOutputStream(outPath));
Debug.println(Level.FINE, "write: " + r);
        assertEquals(r, (int) Files.size(outPath));

        // what has been written is a smaf file again
        vavi.sound.smaf.Sequence readSequence = SmafSystem.getSequence(outPath.toFile());
        assertEquals(smafSequence.getTracks().length, readSequence.getTracks().length);

        // and the notes of the midi sequence are in it
        assertTrue(notes(readSequence) > 0);
        assertEquals(noteOns(midiSequence), notes(readSequence));
    }

    @Test
    @DisplayName("the midi channels become smaf tracks, drums and tempo changes survive")
    public void test2() throws Exception {
        javax.sound.midi.Sequence midiSequence = new javax.sound.midi.Sequence(javax.sound.midi.Sequence.PPQ, 480, 3);
        javax.sound.midi.Track[] midiTracks = midiSequence.getTracks();
        midiTracks[0].add(tempo(0, 500_000));           // a quarter note = 120
        midiTracks[0].add(tempo(480 * 4, 1_000_000));   // a quarter note = 60 from the 5th beat
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            // one note per beat on 3 midi channels, which are 3 smaf tracks (channel / 4)
            note(midiTracks[0], 0, 60 + i, 480 * i, 480);
            note(midiTracks[1], 4, 48 + i, 480 * i, 240);
            note(midiTracks[2], 9, i % 2 == 0 ? 36 : 42, 480 * i, 60);
            // the beats after the tempo change are twice as long, a tick of the smaf
            // sequence is a millisecond (its resolution is 120 * Timebase_D)
            long tick = i < 4 ? i * 500 : 2000 + (i - 4) * 1000;
            expected.add(tick + ":0:" + (60 + i));
            expected.add(tick + ":4:" + (48 + i));
            expected.add(tick + ":9:" + (i % 2 == 0 ? 36 : 42));
        }

        vavi.sound.smaf.Sequence smafSequence = SmafSystem.toSmafSequence(midiSequence, 1);
        assertEquals(3, smafSequence.getTracks().length);
        assertEquals(24, notes(smafSequence));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int r = SmafSystem.write(smafSequence, VaviSmafFileFormat.FILE_TYPE, baos);
        assertEquals(r, baos.size());

        // the notes of the smaf file we have written are those of the midi sequence
        vavi.sound.smaf.Sequence readSequence = SmafSystem.getSequence(new ByteArrayInputStream(baos.toByteArray()));
        javax.sound.midi.Sequence backSequence = SmafSystem.toMidiSequence(readSequence);
        assertEquals(expected.stream().sorted().toList(), noteOnsOf(backSequence).stream().sorted().toList());
        // and they are at the same time, the tempo is baked into the durations
        assertEquals(midiSequence.getMicrosecondLength(), backSequence.getMicrosecondLength(), 50_000d);
    }

    /** a tempo meta event */
    private static MidiEvent tempo(long tick, int microsecondsPerBeat) throws Exception {
        javax.sound.midi.MetaMessage metaMessage = new javax.sound.midi.MetaMessage();
        metaMessage.setMessage(MetaEvent.META_TEMPO.number(), new byte[] {
                (byte) (microsecondsPerBeat >> 16), (byte) (microsecondsPerBeat >> 8), (byte) microsecondsPerBeat }, 3);
        return new MidiEvent(metaMessage, tick);
    }

    /** a note on and its note off */
    private static void note(javax.sound.midi.Track midiTrack, int channel, int pitch, long tick, long length) throws Exception {
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.NOTE_ON, channel, pitch, 100);
        midiTrack.add(new MidiEvent(shortMessage, tick));
        shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.NOTE_OFF, channel, pitch, 0);
        midiTrack.add(new MidiEvent(shortMessage, tick + length));
    }

    /** "tick:channel:pitch" of every note on of all the tracks */
    private static List<String> noteOnsOf(javax.sound.midi.Sequence midiSequence) {
        List<String> noteOns = new ArrayList<>();
        for (javax.sound.midi.Track track : midiSequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent midiEvent = track.get(i);
                if (midiEvent.getMessage() instanceof ShortMessage shortMessage &&
                    shortMessage.getCommand() == ShortMessage.NOTE_ON &&
                    shortMessage.getData2() != 0) {

                    noteOns.add(midiEvent.getTick() + ":" + shortMessage.getChannel() + ":" + shortMessage.getData1());
                }
            }
        }
        return noteOns;
    }

    /** counts the note messages of all the tracks */
    private static int notes(vavi.sound.smaf.Sequence smafSequence) {
        int notes = 0;
        for (Track track : smafSequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                if (track.get(i).getMessage() instanceof NoteMessage) {
                    notes++;
                }
            }
        }
        return notes;
    }

    /** counts the note on events of all the tracks */
    private static int noteOns(javax.sound.midi.Sequence midiSequence) {
        int notes = 0;
        for (javax.sound.midi.Track track : midiSequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                if (track.get(i).getMessage() instanceof ShortMessage shortMessage &&
                    shortMessage.getCommand() == ShortMessage.NOTE_ON &&
                    shortMessage.getData2() != 0) {
                    notes++;
                }
            }
        }
        return notes;
    }

    // ----

    /**
     * Converts the midi file to a smaf file.
     * <pre>
     * usage:
     *  % java SmafContext in_midi_file out_mmf_file
     * </pre>
     */
    public static void main(String[] args) throws Exception {

Debug.println("midi in: " + args[0]);
Debug.println("smaf out: " + args[1]);

        File file = new File(args[0]);
        javax.sound.midi.Sequence midiSequence = MidiSystem.getSequence(file);
        MidiFileFormat midiFileFormat = MidiSystem.getMidiFileFormat(file);
        int type = midiFileFormat.getType();
Debug.println("type: " + type);
        vavi.sound.smaf.Sequence smafSequence = SmafSystem.toSmafSequence(midiSequence, type);

        file = new File(args[1]);
        int r = SmafSystem.write(smafSequence, VaviSmafFileFormat.FILE_TYPE, Files.newOutputStream(file.toPath()));
Debug.println("write: " + r);

        System.exit(0);
    }
}
