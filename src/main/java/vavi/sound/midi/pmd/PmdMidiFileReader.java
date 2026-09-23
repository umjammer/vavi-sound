/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.pmd;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.midi.BasicMidiFileReader;
import vavi.sound.pmd.Pmd;
import vavi.sound.pmd.PmdMidiConverter;
import vavi.sound.pmd.PmdReader;


/**
 * PmdMidiFileReader implemented by vavi.sound.pmd package
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public class PmdMidiFileReader extends BasicMidiFileReader {

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
        if (!Arrays.equals(b, Pmd.TYPE.getBytes())) {
            throw new InvalidMidiDataException("not PMD signature");
        }
        return PmdMidiConverter.toMidiSequence(PmdReader.read(is));
    }
}
