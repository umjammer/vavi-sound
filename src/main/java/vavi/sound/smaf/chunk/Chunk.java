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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.ByteUtil;
import vavi.util.Debug;
import vavi.util.properties.PrefixedPropertiesFactory;


/**
 * Chunk.
 *
 * TODO make InputStream sub class of FilterInputStream
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public abstract class Chunk {

    /** Chunk ID */
    protected byte[] id = new byte[4];

    /** Chunk size */
    protected int size;

    /** */
    public Chunk() {
    }

    /** TODO bean でもいいかも */
    protected Chunk(byte[] id, int size) {

        this.id = id;
        this.size = size;
    }

    /**
     * @param dis Chunk Header は読み込み済みであること
     * @throws IOException
     * @throws InvalidSmafDataException
     * TODO Chunk -> constructor ???
     * TODO parent を渡したいが為。
     */
    protected abstract void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException;

    /** Chunk ID */
    public String getId() {
        return new String(id);
    }

    /** Chunk Header の 8 バイトは含まれていないので注意 */
    public int getSize() {
        return size;
    }

    /**
     * 最初でない親の読み込み用(マーク無し)
     * @param is
     * @return 読み込んだ Chunk オブジェクト
     */
    protected Chunk readFrom(InputStream is)
        throws InvalidSmafDataException, IOException {

        return readFrom(is, this);
    }

    /**
     * @param is should support marking
     * @param parent 親のデータが欲しい時があるので
     * @return 読み込んだ Chunk オブジェクト
     */
    public static Chunk readFrom(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {

        DataInputStream dis;
        if (is instanceof MyDataInputStream) {
            MyDataInputStream mdis = MyDataInputStream.class.cast(is);
            dis = new DataInputStream(mdis.is);
        } else {
            dis = new DataInputStream(is);
        }

        byte[] id = new byte[4];
        dis.readFully(id); // not want to count down

        int size = dis.readInt();
Debug.printf(Level.FINE, "size: 0x%1$08x (%1$d)", size);

        Chunk chunk = newInstance(id, size);
//Debug.println(chunk.getClass().getName() + "\n" + StringUtil.getDump(is, 0, 128));
//Debug.printf(Level.FINE, "is: " + is + " / " + chunk.getClass().getName());
        MyDataInputStream mdis = new MyDataInputStream(is, id, size);
//Debug.printf(Level.FINE, "mdis: " + mdis + " / " + chunk.getClass().getName());
        chunk.init(mdis, parent);

        if (parent != null) {
            // 親のループ内での読み込みの場合
            if (is instanceof MyDataInputStream) {
                mdis = MyDataInputStream.class.cast(is);
                mdis.readSize -= 8 + chunk.getSize();
            } else {
                assert false : "is: " + is.getClass().getName();
            }
        } else {
//Debug.printf(Level.FINE, "crc (calc): %04x, avail: %d, %s, %s", mdis.crc(), mdis.available(), mdis, chunk.getClass().getName());
            if (chunk instanceof FileChunk) {
                FileChunk fc = FileChunk.class.cast(chunk);
                if (fc.getCrc() != mdis.crc()) {
Debug.printf(Level.WARNING, "crc not match expected: %04x, actural: %04x", fc.getCrc(), mdis.crc());
                }
            }
        }

        return chunk;
    }

    /** */
    public abstract void writeTo(OutputStream os) throws IOException;

    //----

    /** input stream with count down, crc */
    protected static class MyDataInputStream extends InputStream implements DataInput {
        InputStream is;
        DataInputStream dis;
        int readSize;
        static ThreadLocal<CRC16> crc = new ThreadLocal<>();

        protected MyDataInputStream(InputStream is, byte[] id, int size) {
            if (is instanceof MyDataInputStream) {
                MyDataInputStream mdis = MyDataInputStream.class.cast(is);
                this.is = mdis.is;
            } else {
                this.is = is;
            }
//Debug.printf(Level.FINE, "is: " + this.is);
            this.dis = new DataInputStream(this.is);
            this.readSize = size;

            if (crc.get() == null) {
                crc.set(new CRC16());
            }
            crc.get().update(id);
            crc.get().update(ByteUtil.getBeBytes(size));
        }
        public int crc() {
//Debug.println("crc len: " + crc.get().getCount());
            return (int) crc.get().getValue();
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
        static int[] crcTable = new int[UCHAR_MAX + 1];
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
         * 16 ビットの CRC を方法 1 で求めます。
         * @param c データを与えます。
         * @return CRC 値を返します。
         */
        public int update(byte[] c) {
            for (int n = 0; n < c.length; n++) {
                crc = (crc << BYTE_BIT) ^ crcTable[((crc >> (16 - BYTE_BIT)) & 0xff) ^ (c[n] & 0xff)];
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

    //----

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
Debug.println(Level.FINE, e);
            return new UndefinedChunk(id, size); // TODO out source
//          throw new InvalidSmafDataException("unsupported chunk id: " + StringUtil.getDump(id));
        } catch (Exception e) {
if (e instanceof InvocationTargetException) {
Debug.printStackTrace(Level.SEVERE, e.getCause());
} else {
Debug.printStackTrace(Level.SEVERE, e);
}
            throw new IllegalStateException(e);
        }
    }

    /** prefix for property file */
    private static final String keyBase = "chunk.";

    /** constructors for factory */
    private static final PrefixedPropertiesFactory<byte[], Constructor<? extends Chunk>> chunkFactory =
        new PrefixedPropertiesFactory<byte[], Constructor<? extends Chunk>>("/vavi/sound/smaf/smaf.properties", keyBase) {

        @Override
        public Constructor<? extends Chunk> get(byte[] id) {
            String type = new String(id);
Debug.printf(Level.FINE, "Chunk ID(read): %s+0x%02x", (Character.isLetterOrDigit(type.charAt(3)) ? type : new String(id, 0, 3)), (int) type.charAt(3));

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
//Debug.println("chunk class: " + StringUtil.getClassName(clazz));
                return clazz.getConstructor(byte[].class, Integer.TYPE);
            } catch (Exception e) {
Debug.printStackTrace(e);
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected String getStoreKey(String key) {
            return key.substring(keyBase.length());
        }
    };
}

/* */
