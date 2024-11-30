/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.logging.Level;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import org.junit.jupiter.api.Test;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * MidiUtilTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080701 nsano initial version <br>
 */
public class MidiUtilTest {

    /**
     * Tests {@link MidiUtil#readVariableLength(java.io.DataInput)}.
     * <pre>
     *  actual number | variable length expression
     * ---------------+----------------------------
     *  00000000      | 00
     *  00000040      | 40
     *  0000007f      | 7f
     *  00000080      | 81 00
     *  00002000      | c0 00
     *  00003fff      | ff 7f
     *  00004000      | 81 80 00
     *  00100000      | c0 80 00
     *  001fffff      | ff ff 7f
     *  00200000      | 81 80 80 00
     *  08000000      | c0 80 80 00
     *  0fffffff      | ff ff ff 7f
     * </pre>
     */
    @Test
    public void testReadOneToFour() throws Exception {
        assertEquals(0x0000_0000, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { 0x00 }))));
        assertEquals(0x0000_0040, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { 0x40 }))));
        assertEquals(0x0000_007f, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { 0x7f }))));
        assertEquals(0x0000_0080, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { (byte) 0x81, 0x00 }))));
        assertEquals(0x0000_2000, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { (byte) 0xc0, 0x00 }))));
        assertEquals(0x0000_3fff, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { (byte) 0xff, 0x7f }))));
        assertEquals(0x0000_4000, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { (byte) 0x81, (byte) 0x80, 0x00 }))));
        assertEquals(0x0010_0000, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { (byte) 0xc0, (byte) 0x80, 0x00 }))));
        assertEquals(0x001f_ffff, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { (byte) 0xff, (byte) 0xff, 0x7f }))));
        assertEquals(0x0020_0000, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { (byte) 0x81, (byte) 0x80, (byte) 0x80, 0x00 }))));
        assertEquals(0x0800_0000, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { (byte) 0xc0, (byte) 0x80, (byte) 0x80, 0x00 }))));
        assertEquals(0x0fff_ffff, MidiUtil.readVariableLength(new DataInputStream(new ByteArrayInputStream(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x7f }))));
    }

    /**
     * @param args 0: midi
     */
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);

        Sequence sequence = MidiSystem.getSequence(file);
Debug.println(Level.FINE, "sequence: " + sequence);

        Sequencer sequencer = MidiSystem.getSequencer();
Debug.println(Level.FINE, "sequencer: " + sequencer);
        sequencer.open();
        sequencer.setSequence(sequence);
        sequencer.start();
        while (sequencer.isRunning()) {
            Thread.yield();
        }
        sequencer.stop();
        sequencer.close();
    }

    /**
     * {@link MidiUtil#readVariableLength(java.io.DataInput)}
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
     * {@link MidiUtil#readVariableLength(java.io.DataInput)}
     */
    private static int wr(int v) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MidiUtil.writeVarInt(new DataOutputStream(baos), v);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return MidiUtil.readVariableLength(new DataInputStream(bais));
    }
}
