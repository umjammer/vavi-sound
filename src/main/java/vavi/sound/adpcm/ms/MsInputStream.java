/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ms;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteOrder;

import vavi.io.OutputEngineInputStream;

import static java.lang.System.getLogger;


/**
 * MS InputStream
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030816 nsano initial version <br>
 */
public class MsInputStream extends FilterInputStream {

    private static final Logger logger = getLogger(MsInputStream.class.getName());

    /**
     * byte order little endian
     */
    public MsInputStream(InputStream in,
                         int samplesPerBlock,
                         int channels,
                         int blockSize)
        throws IOException {

        this(in,
             samplesPerBlock,
             Ms._iCoef.length,
             Ms._iCoef,
             channels,
             blockSize,
             ByteOrder.LITTLE_ENDIAN);
    }

    /**
     *
     */
    public MsInputStream(InputStream in,
                         int samplesPerBlock,
                         int nCoefs,
                         int[][] iCoefs,
                         int channels,
                         int blockSize,
                         ByteOrder byteOrder)
        throws IOException {

        super(new OutputEngineInputStream(new MsOutputEngine(in, samplesPerBlock, nCoefs, iCoefs, channels, blockSize, byteOrder)));

        int bytesPerSample = 2;
        int numSamples = Ms.getSamplesIn(in.available(), // TODO
                                         channels,
                                         blockSize,
                                         samplesPerBlock);
logger.log(Level.TRACE, "numSamples: " + numSamples);
        this.available = numSamples * channels * bytesPerSample;
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
                if (b != null) {
                    b[off + i] = (byte) c;
                }
            }
        } catch (IOException e) {
logger.log(Level.ERROR, e.getMessage(), e);
        }
        return i;
    }
}
