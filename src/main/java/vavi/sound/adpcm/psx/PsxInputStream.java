/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.psx;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteOrder;

import vavi.io.OutputEngineInputStream;

import static java.lang.System.getLogger;


/**
 * PSX (PS-ADPCM) InputStream
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-03 nsano initial version <br>
 */
public class PsxInputStream extends FilterInputStream {

    private static final Logger logger = getLogger(PsxInputStream.class.getName());

    /**
     * @param in headerless PS-ADPCM stream
     * @param channels number of channels
     * @param interleaveBlockSize bytes per channel per interleave block set, 0x10 multiple (mono: 0x10)
     * @param byteOrder byte order of the decoded PCM
     */
    public PsxInputStream(InputStream in, int channels, int interleaveBlockSize, ByteOrder byteOrder)
        throws IOException {

        super(new OutputEngineInputStream(new PsxOutputEngine(in, channels, interleaveBlockSize, byteOrder)));

        int bytesPerSample = 2;
        // each 0x10 byte frame becomes 28 samples, regardless of channel layout
        int numSamples = in.available() / 0x10 * 28;
logger.log(Level.TRACE, "numSamples: " + numSamples);
        this.available = numSamples * bytesPerSample;
logger.log(Level.TRACE, "available: " + available);
    }

    /** */
    private int available;

    @Override
    public int available() throws IOException {
        return available;
    }

    @Override
    public int read() throws IOException {
        available--;
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException("b");
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                 ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException("off: " + off + ", len: " + len);
        } else if (len == 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte) c;

        int i = 1;
        try {
            for (; i < len ; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                b[off + i] = (byte) c;
            }
        } catch (IOException e) {
logger.log(Level.ERROR, e.getMessage(), e);
        }
        return i;
    }
}
