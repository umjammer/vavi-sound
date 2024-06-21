/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.midi.BasicMidiFileWriter;
import vavi.sound.smaf.SmafFileFormat;
import vavi.sound.smaf.SmafSystem;
import vavi.sound.smaf.SmafUnavailableException;

import static java.lang.System.getLogger;


/**
 * SmafMidiFileWriter.
 *
 * @caution don't use MidiSystem in this class!
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class SmafMidiFileWriter extends BasicMidiFileWriter {

    private static final Logger logger = getLogger(SmafMidiFileWriter.class.getName());

    @Override
    public int[] getMidiFileTypes() {
        return new int[] { SmafFileFormat.FILE_TYPE };
    }

    /** @param sequence MIDI sequence */
    @Override
    public int[] getMidiFileTypes(Sequence sequence) {
        return new int[] { SmafFileFormat.FILE_TYPE };
    }

    /** @param fileType supports 0x84:SMAF */
    @Override
    public boolean isFileTypeSupported(int fileType) {
        return fileType == SmafFileFormat.FILE_TYPE;
    }

    /**
     * @param fileType supports 0x84:SMAF
     * TODO ignoring sequence
     */
    @Override
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
        return fileType == SmafFileFormat.FILE_TYPE;
    }

    /**
     * @param in MIDI Sequence
     * @param fileType only #isFileTypeSupported is true
     */
    @Override
    public int write(Sequence in, int fileType, OutputStream out)
        throws IOException {
logger.log(Level.DEBUG, "in: " + in);
logger.log(Level.DEBUG, "fileType: " + fileType);
logger.log(Level.DEBUG, "out: " + out);
        try {
            if (isFileTypeSupported(fileType)) {
                vavi.sound.smaf.Sequence smafSequence = SmafSystem.toSmafSequence(in, fileType);
                return SmafSystem.write(smafSequence, fileType, out);
            } else {
logger.log(Level.WARNING, "unknown fileType: " + fileType);
                return 0;
            }
        } catch (InvalidMidiDataException | SmafUnavailableException e) {
            throw new IOException(e);
        }
    }
}
