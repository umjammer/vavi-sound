/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.midi.BasicMidiFileReader;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafSystem;
import vavi.sound.smaf.SmafUnavailableException;

import static java.lang.System.getLogger;


/**
 * SmafMidiFileReader implemented by vavi.sound.smaf package
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class SmafMidiFileReader extends BasicMidiFileReader {

    private static final Logger logger = getLogger(SmafMidiFileReader.class.getName());

    @Override
    public Sequence getSequence(InputStream is)
        throws InvalidMidiDataException,
               IOException {

        boolean reset = false;

        try {
            if (!is.markSupported()) {
                // TODO should be wrap by BufferedInputStream?
                throw new IOException("mark not supported: " + is);
            }

            is.mark(4); // 4 means SmafSystem#getSequence is able to determine it by 4 bytes

            DataInputStream dis = new DataInputStream(is);
            byte[] b = new byte[4];
            dis.readFully(b);
            if (!Arrays.equals(b, "MMMD".getBytes())) {
                throw new InvalidMidiDataException("not SMAF signature");
            } else {
                is.reset();
                reset = true;
            }

            vavi.sound.smaf.Sequence sequence = SmafSystem.getSequence(is);
//logger.log(Level.DEBUG, sequence);
            return SmafSystem.toMidiSequence(sequence);
        } catch (InvalidSmafDataException e) {
logger.log(Level.INFO, e);
//logger.log(Level.ERROR, e.getMessage(), e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        } catch (SmafUnavailableException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new IOException(e);
        } finally {
            try {
                if (!reset) {
                    is.reset();
                }
            } catch (IOException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            }
        }
    }
}
