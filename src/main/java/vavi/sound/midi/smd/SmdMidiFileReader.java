/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smd;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.midi.BasicMidiFileReader;
import vavi.sound.smd.SmdMidiConverter;
import vavi.sound.smd.SmdReader;


/**
 * SmdMidiFileReader implemented by vavi.sound.smd package
 * <p>
 * SMD has no magic, so this accepts data whose first part ({@code ESC $ D} ... {@code SI})
 * appears in the first {@value #PEEK} bytes and is 7 bit text.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public class SmdMidiFileReader extends BasicMidiFileReader {

    /** a title is up to 255 bytes after conversion */
    static final int PEEK = 1024;

    @Override
    public Sequence getSequence(InputStream is) throws InvalidMidiDataException, IOException {
        if (!is.markSupported()) {
            throw new IOException("mark not supported: " + is);
        }
        is.mark(PEEK);
        byte[] b;
        try {
            b = is.readNBytes(PEEK);
        } finally {
            is.reset();
        }
        if (!looksLikeSmd(b)) {
            throw new InvalidMidiDataException("not SMD");
        }
        return SmdMidiConverter.toMidiSequence(SmdReader.read(is));
    }

    /** the first part is 0x20 ~ 0x7f until SI or the end of peeked bytes */
    static boolean looksLikeSmd(byte[] b) {
        if (!SmdReader.isSmd(b)) {
            return false;
        }
        int p = -1;
        for (int i = 0; i + 2 < b.length; i++) {
            if (b[i] == 0x1b && b[i + 1] == '$' && b[i + 2] == 'D') {
                p = i + 3;
                break;
            }
        }
        int n = 0;
        for (; p < b.length && b[p] != 0x0f; p++, n++) {
            if (b[p] < 0x20) { // 0x80 ~ are negative
                return false;
            }
        }
        return n > 0;
    }
}
