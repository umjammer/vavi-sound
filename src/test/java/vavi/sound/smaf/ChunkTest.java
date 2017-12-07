/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import vavi.sound.smaf.chunk.Chunk;

import static org.junit.Assert.assertEquals;


/**
 * ChunkTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050508 nsano initial version <br>
 */
public class ChunkTest {

    /** for testing protected methods */
    class HackedChunk extends Chunk {

        @Override
        protected void init(InputStream is, Chunk parent) throws InvalidSmafDataException, IOException {
        }

        public int readOneToFour(InputStream is) throws IOException {
            return super.readOneToFour(is);
        }

        public int readOneToTwo(InputStream is) throws IOException {
            return super.readOneToTwo(is);
        }

        @Override
        public void writeTo(OutputStream os) throws IOException {
        }
    }

    /** for testing protected methods */
    class HackedMessage extends SmafMessage {

        /* */
        public void writeOneToFour(OutputStream os, int value) throws IOException {
            super.writeOneToFour(os, value);
        }

        /* */
        @Override
        public void writeOneToTwo(OutputStream os, int value) throws IOException {
            super.writeOneToTwo(os, value);
        }

        @Override
        public int getLength() {
            return 0;
        }

        @Override
        public byte[] getMessage() {
            return null;
        }
    }

    /**
     * Tests {@link HackedChunk#readOneToFour(InputStream)}.
     * <pre>
     *  実際の数値 | 可変長での表現
     * ------------+-------------
     *  00000000   | 00
     *  00000040   | 40
     *  0000007F   | 7F
     *  00000080   | 81 00
     *  00002000   | C0 00
     *  00003FFF   | FF 7F
     *  00004000   | 81 80 00
     *  00100000   | C0 80 00
     *  001FFFFF   | FF FF 7F
     *  00200000   | 81 80 80 00
     *  08000000   | C0 80 80 00
     *  0FFFFFFF   | FF FF FF 7F
     * </pre>
     */
    @Test
    public void testReadOneToFour() throws Exception {
        HackedChunk chunk = new HackedChunk();
        assertEquals(0x00000000, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { 0x00 })));
        assertEquals(0x00000040, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { 0x40 })));
        assertEquals(0x0000007F, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { 0x7F })));
        assertEquals(0x00000080, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { (byte) 0x81, 0x00 })));
        assertEquals(0x00002000, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { (byte) 0xC0, 0x00 })));
        assertEquals(0x00003FFF, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { (byte) 0xFF, 0x7F })));
        assertEquals(0x00004000, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { (byte) 0x81, (byte) 0x80, 0x00 })));
        assertEquals(0x00100000, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { (byte) 0xC0, (byte) 0x80, 0x00 })));
        assertEquals(0x001FFFFF, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { (byte) 0xFF, (byte) 0xFF, 0x7F })));
        assertEquals(0x00200000, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { (byte) 0x81, (byte) 0x80, (byte) 0x80, 0x00 })));
        assertEquals(0x08000000, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { (byte) 0xC0, (byte) 0x80, (byte) 0x80, 0x00 })));
        assertEquals(0x0FFFFFFF, chunk.readOneToFour(new ByteArrayInputStream(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F })));
    }

    /**
     * Tests {@link HackedChunk#readOneToTwo(InputStream)}.
     * <pre>
     *  実際の数値 | 可変長での表現
     * ------------+-------------
     *  00000000   | 00
     *  00000040   | 40
     *  0000007F   | 7F
     *  00000081   | 80 01
     *  00000100   | 81 00
     *  00002000   | C0 00
     *  0000407F   | FF 7F
     * </pre>
     */
    @Test
    public void testReadOneToTwo() throws Exception {
        HackedChunk chunk = new HackedChunk();
        assertEquals(0x00000000, chunk.readOneToTwo(new ByteArrayInputStream(new byte[] { 0x00 })));
        assertEquals(0x00000040, chunk.readOneToTwo(new ByteArrayInputStream(new byte[] { 0x40 })));
        assertEquals(0x0000007F, chunk.readOneToTwo(new ByteArrayInputStream(new byte[] { 0x7F })));
        assertEquals(0x00000081, chunk.readOneToTwo(new ByteArrayInputStream(new byte[] { (byte) 0x80, 0x01 })));
        assertEquals(0x00000100, chunk.readOneToTwo(new ByteArrayInputStream(new byte[] { (byte) 0x81, 0x00 })));
        assertEquals(0x00002080, chunk.readOneToTwo(new ByteArrayInputStream(new byte[] { (byte) 0xC0, 0x00 })));
        assertEquals(0x0000407F, chunk.readOneToTwo(new ByteArrayInputStream(new byte[] { (byte) 0xFF, 0x7F })));
    }

    /**
     * Tests {@link SmafMessage#writeOneToTwo(OutputStream, int)}.
     */
    @Test
    public void test01() throws Exception {
        assertEquals(15000 / 4, wr(15000 / 4));
        assertEquals(0x0040, wr(0x0040));
        assertEquals(0x007f, wr(0x007f));
        assertEquals(0x0080, wr(0x0080));
        assertEquals(0x2000, wr(0x2000));
        assertEquals(0x3fff, wr(0x3fff));
    }

    /**
     * {@link SmafMessage#writeOneToTwo(OutputStream, int)}
     */
    private int wr(int v) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HackedMessage message = new HackedMessage();
        message.writeOneToTwo(baos, v);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        HackedChunk chunk = new HackedChunk();
        return chunk.readOneToTwo(bais);
    }
}

/* */
