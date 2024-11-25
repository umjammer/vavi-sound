/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.midi.BasicMidiFileReader;

import static java.lang.System.getLogger;


/**
 * MfiMidiFileReader implemented by vavi.sound.mfi.vavi package
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class MfiMidiFileReader extends BasicMidiFileReader {

    private static final Logger logger = getLogger(MfiMidiFileReader.class.getName());

    /** Gets a MIDI Sequence converted from MFi */
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

            is.mark(4); // 4 means MfiSystem#getSequence is able to determine it 4 bytes

            DataInputStream dis = new DataInputStream(is);
            byte[] b = new byte[4];
            dis.readFully(b);
            if (!Arrays.equals(b, "melo".getBytes())) {
                throw new InvalidMidiDataException("not MFi signature");
            } else {
                is.reset();
                reset = true;
            }

            vavi.sound.mfi.Sequence mfiSequence = MfiSystem.getSequence(is);
//logger.log(Level.TRACE, mfiSequence);
            return MfiSystem.toMidiSequence(mfiSequence);
        } catch (InvalidMfiDataException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        } catch (MfiUnavailableException e) {
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
