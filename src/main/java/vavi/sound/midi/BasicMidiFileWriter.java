/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.sound.midi.Sequence;
import javax.sound.midi.spi.MidiFileWriter;


/**
 * BasicMidiFileWriter.
 * <p>
 * !!!caution don't use {@link javax.sound.midi.MidiSystem} in this class.
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041223 nsano initial version <br>
 */
public abstract class BasicMidiFileWriter extends MidiFileWriter {

    @Override
    public final int write(Sequence in, int fileType, File out)
        throws IOException {

        return write(in, fileType, new FileOutputStream(out));
    }
}

/* */
