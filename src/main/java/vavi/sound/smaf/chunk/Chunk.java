/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.ByteUtil;
import vavi.util.properties.PrefixedPropertiesFactory;

import static java.lang.System.getLogger;


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
    public Chunk() {
    }

    /** TODO bean might be fine */
    protected Chunk(byte[] id, int size) {

        this.id = id;
        this.size = size;
    }

    /**
     * @param dis chunk Header must be read
     * @throws IOException when an io error occurs
     * @throws InvalidSmafDataException when input smaf is wrong
     * TODO Chunk -> constructor ???
     *      because of passing the parent
     */
    protected abstract void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException;

    /** Chunk ID */
    public String getId() {
        return new String(id);
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

        DataInputStream dis;
        if (is instanceof MyDataInputStream mdis) {
            dis = new DataInputStream(mdis.is);
        } else {
            dis = new DataInputStream(is);
        }

        byte[] id = new byte[4];
        dis.readFully(id); // not want to count down

        int size = dis.readInt();
logger.log(Level.DEBUG, String.format("size: 0x%1$08x (%1$d)", size));

        Chunk chunk = newInstance(id, size);
//logger.log(Level.TRACE, chunk.getClass().getName() + "\n" + StringUtil.getDump(is, 0, 128));
//logger.log(Level.TRACE, String.format("is: " + is + " / " + chunk.getClass().getName()));
        MyDataInputStream mdis = new MyDataInputStream(is, id, size);
//logger.log(Level.TRACE, String.format("mdis: " + mdis + " / " + chunk.getClass().getName()));
        chunk.init(mdis, parent);

        if (parent != null) {
            // for reading inside the parent loop
            if (is instanceof MyDataInputStream) {
                mdis = (MyDataInputStream) is;
                mdis.readSize -= 8 + chunk.getSize();
            } else {
                assert false : "is: " + is.getClass().getName();
            }
        } else {
//logger.log(Level.TRACE, String.format("crc (calc): %04x, avail: %d, %s, %s", mdis.crc(), mdis.available(), mdis, chunk.getClass().getName()));
            if (chunk instanceof FileChunk fc) {
                if (fc.getCrc() != mdis.crc()) {
logger.log(Level.WARNING, String.format("crc not match expected: %04x, actual: %04x", fc.getCrc(), mdis.crc()));
                }
            }
        }

        return chunk;
    }

    /** */
    public abstract void writeTo(OutputStream os) throws IOException;

    // ----

    /** input stream with count down, crc */
    protected static class MyDataInputStream extends InputStream implements DataInput {
        final InputStream is;
        final DataInputStream dis;
        int readSize;
        static final ThreadLocal<CRC16> crc = new ThreadLocal<>();

        protected MyDataInputStream(InputStream is, byte[] id, int size) {
            if (is instanceof MyDataInputStream mdis) {
                this.is = mdis.is;
            } else {
                this.is = is;
            }
//logger.log(Level.TRACE, String.format("is: " + this.is));
            this.dis = new DataInputStream(this.is);
            this.readSize = size;

            if (crc.get() == null) {
                crc.set(new CRC16());
            }
            crc.get().update(id);
            crc.get().update(ByteUtil.getBeBytes(size));
        }
        public int crc() {
//logger.log(Level.TRACE, "crc len: " + crc.get().getCount());
            return crc.get().getValue();
        }
        @Override
        public long skip(long n) throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public int available() throws IOException {
            return readSize;
        }
        @Override
        public int readUnsignedByte() throws IOException {
            int r = dis.readUnsignedByte();
            crc.get().update((byte) r);
            readSize--;
            return r;
        }
        @Override
        public void readFully(byte[] b) throws IOException {
            dis.readFully(b, 0, b.length);
            crc.get().update(b);
            readSize -= b.length;
        }
        @Override
        public int read() throws IOException {
            return is.read();
        }
        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public int skipBytes(int n) throws IOException {
            byte[] b = new byte[n];
            dis.readFully(b);
            crc.get().update(b);
            readSize -= n;
            return n;
        }
        @Override
        public boolean readBoolean() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public byte readByte() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public short readShort() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public int readUnsignedShort() throws IOException {
            int r = dis.readUnsignedShort();
            if (available() > 2) {
                // crc is located at last of the file
                // and this condition assumed to get crc uses this method.
                crc.get().update(ByteUtil.getBeBytes((short) r));
            }
            readSize -= 2;
            return r;
        }
        @Override
        public char readChar() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public int readInt() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public long readLong() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public float readFloat() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public double readDouble() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public String readLine() throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public String readUTF() throws IOException {
            throw new UnsupportedOperationException();
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
     * @param size
     * @return chunk
     */
    private static Chunk newInstance(byte[] id, int size)
        throws InvalidSmafDataException {

        try {
            return chunkFactory.get(id).newInstance(id, size);
        } catch (IllegalArgumentException e) {
logger.log(Level.DEBUG, e);
            return new UndefinedChunk(id, size); // TODO out source
//          throw new InvalidSmafDataException("unsupported chunk id: " + StringUtil.getDump(id));
        } catch (Exception e) {
if (e instanceof InvocationTargetException) {
logger.log(Level.ERROR, e.getCause().getMessage(), e.getCause());
} else {
logger.log(Level.ERROR, e.getMessage(), e);
}
            throw new IllegalStateException(e);
        }
    }

    /** prefix for property file */
    private static final String keyBase = "chunk.";

    /** constructors for factory */
    private static final PrefixedPropertiesFactory<byte[], Constructor<? extends Chunk>> chunkFactory =
            new PrefixedPropertiesFactory<>("/vavi/sound/smaf/smaf.properties", keyBase) {

                @Override
                public Constructor<? extends Chunk> get(byte[] id) {
                    String type = new String(id);
logger.log(Level.DEBUG, String.format("Chunk ID(read): %s+0x%02x", (Character.isLetterOrDigit(type.charAt(3)) ? type : new String(id, 0, 3)), (int) type.charAt(3)));

                    for (String key : instances.keySet()) {
                        if (key.charAt(3) == '*' && key.substring(0, 3).equals(type.substring(0, 3))) {
                            return instances.get(key);
                        } else if (key.equals(type)) {
                            return instances.get(key);
                        }
                    }

                    throw new IllegalArgumentException(type);
                }

                @Override
                protected Constructor<? extends Chunk> getStoreValue(String value) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends Chunk> clazz = (Class<? extends Chunk>) Class.forName(value);
//logger.log(Level.TRACE, "chunk class: " + StringUtil.getClassName(clazz));
                        return clazz.getConstructor(byte[].class, Integer.TYPE);
                    } catch (Exception e) {
                        logger.log(Level.ERROR, e.getMessage(), e);
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                protected String getStoreKey(String key) {
                    return key.substring(keyBase.length());
                }
            };
}
