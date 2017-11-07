/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.properties.PrefixedPropertiesFactory;


/**
 * Chunk.
 *
 * TODO make InputStream sub class of FilterInputStream
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public abstract class Chunk {

    /** Chunk ID */
    protected byte[] id = new byte[4];

    /** Chunk size */
    protected int size;

    /**
     * 親でカウントダウンしないで済むように。
     * (結局ややこしいだけちゃうん？)
     * @see #Chunk(byte[], int)
     * @see #available()
     * @see #read(InputStream)
     * @see #skip(InputStream,long)
     * @see #read(InputStream,byte[])
     */
    private int readSize;

    /** */
    public Chunk() {
    }

    /** TODO bean でもいいかも */
    protected Chunk(byte[] id, int size) {

        this.id = id;
        this.size = size;

        this.readSize = size;
    }

    /**
     * @param is Chunk Header は読み込み済みであること
     * @throws IOException
     * @throws InvalidSmafDataException
     * TODO Chunk -> constructor ???
     * TODO parent を渡したいが為。
     */
    protected abstract void init(InputStream is, Chunk parent)
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
     * カウントダウンした結果
     * @see #readSize
     */
    protected int available() {
        return readSize;
    }

    /**
     * 勝手にカウントダウンしない InputStream から直接読み込んだ場合の補正用
     * @see #readSize
     */
    private void consume(int size) {
        readSize -= size;
    }

    /**
     * EOF チェック付きのユーティリティ
     * @see #readSize カウントダウンされます
     */
    protected void skip(InputStream is, long bytes) throws IOException {
        skipInternal(is, bytes);
        readSize -= bytes;
    }

    /**
     * EOF チェック付きのユーティリティ
     */
    private static void skipInternal(InputStream is, long bytes) throws IOException {
        long l = 0;
        while (bytes - l > 0) {
            long r = is.skip(bytes - l);
            if (r < 0) {
                throw new EOFException();
            }
            l += r;
        }
    }

    /**
     * EOF チェック付きのユーティリティ
     * @see #readSize カウントダウンされます
     * @return unsigned byte
     */
    protected int read(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        consume(1);
        return dis.readUnsignedByte();
    }

    /**
     * EOF チェック付きのユーティリティ
     * @see #readSize カウントダウンされます
     * @return unsigned short
     */
    protected int readShort(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        consume(2);
        return dis.readUnsignedShort();
    }

    /**
     * EOF チェック付きのユーティリティ
     * buffer.length サイズ読み込まれます。
     * @see #readSize カウントダウンされます
     */
    protected void read(InputStream is, byte[] buffer) throws IOException {
        readInternal(is, buffer);
        consume(buffer.length);
    }

    /**
     * EOF チェック付きのユーティリティ
     */
    private static void readInternal(InputStream is, byte[] buffer) throws IOException {
        int l = 0;
        while (buffer.length - l > 0) {
            int r = is.read(buffer, l, buffer.length - l);
            if (r < 0) {
                throw new EOFException();
            }
            l += r;
        }
    }

    /**
     * 最初でない親の読み込み用(マーク無し)
     * @param is
     * @return 読み込んだ Chunk オブジェクト
     */
    protected Chunk readFrom(InputStream is)
        throws InvalidSmafDataException, IOException {

        return readFrom(is, this, false);
    }

    /**
     * @param is should support marking
     * @param parent 親のデータが欲しい時があるので
     * @param mark Chunk Header を読み込んだあと破棄するかどうか(最初だけ使う)
     * @return 読み込んだ Chunk オブジェクト
     * @see #readSize parent != null ならカウントダウンされます
     * @throws IOException when <i>is</i> does not support marking
     */
    public static Chunk readFrom(InputStream is, Chunk parent, boolean mark)
        throws InvalidSmafDataException, IOException {

        if (mark && !is.markSupported()) {
            throw new IOException("cannot mark to stream");
        }

        if (mark) {
            is.mark(8);
        }

        byte[] id = new byte[4];
        readInternal(is, id);

        DataInputStream dis = new DataInputStream(is);
        int size = dis.readInt();
Debug.println("size: " + StringUtil.toHex8(size) + "(" + size + ")");

        if (mark) {
            is.reset();
            is.mark(8 + size);
            skipInternal(is, 8);
        }

        Chunk chunk = newInstance(id, size);
        chunk.init(is, parent);

        if (mark) {
            is.reset();

            is.mark(8 + size);
            int crcLength = size + 8 - 2;
            byte[] buffer = new byte[crcLength];
            readInternal(is, buffer);

            CRC16 crc = new CRC16();
            crc.update(buffer);
Debug.println("crc (calc): " + StringUtil.toHex4((int) ~crc.getValue()));

            is.reset();
        }

        if (parent != null) {
            // 親のループ内での読み込みの場合
            parent.consume(8 + chunk.getSize());
        }

        return chunk;
    }

    /** */
    public abstract void writeTo(OutputStream os) throws IOException;

    //----

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

        /**
         * 16 ビットの CRC を方法 1 で求めます。
         * @param c データを与えます。
         * @return CRC 値を返します。
         */
        public int update(byte[] c) {
            for (int n = 0; n < c.length; n++) {
                crc = (crc << BYTE_BIT) ^ crcTable[((crc >> (16 - BYTE_BIT)) & 0xff) ^ (c[n] & 0xff)];
            }
            return ~crc & 0xffff;
        }

        /** */
        public int update(byte c) {
            crc = (crc << BYTE_BIT) ^ crcTable[((crc >> (16 - BYTE_BIT)) & 0xff) ^ (c & 0xff)];
            return ~crc & 0xffff;
        }

        /** */
        public long getValue() {
            return crc;
        }
    }

    //----

    /**
     * read 1 ~ 2 bytes
     * @return 0〜127, 128〜16511 (0x407f)
     */
    protected int readOneToTwo(InputStream is)
        throws IOException {

        int value;
        int d1 = read(is);
//Debug.println("d1: " + d1 + "(" + StringUtil.toHex2(d1) + ")");
        if ((0x80 & d1) != 0) {         // ---- 0x80 ~ 0xff,  ----
            int d2 = read(is);
//Debug.println("d2: " + d2 + "(" + StringUtil.toHex2(d2) + ")");
            value = (((d1 & 0x7f) + 1) << 7) | (d2 & 0x7f);
        } else {                        // ---- 0x01 ~ 0x7f, ----
            value = d1 & 0x7f;
        }
//Debug.println("value: " + value);
        return value;
    }

    /**
     * read 1 ~ 4 bytes
     * @return 0 ~ 268435455 (0x0fffffff)
     */
    protected int readOneToFour(InputStream is)
        throws IOException {

        int value;
        int d1 = read(is);
        if ((0x80 & d1) != 0) {
            int d2 = read(is);
            if ((0x80 & d2) != 0) {
                int d3 = read(is);
                if ((0x80 & d3) != 0) {
                    int d4 = read(is);
                    value = ((d1 & 0x7f) << 21) | ((d2 & 0x7f) << 14) | ((d3 & 0x7f) << 7) | (d4 & 0x7f);
//Debug.println("1-4(4): " + value);
                } else {
                    value = ((d1 & 0x7f) << 14) | ((d2 & 0x7f) << 7) | (d3 & 0x7f);
//Debug.println("1-4(3): " + value);
                }
            } else {
                value = ((d1 & 0x7f) << 7) | (d2 & 0x7f); // 128 ~
//Debug.println("1-4(2): " + value);
            }
        } else {
            value = d1 & 0x7f; // 0 ~ 127
//Debug.println("1-4(1): " + value);
        }
        return value;
    }

    //----

    /**
     * @param id a chunk id read
     * @param size
     * @return chunk
     */
    private static Chunk newInstance(byte[] id, int size)
        throws InvalidSmafDataException {

        try {
            return chunkFactory.get(id).newInstance(id, size);
        } catch (IllegalArgumentException e) {
            return new UndefinedChunk(id, size); // TODO out source
//          throw new InvalidSmafDataException("unsupported chunk id: " + StringUtil.getDump(id));
        } catch (Exception e) {
if (e instanceof InvocationTargetException) {
Debug.printStackTrace(e.getCause());
} else {
Debug.printStackTrace(e);
}
            throw new IllegalStateException(e);
        }
    }

    /** */
    private static final String keyBase = "chunk.";

    /** */
    private static final PrefixedPropertiesFactory<byte[], Constructor<? extends Chunk>> chunkFactory =
        new PrefixedPropertiesFactory<byte[], Constructor<? extends Chunk>>("/vavi/sound/smaf/smaf.properties", keyBase) {

        @Override
        public Constructor<? extends Chunk> get(byte[] id) {
            String type = new String(id);
Debug.println("Chunk ID(read): " + (Character.isLetterOrDigit(type.charAt(3)) ? type : new String(id, 0, 3) + "+0x" + StringUtil.toHex2(type.charAt(3))));

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
