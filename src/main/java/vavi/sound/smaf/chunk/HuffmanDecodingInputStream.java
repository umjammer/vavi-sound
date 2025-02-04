/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import static java.lang.System.getLogger;


/**
 * HuffmanDecodingInputStream.
 *
 * TODO source
 * TODO not work
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080505 nsano initial version <br>
 */
public class HuffmanDecodingInputStream extends FilterInputStream {

    private static final Logger logger = getLogger(HuffmanDecodingInputStream.class.getName());

    /** */
    private int currentByte;

    /** */
    private int bitOffset = 8;

    /** */
    private final int root;

    /** */
    public HuffmanDecodingInputStream(InputStream is) throws IOException {
        super(is);

        // read tree
        if ((root = readTree(true)) == -1) {
            throw new IllegalStateException("can not initialize reading tree");
        }
logger.log(Level.DEBUG, "root: " + root);
    }

    /**
     * @return -1 error
     */
    private int readBit() throws IOException {

        if (bitOffset == 8) {
            if (in.available() == 0) {
                throw new IllegalStateException("Over size...It may be bug.");
            }
            bitOffset = 0;
            currentByte = in.read();
logger.log(Level.DEBUG, "currentByte: " + currentByte);
        }

        int bit = (currentByte >> (7 - bitOffset)) & 0x01;
logger.log(Level.DEBUG, "bit: " + bit + " (" + bitOffset + ")");
        bitOffset++;

        return bit;
    }

    /**
     * @param n number of bits
     * @return 0
     */
    private int readBits(int n) throws IOException {
        int bits = 0;

        for (int i = 0; i < n; i++) {
            int bit;
            if ((bit = readBit()) == -1) {
                return 0;
            }
            bits = (bits << 1) | bit;
        }

        return bits;
    }

    /** chars */
    private static final int N = 256;

    /** */
    private final int[] left = new int[2 * N - 1];
    /** */
    private final int[] right = new int[2 * N - 1];

    /** */
    private int available; // TODO thread safe?

    /**
     *
     * @return -1
     */
    private int readTree(boolean init) throws IOException {

        if (init) {
            available = N;
        }

        int bit = readBit();
logger.log(Level.DEBUG, "bit: " + bit);
        if (bit == -1) {
            return -1;
        }

        if (bit != 0) { // 1 = fushi
            int i;
            if ((i = available++) >= 2 * N - 1) {
                throw new IllegalStateException("incorrect tree");
            }
            if ((left[i] = readTree(false)) == -1) {
                return -1;
            }
            if ((right[i] = readTree(false)) == -1) {
                return -1;
            }
            return i;
        } else {
            return readBits(8);
        }
    }

    @Override
    public int read() throws IOException {
        byte[] buf = new byte[1];
        int r = read(buf, 0, 1);
        if (r < 0) {
            return -1;
        } else {
            return buf[0] & 0xff;
        }
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        int position = 0;
        while (position < length) {
            int node = root;
logger.log(Level.DEBUG, "node: " + node);
            while (node >= N) {
                int bit = readBit();
                if (bit == -1) {
                    return -1;
                }
                if (bit != 0) {
                    node = right[node];
                } else {
                    node = left[node];
                }
            }

            b[offset + position++] = (byte) node;
        }

        return position; // TODO ???
    }
}
