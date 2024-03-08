/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.spi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import vavi.sound.mfi.Sequence;


/**
 * MfiFileWriter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020629 nsano initial version <br>
 *          0.01 020704 nsano midi compliant <br>
 *          0.02 030817 nsano add isFileTypeSupported <br>
 * @see javax.sound.midi.spi.MidiFileWriter
 */
public abstract class MfiFileWriter {

    /** @see javax.sound.midi.spi.MidiFileWriter#getMidiFileTypes()  */
    public abstract int[] getMfiFileTypes();

    /** @see javax.sound.midi.spi.MidiFileWriter#getMidiFileTypes(javax.sound.midi.Sequence)  */
    public abstract int[] getMfiFileTypes(Sequence sequence);

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
