/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import vavi.sound.mfi.MfiSystem;
import vavi.util.Debug;


/**
 * MfiContextTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class MfiContextTest {

    static Path dir;

    @BeforeAll
    static void setup() throws Exception {
        dir = Paths.get("tmp");
        if (Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    @Test
    public void test() throws Exception {
        Path inPath = Paths.get(MfiContextTest.class.getResource("/test.mid").toURI());
        javax.sound.midi.Sequence midiSequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(inPath)));
        MidiFileFormat midiFileFormat = MidiSystem.getMidiFileFormat(new BufferedInputStream(Files.newInputStream(inPath)));
        int type = midiFileFormat.getType();
Debug.println("type: " + type);
        vavi.sound.mfi.Sequence mfiSequence = MfiSystem.toMfiSequence(midiSequence, type);
        Path outPath = dir.resolve("MfiContextTest.mld");
        int r = MfiSystem.write(mfiSequence, VaviMfiFileFormat.FILE_TYPE, Files.newOutputStream(outPath));
Debug.println("write: " + r);
    }

    //-------------------------------------------------------------------------

    /**
     * Converts the midi file to a mfi file.
     * <pre>
     * usage:
     *  % java MfiContext in_midi_file out_mld_file
     * </pre>
     */
    public static void main(String[] args) throws Exception {

Debug.println("midi in: " + args[0]);
Debug.println("mfi out: " + args[1]);

        File file = new File(args[0]);
        javax.sound.midi.Sequence midiSequence = MidiSystem.getSequence(file);
        MidiFileFormat midiFileFormat = MidiSystem.getMidiFileFormat(file);
        int type = midiFileFormat.getType();
Debug.println("type: " + type);
        vavi.sound.mfi.Sequence mfiSequence = MfiSystem.toMfiSequence(midiSequence, type);
        file = new File(args[1]);
        int r = MfiSystem.write(mfiSequence, VaviMfiFileFormat.FILE_TYPE, file);
Debug.println("write: " + r);

        System.exit(0);
    }
}

/* */
