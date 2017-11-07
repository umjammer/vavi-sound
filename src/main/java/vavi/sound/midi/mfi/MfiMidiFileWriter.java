/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.midi.BasicMidiFileWriter;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * MfiMidiFileWriter implemented by vavi.
 *
 * @caution MidiSystem は使っちゃだめ！
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class MfiMidiFileWriter extends BasicMidiFileWriter {

    /** */
    public int[] getMidiFileTypes() {
Debug.println("(1): " + StringUtil.expand(MfiSystem.getMfiFileTypes()));
        return MfiSystem.getMfiFileTypes();
    }

    /** @param sequence MIDI sequence */
    public int[] getMidiFileTypes(Sequence sequence) {
Debug.println("(2): " + StringUtil.expand(MfiSystem.getMfiFileTypes()));
        return MfiSystem.getMfiFileTypes();
    }

    /** @param fileType 0x88:MFi (vavi) をサポートします */
    public boolean isFileTypeSupported(int fileType) {
Debug.println("(1): fileType: " + fileType);
        return MfiSystem.isFileTypeSupported(fileType);
    }

    /**
     * @param fileType 0x88:MFi (vavi) をサポートします
     * TODO sequence を無視している
     */
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
Debug.println("(2): fileType: " + fileType);
//Debug.println(sequence);
        return MfiSystem.isFileTypeSupported(fileType);
    }

    /**
     * @param in MIDI Sequence
     * @param fileType {@link #isFileTypeSupported(int)},
     *        {@link #isFileTypeSupported(int, Sequence)} が true のもののみ
     */
    public int write(Sequence in, int fileType, OutputStream out)
        throws IOException {
Debug.println("in: " + in);
Debug.println("fileType: " + fileType);
Debug.println("out: " + out);
        try {
            if (isFileTypeSupported(fileType)) {
                vavi.sound.mfi.Sequence mfiSequence = MfiSystem.toMfiSequence(in, fileType);
                return MfiSystem.write(mfiSequence, fileType, out);
            } else {
Debug.println(Level.WARNING, "unknown fileType: " + fileType);
                return 0;
            }
        } catch (InvalidMidiDataException e) {
            throw (IOException) new IOException().initCause(e);
        } catch (MfiUnavailableException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }
}

/* */
