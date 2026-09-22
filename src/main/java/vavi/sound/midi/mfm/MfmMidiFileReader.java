/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.midi.BasicMidiFileReader;
import vavi.sound.mfm.Mfm;
import vavi.sound.mfm.MfmMidiConverter;
import vavi.sound.mfm.MfmReader;


/**
 * MfmMidiFileReader implemented by vavi.sound.mfm package
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public class MfmMidiFileReader extends BasicMidiFileReader {

    @Override
    public Sequence getSequence(InputStream is) throws InvalidMidiDataException, IOException {
        if (!is.markSupported()) {
            throw new IOException("mark not supported: " + is);
        }
        is.mark(4);
        byte[] b;
        try {
            b = is.readNBytes(4);
        } finally {
            is.reset();
        }
        if (!Arrays.equals(b, Mfm.TYPE.getBytes())) {
            throw new InvalidMidiDataException("not MFM signature");
        }
        return MfmMidiConverter.toMidiSequence(MfmReader.read(is));
    }
}
