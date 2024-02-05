/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Level;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.midi.BasicMidiFileWriter;
import vavi.util.Debug;


/**
 * MfiMidiFileWriter implemented by vavi.
 *
 * @caution MidiSystem は使っちゃだめ！
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class MfiMidiFileWriter extends BasicMidiFileWriter {

    @Override
    public int[] getMidiFileTypes() {
Debug.println(Level.FINE, "(1): " + Arrays.toString(MfiSystem.getMfiFileTypes()));
        return MfiSystem.getMfiFileTypes();
    }

    /** @param sequence MIDI sequence */
    @Override
    public int[] getMidiFileTypes(Sequence sequence) {
Debug.println(Level.FINE, "(2): " + Arrays.toString(MfiSystem.getMfiFileTypes()));
        return MfiSystem.getMfiFileTypes();
    }

    /** @param fileType 0x88:MFi (vavi) をサポートします */
    @Override
    public boolean isFileTypeSupported(int fileType) {
Debug.println(Level.FINE, "(1): fileType: " + fileType);
        return MfiSystem.isFileTypeSupported(fileType);
    }

    /**
     * @param fileType 0x88:MFi (vavi) をサポートします
     * TODO sequence を無視している
     */
    @Override
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
Debug.println(Level.FINE, "(2): fileType: " + fileType);
//Debug.println(sequence);
        return MfiSystem.isFileTypeSupported(fileType);
    }

    /**
     * @param in MIDI Sequence
     * @param fileType {@link #isFileTypeSupported(int)},
     *        {@link #isFileTypeSupported(int, Sequence)} が true のもののみ
     */
    @Override
    public int write(Sequence in, int fileType, OutputStream out)
        throws IOException {
Debug.println(Level.FINE, "in: " + in);
Debug.println(Level.FINE, "fileType: " + fileType);
Debug.println(Level.FINE, "out: " + out);
        try {
            if (isFileTypeSupported(fileType)) {
                vavi.sound.mfi.Sequence mfiSequence = MfiSystem.toMfiSequence(in, fileType);
                return MfiSystem.write(mfiSequence, fileType, out);
            } else {
Debug.println(Level.WARNING, "unknown fileType: " + fileType);
                return 0;
            }
        } catch (InvalidMidiDataException | MfiUnavailableException e) {
            throw new IOException(e);
        }
    }
}
