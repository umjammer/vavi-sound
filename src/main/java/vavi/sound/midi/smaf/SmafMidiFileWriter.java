/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import java.io.IOException;
import java.io.OutputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.midi.BasicMidiFileWriter;
import vavi.sound.smaf.SmafFileFormat;
import vavi.sound.smaf.SmafSystem;
import vavi.sound.smaf.SmafUnavailableException;
import vavi.util.Debug;


/**
 * SmafMidiFileWriter.
 * 
 * @caution MidiSystem �͎g�����Ⴞ�߁I
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class SmafMidiFileWriter extends BasicMidiFileWriter {

    /** */
    public int[] getMidiFileTypes() {
        return new int[] { SmafFileFormat.FILE_TYPE };
    }

    /** @param sequence MIDI sequence */
    public int[] getMidiFileTypes(Sequence sequence) {
        return new int[] { SmafFileFormat.FILE_TYPE };
    }

    /** @param fileType 0x84:SMAF ���T�|�[�g���܂� */
    public boolean isFileTypeSupported(int fileType) {
        return fileType == SmafFileFormat.FILE_TYPE;
    }

    /**
     * @param fileType 0x84:SMAF ���T�|�[�g���܂�
     * TODO sequence �𖳎����Ă���
     */
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
        return fileType == SmafFileFormat.FILE_TYPE;
    }

    /**
     * @param in MIDI Sequence
     * @param fileType #isFileTypeSupported �� true �̂��̂̂�
     */
    public int write(Sequence in, int fileType, OutputStream out)
        throws IOException {
Debug.println("in: " + in);
Debug.println("fileType: " + fileType);
Debug.println("out: " + out);
        try {
            if (isFileTypeSupported(fileType)) {
                vavi.sound.smaf.Sequence smafSequence = SmafSystem.toSmafSequence(in, fileType);
                return SmafSystem.write(smafSequence, fileType, out);
            } else {
Debug.println("unknown fileType: " + fileType);
                return 0;
            }
        } catch (InvalidMidiDataException e) {
            throw (IOException) new IOException().initCause(e);
        } catch (SmafUnavailableException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }
}

/* */
