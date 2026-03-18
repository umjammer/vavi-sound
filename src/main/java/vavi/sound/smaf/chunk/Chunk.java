/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ServiceLoader;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;
import static vavi.sound.smaf.chunk.Chunk.DumpContext.getDC;


/**
 * Chunk.
 *
 * TODO make InputStream sub class of FilterInputStream
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public abstract class Chunk {

    private static final Logger logger = getLogger(Chunk.class.getName());

    /** Chunk ID */
    protected byte[] id = new byte[4];

    /** Chunk size */
    protected int size;

    /** */
    protected abstract boolean accept(String key);

    /** */
    protected Chunk init(byte[] id, int size) {

        this.id = id;
        this.size = size;

        return this;
    }

    /**
     * @param dis chunk Header must be read
     * @throws IOException when an io error occurs
     * @throws InvalidSmafDataException when input smaf is wrong
     * TODO Chunk -> constructor ???
     *      because of passing the parent
     */
    protected abstract void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException;

    /** Chunk ID */
    public String getId() {
        return new String(id, 0, 3) + (Character.isLetterOrDigit((char) id[3]) ? (char) id[3] : "_");
    }

    /** note that the 8 bytes of Chunk Header are not included. */
    public int getSize() {
        return size;
    }

    /**
     * For reading non-first parent (unmarked)
     * @param is source samf stream
     * @return Chunk object read
     */
    protected Chunk readFrom(InputStream is)
        throws InvalidSmafDataException, IOException {

        return readFrom(is, this);
    }

    /**
     * @param is should support marking
     * @param parent sometimes I want my parents' data.
     * @return Chunk object read
     */
    public static Chunk readFrom(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {

        DataInputStream dis; // not want to count down and crc for now
        if (is instanceof CrcDataInputStream mdis) {
            dis = new DataInputStream(mdis.is);
        } else {
            dis = new DataInputStream(is);
        }

        byte[] id = new byte[4];
        dis.readFully(id); // not want to count down

        int size = dis.readInt();
logger.log(Level.DEBUG, "size: 0x%1$08x (%1$d)".formatted(size));

        Chunk chunk = factory(id, size);
//logger.log(Level.TRACE, chunk.getClass().getName() + "\n" + StringUtil.getDump(is, 0, 128));
//logger.log(Level.TRACE, "is: " + is + " / " + chunk.getClass().getName());
        CrcDataInputStream mdis = new CrcDataInputStream(is, id, size);
//logger.log(Level.TRACE, "mdis: " + mdis + " / " + chunk.getClass().getName());
        chunk.init(mdis, parent);

        if (parent != null) {
            // for reading inside the parent loop
            if (is instanceof CrcDataInputStream) {
                mdis = (CrcDataInputStream) is;
                mdis.readSize -= 8 + chunk.getSize();
            } else {
                assert false : "is: " + is.getClass().getName();
            }
        } else {
//logger.log(Level.TRACE, "crc (calc): %04x, avail: %d, %s, %s".formatted(mdis.crc(), mdis.available(), mdis, chunk.getClass().getName()));
            if (chunk instanceof FileChunk fc) {
                if (fc.getCrc() != mdis.crc()) {
logger.log(Level.WARNING, "crc not match expected: %04x, actual: %04x".formatted(fc.getCrc(), mdis.crc()));
                }
            }
        }

        return chunk;
    }

    /** */
    public abstract void writeTo(OutputStream os) throws IOException;

    // ----

    /** input stream with count down, crc */
    protected static class CrcDataInputStream extends InputStream implements DataInput {
        final InputStream is;
        final DataInputStream dis;
        /** written from outside */
        int readSize;
        static final ThreadLocal<CRC16> crc = new ThreadLocal<>();

        CRC16 getCrc() {
            return crc.get();
        }

        /** {@code id} and {@code size} are used for crc */
        protected CrcDataInputStream(InputStream is, byte[] id, int size) {
            if (is instanceof CrcDataInputStream mdis) {
                this.is = mdis.is;
            } else {
                this.is = is;
            }
//logger.log(Level.TRACE, "is: " + this.is);
            this.dis = new DataInputStream(this.is);
            this.readSize = size;

            if (getCrc() == null) {
                crc.set(new CRC16());
            }
            getCrc().update(id);
            getCrc().update(ByteUtil.getBeBytes(size));
        }
        public int crc() {
//logger.log(Level.TRACE, "crc len: " + getCrc().getCount());
            return getCrc().getValue();
        }
        @Override public long skip(long n) throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override public int available() throws IOException {
            return readSize;
        }
        @Override public int readUnsignedByte() throws IOException {
            int r = dis.readUnsignedByte();
            getCrc().update((byte) r);
            consume(1);
            return r;
        }
        @Override public void readFully(byte[] b) throws IOException {
            dis.readFully(b, 0, b.length);
            getCrc().update(b);
            consume(b.length);
        }
        @Override public int read() throws IOException {
            int r = dis.read();
            if (r == -1) return -1;
            getCrc().update((byte) r);
            consume(1);
            return r;
        }
        @Override public void readFully(byte[] b, int off, int len) throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override public int skipBytes(int n) throws IOException {
            byte[] b = new byte[n];
            dis.readFully(b);
            getCrc().update(b);
            consume(n);
            return n;
        }
        @Override public boolean readBoolean() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override public byte readByte() throws IOException {
            byte b = dis.readByte();
            getCrc().update(b);
            consume(1);
            return b;
        }
        @Override public short readShort() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override public int readUnsignedShort() throws IOException {
            int r = dis.readUnsignedShort();
            if (available() > 2) {
                // crc is located at last of the file
                // and this condition assumed to get crc uses this method.
                getCrc().update(ByteUtil.getBeBytes((short) r));
            }
            consume(2);
            return r;
        }
        @Override public char readChar() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override public int readInt() throws IOException {
            int i = dis.readInt();
            getCrc().update(ByteUtil.getBeBytes(i));
            consume(4);
            return i;
        }
        @Override public long readLong() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override public float readFloat() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override public double readDouble() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override public String readLine() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override public String readUTF() throws IOException {
            throw new UnsupportedOperationException();
        }
        private void consume(int decrement) throws EOFException {
            readSize -= decrement;
            if (readSize < 0)
                throw new EOFException();
        }
    }

    /** CCITT X.25 */
    static class CRC16 {
        /** number of bits in a char */
        static final int BYTE_BIT = 8;
        /** maximum unsigned char value */
        static final int UCHAR_MAX = 0xff;
        static final int[] crcTable = new int[UCHAR_MAX + 1];
        static final int CRCPOLY1 = 0x1021;

        static {
            for (int i = 0; i <= UCHAR_MAX; i++) {
                int r = i << (16 - BYTE_BIT);
                for (int j = 0; j < BYTE_BIT; j++) {
                    if ((r & 0x8000) != 0) {
                        r = (r << 1) ^ CRCPOLY1;
                    } else {
                        r <<= 1;
                    }
                }
                crcTable[i] = r & 0xffff;
            }
        }

        /** */
        int crc = 0xffff;

        int count;

        /**
         * Determine the 16-bit CRC using method 1.
         * @param c data
         * @return CRC value
         */
        public int update(byte[] c) {
            for (byte b : c) {
                crc = (crc << BYTE_BIT) ^ crcTable[((crc >> (16 - BYTE_BIT)) & 0xff) ^ (b & 0xff)];
                count++;
            }
            return ~crc & 0xffff;
        }

        /** */
        public int update(byte c) {
            crc = (crc << BYTE_BIT) ^ crcTable[((crc >> (16 - BYTE_BIT)) & 0xff) ^ (c & 0xff)];
            count++;
            return ~crc & 0xffff;
        }

        /** */
        public int getValue() {
            return ~crc & 0xffff;
        }

        /** */
        public int getCount() {
            return count;
        }
    }

    // ----

    /**
     * factory
     * @param id a chunk id read
     * @param size chunk size
     * @return chunk
     */
    private static Chunk factory(byte[] id, int size) {
        String type = new String(id);
logger.log(Level.DEBUG, "Chunk ID(read): " + (Character.isLetterOrDigit(type.charAt(3)) ? type : "%s+0x%02x".formatted(new String(id, 0, 3), (int) type.charAt(3) & 0xff)));
        for (Chunk chunk : ServiceLoader.load(Chunk.class)) {
            if (chunk.accept(type)) {
                return chunk.init(id, size);
            }
        }

        return new UndefinedChunk().init(id, size); // TODO out source
    }

    // ----

    /** indentation management */
    protected static class DumpContext implements AutoCloseable /* i know this is abuse. */ {
        /** indentation management store */
        private static final ThreadLocal<DumpContext> dc = new ThreadLocal<>();

        static final String indent = " ".repeat(4);
        int depth = 0;
        String indent() { return indent.repeat(depth); }
        DumpContext open() { depth++; return this; }
        @Override public void close() { depth--; }

        /** Gets indentation manager */
        static DumpContext getDC() {
            if (dc.get() == null)
                dc.set(new DumpContext());
            return dc.get();
        }

        /** Gets indented string. */
        String format(String x) { return getDC().indent() + " +--- " + x + "\n"; }
    }

    @Override
    public String toString() {
        return getDC().format(getId());
    }
}
