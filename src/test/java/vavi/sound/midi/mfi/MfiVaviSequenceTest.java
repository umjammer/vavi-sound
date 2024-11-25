/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.File;
import java.io.InputStream;

import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

import org.junit.jupiter.api.Test;

import vavi.util.StringUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * MfiVaviSequenceTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class MfiVaviSequenceTest {

    @Test
    public void test0() throws Exception {
        InputStream is = MfiVaviSequenceTest.class.getResourceAsStream("/test.mld");
        Sequence sequence = new MfiMidiFileReader().getSequence(is);
        assertNotNull(sequence);
    }

    @Test
    public void test2() throws Exception {
        InputStream is = MfiVaviSequenceTest.class.getResourceAsStream("/test.mld");
        Sequence sequence = MidiSystem.getSequence(is);
        assertNotNull(sequence);
    }

    @Test
    public void test3() throws Exception {
        InputStream is = MfiVaviSequenceTest.class.getResourceAsStream("/test.mld");
        MidiFileFormat format = new MfiMidiFileReader().getMidiFileFormat(is);
        assertNotNull(format);
    }

    @Test
    public void test4() throws Exception {
        InputStream is = MfiVaviSequenceTest.class.getResourceAsStream("/test.mld");
        MidiFileFormat format = MidiSystem.getMidiFileFormat(is);
        assertNotNull(format);
    }

    // ----

    /** load MFi */
    public static void main(String[] args) throws Exception {
        MidiFileFormat mff = MidiSystem.getMidiFileFormat(new File(args[0]));
System.err.println(StringUtil.paramString(mff));
    }
}
