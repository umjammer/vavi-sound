/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import java.io.File;

import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavi.util.StringUtil;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * SmafVaviSequenceTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@Disabled
public class SmafVaviSequenceTest {

    @Test
    public void test() {
        fail("Not yet implemented");
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
