/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.Sequence;

import vavi.util.Debug;


/**
 * SMAF (*.mmf)
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 1.00 041223 nsano initial version <br>
 */
public final class SmafSystem {

    /** TODO SmafDeviceProvider */
    private static Sequencer sequencer;
    /** TODO SmafDeviceProvider */
    private static SmafMidiConverter converter;
    /** TODO SmafDeviceProvider */
    private static MetaEventListener waveSequencer;
    /** */
    private static SmafFileReader reader;
    /** */
    private static SmafFileWriter writer;

    /** */
    static {
        // TODO SmafDeviceProvider
        sequencer = new SmafSequencer();
        converter = new SmafMidiConverter();
        waveSequencer = new MetaEventAdapter();

        reader = new SmafFileReader();
        writer = new SmafFileWriter();
    }

    /** */
    private SmafSystem() {
    }

    /** シーケンサを取得します。 */
    public static Sequencer getSequencer()
        throws SmafUnavailableException {

        return sequencer;
    }

    /** MIDI シーケンサに付加するリスナを取得します。 */
    public static MetaEventListener getMetaEventListener()
        throws SmafUnavailableException {

        return waveSequencer;
    }

    /**
     * @param is
     * @return SmafFileFormat
     */
    public static SmafFileFormat getSmafFileFormat(InputStream is)
        throws InvalidSmafDataException, IOException {

        SmafFileFormat ff = reader.getSmafFileFormat(is);
        return ff;
    }

    /**
     * @param is
     * @return SMAF Sequence
     */
    public static vavi.sound.smaf.Sequence getSequence(InputStream is)
        throws InvalidSmafDataException, IOException {

        SmafFileFormat ff = getSmafFileFormat(is);
        return ff.getSequence();
    }

    /** SMAF シーケンスを取得します。 */
    public static vavi.sound.smaf.Sequence getSequence(File file)
        throws InvalidSmafDataException, IOException {

        return getSequence(new BufferedInputStream(new FileInputStream(file)));
    }

    /**
     * @param sequence SMAF Sequence
     * @return MIDI Sequence
     */
    public static Sequence toMidiSequence(vavi.sound.smaf.Sequence sequence)
        throws InvalidSmafDataException, SmafUnavailableException {

        try {
            return converter.convert(sequence);
        } catch (IOException e) {
Debug.printStackTrace(e);
            throw (InvalidSmafDataException) new InvalidSmafDataException().initCause(e);
        } catch (InvalidMidiDataException e) {
Debug.printStackTrace(e);
            throw (InvalidSmafDataException) new InvalidSmafDataException().initCause(e);
        }
    }

    /**
     * @param in MIDI Sequence
     * @param fileType
     * @return SMAF sequence
     */
    public static vavi.sound.smaf.Sequence toSmafSequence(Sequence in, int fileType)
        throws InvalidMidiDataException, SmafUnavailableException {

        try {
            return converter.convert(in, fileType);
        } catch (IOException e) {
Debug.printStackTrace(e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        } catch (InvalidSmafDataException e) {
Debug.printStackTrace(e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        }
    }

    /**
     * @param smafSequence
     * @param fileType
     * @param out
     * @return SMAF sequence
     */
    public static int write(vavi.sound.smaf.Sequence smafSequence, int fileType, OutputStream out)
        throws IOException {

        return writer.write(smafSequence, fileType, out);
    }

    /** SMAF or MIDI で書き出します。 */
    public static int write(vavi.sound.smaf.Sequence smafSequence, int fileType, File out)
        throws IOException {

        return writer.write(smafSequence, fileType, out);
    }
}

/* */

