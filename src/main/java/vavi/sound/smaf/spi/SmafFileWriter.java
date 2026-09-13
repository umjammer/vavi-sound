/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.spi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import vavi.sound.smaf.Sequence;


/**
 * MfiFileWriter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260912 nsano initial version <br>
 * @see javax.sound.midi.spi.MidiFileWriter
 */
public abstract class SmafFileWriter {

    /** @see javax.sound.midi.spi.MidiFileWriter#getMidiFileTypes()  */
    public abstract int[] getSmafFileTypes();

    /** @see javax.sound.midi.spi.MidiFileWriter#getMidiFileTypes(javax.sound.midi.Sequence)  */
    public abstract int[] getSmafFileTypes(Sequence sequence);

    /** @see javax.sound.midi.spi.MidiFileWriter#isFileTypeSupported(int)  */
    public boolean isFileTypeSupported(int fileType) {
        return false;
    }

    /** @see javax.sound.midi.spi.MidiFileWriter#isFileTypeSupported(int, javax.sound.midi.Sequence)  */
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
        return false;
    }

    /** @see javax.sound.midi.spi.MidiFileWriter#write(javax.sound.midi.Sequence, int, OutputStream)  */
    public abstract int write(Sequence in, int fileType, OutputStream out) throws IOException;

    /** @see javax.sound.midi.spi.MidiFileWriter#write(javax.sound.midi.Sequence, int, File) */
    public abstract int write(Sequence in, int fileType, File out) throws IOException;
}
