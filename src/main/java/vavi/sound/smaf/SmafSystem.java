/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.Sequence;

import vavi.util.Debug;


/**
 * SMAF (*.mmf)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
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

    /* */
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

    /** Gets a sequencer. */
    public static Sequencer getSequencer()
        throws SmafUnavailableException {

        return sequencer;
    }

    /** Gets a listener to attach to a MIDI sequencer. */
    public static MetaEventListener getMetaEventListener()
        throws SmafUnavailableException {

        return waveSequencer;
    }

    /**
     * @param is sequencer
     * @return SmafFileFormat
     */
    public static SmafFileFormat getSmafFileFormat(InputStream is)
        throws InvalidSmafDataException, IOException {

        SmafFileFormat ff = reader.getSmafFileFormat(is);
        return ff;
    }

    /**
     * @param is sequencer
     * @return SMAF Sequence
     */
    public static vavi.sound.smaf.Sequence getSequence(InputStream is)
        throws InvalidSmafDataException, IOException {

        SmafFileFormat ff = getSmafFileFormat(is);
        return ff.getSequence();
    }

    /** Gets a SMAF sequence. */
    public static vavi.sound.smaf.Sequence getSequence(File file)
        throws InvalidSmafDataException, IOException {

        return getSequence(new BufferedInputStream(Files.newInputStream(file.toPath())));
    }

    /**
     * @param sequence SMAF Sequence
     * @return MIDI Sequence
     */
    public static Sequence toMidiSequence(vavi.sound.smaf.Sequence sequence)
        throws InvalidSmafDataException, SmafUnavailableException {

        try {
            return converter.convert(sequence);
        } catch (IOException | InvalidMidiDataException e) {
Debug.printStackTrace(e);
            throw new InvalidSmafDataException(e);
        }
    }

    /**
     * @param in MIDI Sequence
     * @param fileType SMAF file type
     * @return SMAF sequence
     */
    public static vavi.sound.smaf.Sequence toSmafSequence(Sequence in, int fileType)
        throws InvalidMidiDataException, SmafUnavailableException {

        try {
            return converter.convert(in, fileType);
        } catch (IOException | InvalidSmafDataException e) {
Debug.printStackTrace(e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        }
    }

    /**
     * @param smafSequence SMAF Sequence
     * @param fileType SMAF file type
     * @param out stream to output
     * @return SMAF sequence
     */
    public static int write(vavi.sound.smaf.Sequence smafSequence, int fileType, OutputStream out)
        throws IOException {

        return writer.write(smafSequence, fileType, out);
    }

    /** Writes as SMAF or MIDI. */
    public static int write(vavi.sound.smaf.Sequence smafSequence, int fileType, File out)
        throws IOException {

        return writer.write(smafSequence, fileType, out);
    }
}
