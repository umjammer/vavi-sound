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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;

import static java.lang.System.getLogger;


/**
 * SMAF (*.mmf)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 1.00 041223 nsano initial version <br>
 */
public final class SmafSystem {

    private static final Logger logger = getLogger(SmafSystem.class.getName());

    /** TODO spi */
    private static SmafDeviceProvider deviceProvider = new SmafDeviceProvider();
    /** TODO spi */
    private static final SmafFileReader reader = new SmafFileReader();
    /** TODO spi */
    private static final SmafFileWriter writer = new SmafFileWriter();

    /** */
    private SmafSystem() {
    }

    /** Gets a sequencer. */
    public static Sequencer getSequencer() throws SmafUnavailableException {

        return getSequencer(false);
    }

    /** Gets a sequencer. */
    public static Sequencer getSequencer(boolean connected) throws SmafUnavailableException {
        SmafSequencer sequencer = (SmafSequencer) deviceProvider.getDevice(SmafSequencer.info);
        if (connected) {
            try {
                sequencer.open();
                sequencer.getTransmitter().setReceiver(getSynthesizer().getReceiver());
            } catch (MidiUnavailableException e) {
                throw new SmafUnavailableException(e);
            }
        }
        return sequencer;
    }

    /** Gets a listener to attach to a MIDI sequencer. */
    public static Synthesizer getSynthesizer() throws SmafUnavailableException {

        SmafSynthesizer synthesizer = (SmafSynthesizer) deviceProvider.getDevice(SmafSynthesizer.info);
        return synthesizer;
    }

    /**
     * @param is sequencer
     * @return SmafFileFormat
     */
    public static SmafFileFormat getSmafFileFormat(InputStream is) throws InvalidSmafDataException, IOException {

        SmafFileFormat ff = reader.getSmafFileFormat(is);
        return ff;
    }

    /**
     * @param is sequencer
     * @return SMAF Sequence
     */
    public static vavi.sound.smaf.Sequence getSequence(InputStream is) throws InvalidSmafDataException, IOException {

        SmafFileFormat ff = getSmafFileFormat(is);
        return ff.getSequence();
    }

    /** Gets a SMAF sequence. */
    public static vavi.sound.smaf.Sequence getSequence(File file) throws InvalidSmafDataException, IOException {

        return getSequence(new BufferedInputStream(Files.newInputStream(file.toPath())));
    }

    /**
     * @param sequence SMAF Sequence
     * @return MIDI Sequence
     */
    public static Sequence toMidiSequence(vavi.sound.smaf.Sequence sequence) throws InvalidSmafDataException, SmafUnavailableException {

        try {
            SmafMidiConverter converter = (SmafMidiConverter) deviceProvider.getDevice(SmafMidiConverter.info);
            return converter.convert(sequence);
        } catch (IOException | InvalidMidiDataException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new InvalidSmafDataException(e);
        }
    }

    /**
     * @param in MIDI Sequence
     * @param fileType SMAF file type
     * @return SMAF sequence
     */
    public static vavi.sound.smaf.Sequence toSmafSequence(Sequence in, int fileType) throws InvalidMidiDataException, SmafUnavailableException {

        try {
            SmafMidiConverter converter = (SmafMidiConverter) deviceProvider.getDevice(SmafMidiConverter.info);
            return converter.convert(in, fileType);
        } catch (IOException | InvalidSmafDataException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        }
    }

    /**
     * @param smafSequence SMAF Sequence
     * @param fileType SMAF file type
     * @param out stream to output
     * @return SMAF sequence
     */
    public static int write(vavi.sound.smaf.Sequence smafSequence, int fileType, OutputStream out) throws IOException {

        return writer.write(smafSequence, fileType, out);
    }

    /** Writes as SMAF or MIDI. */
    public static int write(vavi.sound.smaf.Sequence smafSequence, int fileType, File out) throws IOException {

        return writer.write(smafSequence, fileType, out);
    }
}
