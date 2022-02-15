/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import java.io.File;
import java.io.InputStream;

import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

import org.junit.jupiter.api.Test;

import vavi.util.StringUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * SmafVaviSequenceTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class SmafVaviSequenceTest {

    @Test
    public void test1() throws Exception {
        InputStream is = SmafVaviSequenceTest.class.getResourceAsStream("/test.mmf");
        Sequence sequence = new SmafMidiFileReader().getSequence(is);
        assertNotNull(sequence);
    }

    @Test
    public void test2() throws Exception {
        InputStream is = SmafVaviSequenceTest.class.getResourceAsStream("/test.mmf");
        Sequence sequence = MidiSystem.getSequence(is);
        assertNotNull(sequence);
    }

    @Test
    public void test3() throws Exception {
        InputStream is = SmafVaviSequenceTest.class.getResourceAsStream("/test.mmf");
        MidiFileFormat mff = new SmafMidiFileReader().getMidiFileFormat(is);
        assertNotNull(mff);
    }

    @Test
    public void test4() throws Exception {
        InputStream is = SmafVaviSequenceTest.class.getResourceAsStream("/test.mmf");
        MidiFileFormat mff = MidiSystem.getMidiFileFormat(is);
        assertNotNull(mff);
    }

    //----

    /** load smaf */
    public static void main(String[] args) throws Exception {
        MidiFileFormat mff = MidiSystem.getMidiFileFormat(new File(args[0]));
System.err.println(StringUtil.paramString(mff));
        System.exit(0);
    }
}

/* */
