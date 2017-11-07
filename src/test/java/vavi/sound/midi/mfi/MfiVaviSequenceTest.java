/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.File;

import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;

import org.junit.Test;

import vavi.util.StringUtil;

import static org.junit.Assert.*;


/**
 * MfiVaviSequenceTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class MfiVaviSequenceTest {

    @Test
    public void test() {
        fail("Not yet implemented");
    }

    //----

    /** load MFi */
    public static void main(String[] args) throws Exception {
        MidiFileFormat mff = MidiSystem.getMidiFileFormat(new File(args[0]));
System.err.println(StringUtil.paramString(mff));
        System.exit(0);
    }
}

/* */
