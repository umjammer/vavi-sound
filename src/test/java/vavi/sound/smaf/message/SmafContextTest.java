/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.io.File;
import java.io.FileOutputStream;

import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;

import org.junit.Test;

import vavi.sound.smaf.SmafSystem;
import vavi.util.Debug;

import static org.junit.Assert.*;


/**
 * SmafContextTest. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class SmafContextTest {

    @Test
    public void test() {
        fail("Not yet implemented");
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
        int r = SmafSystem.write(smafSequence, 0, new FileOutputStream(file));
Debug.println("write: " + r);

        System.exit(0);
    }
}

/* */
