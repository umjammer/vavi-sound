/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.midi.BasicMidiFileWriter;

import static java.lang.System.getLogger;


/**
 * MfiMidiFileWriter implemented by vavi.
 *
 * @caution don't use MidiSystem in this class!
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class MfiMidiFileWriter extends BasicMidiFileWriter {

    private static final Logger logger = getLogger(MfiMidiFileWriter.class.getName());

    @Override
    public int[] getMidiFileTypes() {
logger.log(Level.DEBUG, "(1): " + Arrays.toString(MfiSystem.getMfiFileTypes()));
        return MfiSystem.getMfiFileTypes();
    }

    /** @param sequence MIDI sequence */
    @Override
    public int[] getMidiFileTypes(Sequence sequence) {
logger.log(Level.DEBUG, "(2): " + Arrays.toString(MfiSystem.getMfiFileTypes()));
        return MfiSystem.getMfiFileTypes();
    }

    /** @param fileType supports 0x88:MFi (vavi) */
    @Override
    public boolean isFileTypeSupported(int fileType) {
logger.log(Level.DEBUG, "(1): fileType: " + fileType);
        return MfiSystem.isFileTypeSupported(fileType);
    }

    /**
     * @param fileType supports 0x88:MFi (vavi)
     * TODO ignoring sequence
     */
    @Override
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
logger.log(Level.DEBUG, "(2): fileType: " + fileType);
//logger.log(Level.TRACE, sequence);
        return MfiSystem.isFileTypeSupported(fileType);
    }

    /**
     * @param in MIDI Sequence
     * @param fileType only {@link #isFileTypeSupported(int)},
     *        {@link #isFileTypeSupported(int, Sequence)} are true
     */
    @Override
    public int write(Sequence in, int fileType, OutputStream out)
        throws IOException {
logger.log(Level.DEBUG, "in: " + in);
logger.log(Level.DEBUG, "fileType: " + fileType);
logger.log(Level.DEBUG, "out: " + out);
        try {
            if (isFileTypeSupported(fileType)) {
                vavi.sound.mfi.Sequence mfiSequence = MfiSystem.toMfiSequence(in, fileType);
                return MfiSystem.write(mfiSequence, fileType, out);
            } else {
logger.log(Level.WARNING, "unknown fileType: " + fileType);
                return 0;
            }
        } catch (InvalidMidiDataException | MfiUnavailableException e) {
            throw new IOException(e);
        }
    }
}
