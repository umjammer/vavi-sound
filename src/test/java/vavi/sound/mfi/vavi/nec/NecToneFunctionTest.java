/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.util.List;

import org.junit.jupiter.api.Test;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests the MFi 3.0 (MA-3 / MA-5) NEC tone and waveform functions.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
class NecToneFunctionTest {

    private static MachineDependentMessage message(byte[] payload) throws Exception {
        MachineDependentMessage message = new MachineDependentMessage().init();
        message.setMessage(0, payload);
        return message;
    }

    private static byte[] bytes(int... values) {
        byte[] b = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            b[i] = (byte) values[i];
        }
        return b;
    }

    @Test
    void factory() {
        assertInstanceOf(Function1_240_4.class, MachineDependentFunction.Factory.getFunction("16.1_240_4"));
        assertInstanceOf(Function1_240_5.class, MachineDependentFunction.Factory.getFunction("16.1_240_5"));
        assertInstanceOf(Function1_240_6.class, MachineDependentFunction.Factory.getFunction("16.1_240_6"));
        assertInstanceOf(Function1_240_8.class, MachineDependentFunction.Factory.getFunction("16.1_240_8"));
        assertInstanceOf(Function1_241_7.class, MachineDependentFunction.Factory.getFunction("16.1_241_7"));
        assertInstanceOf(Function1_241_8.class, MachineDependentFunction.Factory.getFunction("16.1_241_8"));
        assertInstanceOf(Function1_241_9.class, MachineDependentFunction.Factory.getFunction("16.1_241_9"));
        assertInstanceOf(Function1_242_7.class, MachineDependentFunction.Factory.getFunction("16.1_242_7"));
        assertInstanceOf(Function1_243_1.class, MachineDependentFunction.Factory.getFunction("16.1_243_1"));
        assertInstanceOf(Function1_243_7.class, MachineDependentFunction.Factory.getFunction("16.1_243_7"));
        assertInstanceOf(Function1_243_10.class, MachineDependentFunction.Factory.getFunction("16.1_243_10"));
    }

    /** two 4 operator voices and one 2 operator voice in one message, the shapes real files use */
    @Test
    void fmTone() throws Exception {
        byte[] rec4a = new byte[34];
        rec4a[0] = Function1_240_4.TYPE_4OP;
        rec4a[1] = 0x07;        // bank 7
        rec4a[2] = 0x01;        // program 1
        rec4a[5] = 0x43;        // LFO, PE, ALG
        byte[] rec2 = new byte[20];
        rec2[0] = Function1_240_4.TYPE_2OP;
        rec2[1] = (byte) 0x85;  // bank 5, drum
        rec2[2] = 0x10;         // program 16
        rec2[5] = 0x41;
        byte[] rec4b = new byte[34];
        rec4b[0] = Function1_240_4.TYPE_4OP;
        rec4b[1] = 0x06;
        rec4b[2] = 0x1d;
        rec4b[5] = 0x40;        // 4 operator although ALG is 0, this really happens

        byte[] payload = new byte[4 + rec4a.length + rec2.length + rec4b.length];
        payload[0] = 0x11; payload[1] = 0x01; payload[2] = (byte) 0xf0; payload[3] = 0x04;
        System.arraycopy(rec4a, 0, payload, 4, 34);
        System.arraycopy(rec2, 0, payload, 38, 20);
        System.arraycopy(rec4b, 0, payload, 58, 34);

        Function1_240_4 in = new Function1_240_4();
        in.process(message(payload));
        List<ToneFunction.Tone> tones = in.getTones();
        assertEquals(3, tones.size());

        assertEquals(Function1_240_4.TYPE_4OP, tones.get(0).type);
        assertEquals(7, tones.get(0).bank);
        assertFalse(tones.get(0).drum);
        assertEquals(1, tones.get(0).program);
        assertEquals(31, tones.get(0).voice.length);

        assertEquals(Function1_240_4.TYPE_2OP, tones.get(1).type);
        assertEquals(5, tones.get(1).bank);
        assertTrue(tones.get(1).drum);
        assertEquals(16, tones.get(1).program);
        assertEquals(17, tones.get(1).voice.length);

        assertEquals(0x1d, tones.get(2).program);
        assertEquals(31, tones.get(2).voice.length);

        assertArrayEquals(payload, in.getMessage());
    }

    /** "(I NEED A)MIRACLE" registers this one: bank 4 + drum, program 1, 12000Hz, rom wave 0 */
    @Test
    void wtTone() throws Exception {
        byte[] payload = bytes(0x11, 0x01, 0xf0, 0x05,
                0x84, 0x01,
                0x2e, 0xe0, 0x79, 0x00, 0x08, 0xf0, 0xf0, 0x10, 0x00, 0x00, 0x00, 0x03, 0xa9, 0x03, 0xa9, 0x80);

        Function1_240_5 in = new Function1_240_5();
        in.process(message(payload));
        assertEquals(1, in.getTones().size());

        ToneFunction.Tone tone = in.getTones().get(0);
        assertEquals(-1, tone.type);
        assertEquals(4, tone.bank);
        assertTrue(tone.drum);
        assertEquals(1, tone.program);
        assertEquals(Function1_240_5.VOICE, tone.voice.length);
        assertEquals(12000, Function1_240_5.getSamplingRate(tone));
        assertEquals(937, Function1_240_5.getLoopPoint(tone));
        assertEquals(937, Function1_240_5.getEndPoint(tone));
        assertTrue(Function1_240_5.isRomWave(tone));
        assertEquals(0, Function1_240_5.getWaveId(tone));

        assertArrayEquals(payload, in.getMessage());
    }

    @Test
    void alTone() throws Exception {
        for (int[] shape : new int[][] {{Function1_240_8.TYPE_WT, 46, 43}, {Function1_240_8.TYPE_FM, 47, 44}, {Function1_240_8.TYPE_FM, 61, 58}}) {
            byte[] payload = new byte[4 + shape[1]];
            payload[0] = 0x11; payload[1] = 0x01; payload[2] = (byte) 0xf0; payload[3] = 0x08;
            payload[4] = (byte) shape[0];
            payload[5] = 0x07;  // bank
            payload[6] = 0x10;  // program

            Function1_240_8 in = new Function1_240_8();
            in.process(message(payload));
            assertEquals(1, in.getTones().size());
            assertEquals(shape[0], in.getTones().get(0).type);
            assertEquals(7, in.getTones().get(0).bank);
            assertEquals(0x10, in.getTones().get(0).program);
            assertEquals(shape[2], in.getTones().get(0).voice.length);
            assertArrayEquals(payload, in.getMessage());
        }
    }

    @Test
    void wtWave() throws Exception {
        byte[] data = new byte[1526];
        data[0] = (byte) 0xf0;

        Function1_240_6 out = new Function1_240_6();
        out.setWaveId(0);
        out.setFormat(Function1_240_6.FORMAT_ADPCM);
        out.setData(data);

        Function1_240_6 in = new Function1_240_6();
        in.process(message(out.getMessage()));
        assertEquals(0, in.getWaveId());
        assertEquals(Function1_240_6.FORMAT_ADPCM, in.getFormat());
        assertEquals(1526, in.getData().length);
        // the end point of the voice that uses the wave, 3052 in "01 キラキラ.mld"
        assertEquals(3052, in.getSampleCount());

        out.setFormat(Function1_240_6.FORMAT_PCM8);
        in.process(message(out.getMessage()));
        assertEquals(1526, in.getSampleCount());

        assertThrows(InvalidMfiDataException.class,
                () -> new Function1_240_6().process(message(bytes(0x11, 0x01, 0xf0, 0x06, 0x00))));
    }

    @Test
    void channelStatus() throws Exception {
        int[] statuses = new int[Function1_242_7.CHANNELS];
        statuses[3] = 0x01;     // melody
        statuses[8] = 0x32;     // no melody, LED and vibration on

        Function1_242_7 out = new Function1_242_7();
        out.setChannelStatuses(statuses);
        assertEquals(4 + 16, out.getMessage().length);

        Function1_242_7 in = new Function1_242_7();
        in.process(message(out.getMessage()));
        assertArrayEquals(statuses, in.getChannelStatuses());
        assertEquals(1, in.getType(3));
        assertEquals(2, in.getType(8));
        assertEquals(0, in.getKeyControlStatus(8));

        assertThrows(InvalidMfiDataException.class,
                () -> new Function1_242_7().process(message(bytes(0x11, 0x01, 0xf2, 0x07, 0x00))));
    }

    /**
     * the five 0x81 level messages of "威風堂々　クラシカル.mld", the only file that has them
     *
     * @see Level1AliasFunction
     */
    @Test
    void level0x81Alias() throws Exception {
        assertInstanceOf(Function129_0.class, MachineDependentFunction.Factory.getFunction("16.129_0"));
        assertInstanceOf(Function129_2.class, MachineDependentFunction.Factory.getFunction("16.129_2"));
        assertInstanceOf(Function129_3.class, MachineDependentFunction.Factory.getFunction("16.129_3"));

        // 11 81 f0 05 84 00 27 10 ... a 10000Hz WT tone for bank 4 (drum), program 0
        byte[] wt = bytes(0x11, 0x81, 0xf0, 0x05,
                0x84, 0x00,
                0x27, 0x10, 0x79, 0x00, 0x08, 0xf0, 0xf0, 0x10, 0x00, 0x00, 0x00, 0x03, 0xa9, 0x03, 0xa9, 0x80);
        new Function129_0().process(message(wt));

        Function1_240_5 delegate = (Function1_240_5) MachineDependentFunction.Factory.getFunction("16.1_240_5");
        assertEquals(1, delegate.getTones().size());
        assertEquals(4, delegate.getTones().get(0).bank);
        assertTrue(delegate.getTones().get(0).drum);
        assertEquals(0, delegate.getTones().get(0).program);
        assertEquals(10000, Function1_240_5.getSamplingRate(delegate.getTones().get(0)));

        // 11 81 f3 01 00 FM mode setting
        new Function129_3().process(message(bytes(0x11, 0x81, 0xf3, 0x01, 0x00)));
        assertEquals(0, ((Function1_243_1) MachineDependentFunction.Factory.getFunction("16.1_243_1")).getValue());

        // 11 81 f2 07 + 16 bytes of channel status
        byte[] cs = new byte[4 + Function1_242_7.CHANNELS];
        cs[0] = 0x11; cs[1] = (byte) 0x81; cs[2] = (byte) 0xf2; cs[3] = 0x07;
        cs[4 + 15] = 0x02;
        new Function129_2().process(message(cs));
        assertEquals(2, ((Function1_242_7) MachineDependentFunction.Factory.getFunction("16.1_242_7")).getType(15));

        assertThrows(InvalidMfiDataException.class,
                () -> new Function129_3().process(message(bytes(0x11, 0x81, 0xf3))));
    }

    /** the shape the three MFi 2.0 files that have it write */
    @Test
    void unknownF206() throws Exception {
        assertInstanceOf(Function242_6.class, MachineDependentFunction.Factory.getFunction("16.242_6"));

        Function242_6 out = new Function242_6();
        out.setChannel(0);
        out.setData1(0x40);
        out.setData2(0x00);
        assertArrayEquals(bytes(0x11, 0xf2, 0x06, 0x40, 0x00), out.getMessage());

        Function242_6 in = new Function242_6();
        in.process(message(out.getMessage()));
        assertEquals(0, in.getChannel());
        assertEquals(0x40, in.getData1());
        assertEquals(0x00, in.getData2());

        assertThrows(InvalidMfiDataException.class,
                () -> new Function242_6().process(message(bytes(0x11, 0xf2, 0x06, 0x00))));
    }

    @Test
    void switchesAndGlobals() throws Exception {
        Function1_241_9 playOn = new Function1_241_9();
        playOn.setChannel(2);
        assertArrayEquals(bytes(0x11, 0x01, 0xf1, 0x89), playOn.getMessage());
        Function1_241_9 in = new Function1_241_9();
        in.process(message(playOn.getMessage()));
        assertEquals(2, in.getChannel());

        Function1_241_7 hold1 = new Function1_241_7();
        hold1.setChannel(1);
        hold1.setValue(0x7f);
        assertArrayEquals(bytes(0x11, 0x01, 0xf1, 0x47, 0x7f), hold1.getMessage());

        Function1_243_1 fmMode = new Function1_243_1();
        fmMode.setValue(1);
        assertArrayEquals(bytes(0x11, 0x01, 0xf3, 0x01, 0x01), fmMode.getMessage());
        Function1_243_1 fmModeIn = new Function1_243_1();
        fmModeIn.process(message(fmMode.getMessage()));
        assertEquals(1, fmModeIn.getValue());
    }
}
