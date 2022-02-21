/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Level;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.midi.BasicMidiFileReader;
import vavi.util.Debug;


/**
 * MfiMidiFileReader implemented by vavi.sound.mfi.vavi package
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class MfiMidiFileReader extends BasicMidiFileReader {

    /** Gets a MIDI Sequence converted from MFi */
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
//Debug.println(mfiSequence);
            return MfiSystem.toMidiSequence(mfiSequence);
        } catch (InvalidMfiDataException e) {
Debug.println(Level.FINE, e);
//Debug.printStackTrace(e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        } catch (MfiUnavailableException e) {
Debug.println(Level.SEVERE, e);
            throw new IOException(e);
        } finally {
            try {
                if (!reset) {
                    is.reset();
                }
            } catch (IOException e) {
Debug.println(Level.SEVERE, e);
                throw e;
            }
        }
    }
}

/* */
