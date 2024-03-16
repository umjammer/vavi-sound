/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ima;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import vavi.io.OutputEngineInputStream;
import vavi.util.Debug;


/**
 * IMA InputStream
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030816 nsano initial version <br>
 */
public class ImaInputStream extends FilterInputStream {

    /**
     * byte order is little endian
     */
    public ImaInputStream(InputStream in,
                          int samplesPerBlock,
                          int channels,
                          int blockSize)
        throws IOException {

        this(in,
             samplesPerBlock,
             channels,
             blockSize,
             ByteOrder.BIG_ENDIAN);
    }

    /**
     *
     * @param in
     * @param samplesPerBlock
     * @param channels
     * @param blockSize
     * @param byteOrder
     */
    public ImaInputStream(InputStream in,
                          int samplesPerBlock,
                          int channels,
                          int blockSize,
                          ByteOrder byteOrder)
        throws IOException {

        super(new OutputEngineInputStream(new ImaOutputEngine(in, samplesPerBlock, channels, blockSize, byteOrder)));

        int bytesPerSample = 2;
        int numSamples = Ima.getSamplesIn(in.available(), // TODO
                                          channels,
                                          blockSize,
                                          samplesPerBlock);
        this.available = numSamples * channels * bytesPerSample;
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
            Debug.printStackTrace(e);
        }
        return i;
    }
}
