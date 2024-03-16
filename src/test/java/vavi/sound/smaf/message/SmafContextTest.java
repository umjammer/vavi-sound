/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vavi.sound.smaf.SmafSystem;
import vavi.util.Debug;


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
        if (Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    @Test
    @Disabled("not implemented yet")
    public void test() throws Exception {
        Path inPath = Paths.get(SmafContextTest.class.getResource("/test.mid").toURI());
        javax.sound.midi.Sequence midiSequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(inPath)));
        MidiFileFormat midiFileFormat = MidiSystem.getMidiFileFormat(new BufferedInputStream(Files.newInputStream(inPath)));
        int type = midiFileFormat.getType();
Debug.println(Level.FINE, "type: " + type);
        vavi.sound.smaf.Sequence smafSequence = SmafSystem.toSmafSequence(midiSequence, type);

        Path outPath = dir.resolve("SmafContextTest.mmf");
        int r = SmafSystem.write(smafSequence, 0, Files.newOutputStream(outPath));
Debug.println(Level.FINE, "write: " + r);
    }

    //-------------------------------------------------------------------------

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
        int r = SmafSystem.write(smafSequence, 0, Files.newOutputStream(file.toPath()));
Debug.println("write: " + r);

        System.exit(0);
    }
}
