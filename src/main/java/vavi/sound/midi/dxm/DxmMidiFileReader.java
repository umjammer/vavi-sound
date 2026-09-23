/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.dxm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.dxm.Dxm;
import vavi.sound.dxm.DxmMidiConverter;
import vavi.sound.dxm.DxmReader;
import vavi.sound.midi.BasicMidiFileReader;


/**
 * DxmMidiFileReader implemented by vavi.sound.dxm package
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public class DxmMidiFileReader extends BasicMidiFileReader {

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
        if (!Arrays.equals(b, Dxm.TYPE.getBytes())) {
            throw new InvalidMidiDataException("not DXM signature");
        }
        return DxmMidiConverter.toMidiSequence(DxmReader.read(is));
    }
}
