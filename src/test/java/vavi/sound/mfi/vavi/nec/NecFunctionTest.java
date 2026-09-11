/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.sequencer.SmafExclusive;
import vavi.sound.mfi.vavi.sequencer.SmafExclusiveCapture;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests the MFi 4.0 (MA-7) NEC machine dependent functions.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
class NecFunctionTest {

    /** collects the SMAF exclusives the functions hand to the synthesizer */
    final SmafExclusiveCapture receiver = new SmafExclusiveCapture();

    @BeforeEach
    void setup() {
        receiver.clear();
    }

    /** wraps a function payload (vendor byte first) into a message the sequencer would feed back */
    private static MachineDependentMessage message(byte[] payload) throws Exception {
        MachineDependentMessage message = new MachineDependentMessage().init();
        message.setMessage(0, payload);
        return message;
    }

    @Test
    void factory() {
        assertInstanceOf(Function2_240_12.class, MachineDependentFunction.Factory.getFunction("16.2_240_12"));
        assertInstanceOf(Function2_240_14.class, MachineDependentFunction.Factory.getFunction("16.2_240_14"));
        assertInstanceOf(Function2_240_15.class, MachineDependentFunction.Factory.getFunction("16.2_240_15"));
        assertInstanceOf(Function1_241_10.class, MachineDependentFunction.Factory.getFunction("16.1_241_10"));
        assertInstanceOf(Function1_241_11.class, MachineDependentFunction.Factory.getFunction("16.1_241_11"));
        assertInstanceOf(Function2_241_10.class, MachineDependentFunction.Factory.getFunction("16.2_241_10"));
        assertInstanceOf(Function2_241_11.class, MachineDependentFunction.Factory.getFunction("16.2_241_11"));
        assertInstanceOf(Function2_241_13.class, MachineDependentFunction.Factory.getFunction("16.2_241_13"));
        assertInstanceOf(Function2_241_14.class, MachineDependentFunction.Factory.getFunction("16.2_241_14"));
        assertInstanceOf(Function2_241_15.class, MachineDependentFunction.Factory.getFunction("16.2_241_15"));
        assertInstanceOf(Function2_242_7.class, MachineDependentFunction.Factory.getFunction("16.2_242_7"));
        assertInstanceOf(Function2_243_3.class, MachineDependentFunction.Factory.getFunction("16.2_243_3"));
        assertInstanceOf(Function2_243_10.class, MachineDependentFunction.Factory.getFunction("16.2_243_10"));
        assertInstanceOf(Function2_243_11.class, MachineDependentFunction.Factory.getFunction("16.2_243_11"));
    }

    @Test
    void toneSetting() throws Exception {
        // a WT + AL voice, like the ones "14 Piano.mld" registers
        byte[] voice = new byte[Function2_240_12.Type.WTAL.length];
        voice[0] = (byte) Function2_240_12.Type.WTAL.flags;
        voice[1] = (byte) 0x2e;
        voice[voice.length - 1] = (byte) 0x80;

        Function2_240_12 out = new Function2_240_12();
        out.setBank(12);
        out.setProgram(0);
        out.setNote(0x80);
        out.setKeyHigh(0x34);
        out.setVoice(voice);

        byte[] payload = out.getMessage();
        assertEquals(4 + 4 + voice.length, payload.length);
        assertArrayEquals(new byte[] {0x11, 0x02, (byte) 0xf0, 0x0c, 0x0c, 0x00, (byte) 0x80, 0x34},
                java.util.Arrays.copyOf(payload, 8));

        Function2_240_12 in = new Function2_240_12();
        in.process(message(payload), receiver);
        assertEquals(12, in.getBank());
        assertFalse(in.isDrum());
        assertEquals(0, in.getProgram());
        assertEquals(0x80, in.getNote());
        assertEquals(0x34, in.getKeyHigh());
        assertEquals(Function2_240_12.Type.WTAL, in.getType());
        assertArrayEquals(voice, in.getVoice());

        // handed over as a VM35 PCM voice, bank 12 program 0 -> midi program 0, no drum note
        byte[] exclusive = receiver.getOnly();
        assertArrayEquals(new byte[] {0x43, 0x79, 0x07, 0x7f, 0x01, 0x00, 0x00, 0x00, 0x00,
                        (byte) SmafExclusive.VoiceType.PCM.ordinal()},
                Arrays.copyOf(exclusive, 10));
        assertArrayEquals(in.getVm35Voice(), Arrays.copyOfRange(exclusive, 10, exclusive.length - 1));
        assertEquals((byte) 0xf7, exclusive[exclusive.length - 1]);
    }

    @Test
    void toneSettingDrum() throws Exception {
        // "01 LINEAR.mld": 88 01 24 00, a WT voice for note 36 (= program 1 + 35)
        byte[] voice = new byte[Function2_240_12.Type.WT.length];
        voice[0] = (byte) Function2_240_12.Type.WT.flags;

        Function2_240_12 out = new Function2_240_12();
        out.setBank(8);
        out.setDrum(true);
        out.setProgram(1);
        out.setNote(1 + 35);
        out.setVoice(voice);

        Function2_240_12 in = new Function2_240_12();
        in.process(message(out.getMessage()), receiver);
        assertTrue(in.isDrum());
        assertEquals(8, in.getBank());
        assertEquals(0x24, in.getNote());
        assertEquals(0, in.getKeyHigh());
        assertEquals(Function2_240_12.Type.WT, in.getType());

        // bank 8 program 1 -> midi program 1, a drum voice so its note comes along
        byte[] exclusive = receiver.getOnly();
        assertEquals(0x01, exclusive[7] & 0xff);
        assertEquals(0x24, exclusive[8] & 0xff);
        assertEquals(SmafExclusive.VoiceType.PCM.ordinal(), exclusive[9] & 0xff);
    }

    /** the MA-7 register image folds back into the VM35 voice image a synthesizer takes */
    @Test
    void vm35FmVoice() throws Exception {
        for (Function2_240_12.Type type : new Function2_240_12.Type[] {
                Function2_240_12.Type.FM_2Op, Function2_240_12.Type.FM_2OpAL,
                Function2_240_12.Type.FM_4Op, Function2_240_12.Type.FM_4OpAL}) {

            int operators = type == Function2_240_12.Type.FM_2Op || type == Function2_240_12.Type.FM_2OpAL ? 2 : 4;

            byte[] voice = new byte[type.length];
            voice[0] = (byte) type.flags;
            voice[1] = 0x2e;    // KeyNumber
            voice[2] = 0x20;    // Panpot, BO
            voice[3] = 0x41;    // LFO, PE, ALG
            for (int op = 0; op < operators; op++) {
                int b = 4 + 10 * op;
                voice[b    ] = (byte) (0x10 + op);  // SR, XOF, SUS, KSR
                voice[b + 1] = (byte) (0x20 + op);  // RR, DR
                voice[b + 2] = (byte) (0x30 + op);  // AR, SL
                voice[b + 3] = (byte) (0x40 + op);  // TL, KSL
                voice[b + 4] = (byte) (0x50 + op);  // DAM, EAM, DVB, EVB
                voice[b + 5] = 0x0f;                // EXAR ~ EXRR, no VM35 field for it
                voice[b + 6] = (byte) (0x60 + op);  // WS, FB
                voice[b + 7] = 0x1f;                // FIXBLOCK, FIXFnum(H), AL only
                voice[b + 8] = (byte) 0xff;         // FIXFnum(L), AL only
                voice[b + 9] = (byte) (0x70 + op);  // MULTI, DT
            }

            Function2_240_12 out = new Function2_240_12();
            out.setBank(1);
            out.setProgram(3);
            out.setVoice(voice);

            Function2_240_12 in = new Function2_240_12();
            in.process(message(out.getMessage()), receiver);
            assertEquals(type, in.getType());

            byte[] expected = new byte[3 + 7 * operators];
            expected[0] = 0x2e;
            expected[1] = 0x20;
            expected[2] = 0x41;
            for (int op = 0; op < operators; op++) {
                int b = 3 + 7 * op;
                expected[b    ] = (byte) (0x10 + op);
                expected[b + 1] = (byte) (0x20 + op);
                expected[b + 2] = (byte) (0x30 + op);
                expected[b + 3] = (byte) (0x40 + op);
                expected[b + 4] = (byte) (0x50 + op);
                expected[b + 5] = (byte) (0x70 + op);   // MULTI, DT moves up
                expected[b + 6] = (byte) (0x60 + op);
            }
            assertArrayEquals(expected, in.getVm35Voice(), type.toString());

            // bank 1 program 3 -> midi program 0x43
            byte[] exclusive = receiver.getOnly();
            assertEquals(0x43, exclusive[7] & 0xff);
            assertEquals(SmafExclusive.VoiceType.FM.ordinal(), exclusive[9] & 0xff);
            receiver.clear();
        }
    }

    /** the wave table image loses the EX rates and, when it has a filter, keeps its wave id */
    @Test
    void vm35WtVoice() throws Exception {
        for (Function2_240_12.Type type : new Function2_240_12.Type[] {
                Function2_240_12.Type.WT, Function2_240_12.Type.WTAL}) {

            byte[] voice = new byte[type.length];
            voice[0] = (byte) type.flags;
            for (int i = 1; i <= 9; i++) {
                voice[i] = (byte) (0x10 + i);           // Fs(MSB) ~ DAM, EAM, DVB, EVB
            }
            voice[10] = 0x0f;                           // EXAR ~ EXRR, no VM35 field for it
            for (int i = 11; i <= 16; i++) {
                voice[i] = (byte) (0x20 + i);           // StartAddressOffset ~ EndPoint(LSB)
            }
            voice[type == Function2_240_12.Type.WTAL ? 33 : 17] = (byte) 0x83; // RM, WaveID

            Function2_240_12 out = new Function2_240_12();
            out.setVoice(voice);

            Function2_240_12 in = new Function2_240_12();
            in.process(message(out.getMessage()), receiver);
            assertEquals(type, in.getType());

            byte[] expected = new byte[16];
            for (int i = 0; i < 9; i++) {
                expected[i] = (byte) (0x11 + i);
            }
            for (int i = 0; i < 6; i++) {
                expected[9 + i] = (byte) (0x2b + i);
            }
            expected[15] = (byte) 0x83;
            assertArrayEquals(expected, in.getVm35Voice(), type.toString());

            assertEquals(SmafExclusive.VoiceType.PCM.ordinal(), receiver.getOnly()[9] & 0xff);
            receiver.clear();
        }
    }

    /** an unknown voice shape is decoded but has nothing to hand over */
    @Test
    void vm35UnknownVoice() throws Exception {
        // the 43 byte "0x05 / 0x07" variants, 4 messages in ~4400 files
        byte[] voice = new byte[43];
        voice[0] = 0x07;

        Function2_240_12 out = new Function2_240_12();
        out.setVoice(voice);

        Function2_240_12 in = new Function2_240_12();
        in.process(message(out.getMessage()), receiver);
        assertEquals(null, in.getType());
        assertEquals(null, in.getVm35Voice());
        assertTrue(receiver.getExclusives().isEmpty());
    }

    @Test
    void toneSettingTypes() {
        assertEquals(Function2_240_12.Type.FM_2Op, Function2_240_12.Type.valueOf(0x00, 24));
        assertEquals(Function2_240_12.Type.FM_4Op, Function2_240_12.Type.valueOf(0x00, 44));
        assertEquals(Function2_240_12.Type.WT, Function2_240_12.Type.valueOf(0x01, 18));
        assertEquals(Function2_240_12.Type.FM_2OpAL, Function2_240_12.Type.valueOf(0x02, 40));
        assertEquals(Function2_240_12.Type.FM_4OpAL, Function2_240_12.Type.valueOf(0x02, 60));
        assertEquals(Function2_240_12.Type.WTAL, Function2_240_12.Type.valueOf(0x03, 34));
        // the 43 byte "0x05 / 0x07" variants are still unknown
        assertEquals(null, Function2_240_12.Type.valueOf(0x01, 43));
    }

    @Test
    void channelStatus() throws Exception {
        // "18 Cyber Call.mld" uses 7 channels, the rest is 0x10
        int[] statuses = new int[Function2_242_7.CHANNELS];
        java.util.Arrays.fill(statuses, 0x10);
        statuses[0] = 0x0f;
        for (int i = 1; i < 7; i++) {
            statuses[i] = 0x00;
        }

        Function2_242_7 out = new Function2_242_7();
        out.setChannelStatuses(statuses);

        byte[] payload = out.getMessage();
        assertEquals(4 + Function2_242_7.CHANNELS, payload.length);

        Function2_242_7 in = new Function2_242_7();
        in.process(message(payload), receiver);
        assertArrayEquals(statuses, in.getChannelStatuses());
        assertTrue(in.isUsed(0));
        assertTrue(in.isUsed(6));
        assertFalse(in.isUsed(7));
        assertEquals(3, in.getType(0));  // rhythm
        assertEquals(0, in.getType(1));  // no care

        assertThrows(vavi.sound.mfi.InvalidMfiDataException.class, () -> {
            MachineDependentMessage short_ = new MachineDependentMessage().init();
            short_.setMessage(0, new byte[] {0x11, 0x02, (byte) 0xf2, 0x07, 0x00});
            new Function2_242_7().process(short_, receiver);
        });
    }

    @Test
    void channelStatusRotation() {
        for (int i = 0; i < 0x100; i++) {
            assertEquals(i, Function2_242_7.toSmaf(Function2_242_7.toMfi(i)));
        }
        // KCS 2 (on), vibration on, type rhythm -> the two top bits move down
        assertEquals(0x0f, Function2_242_7.toMfi(0xc3));
        assertEquals(0xc3, Function2_242_7.toSmaf(0x0f));
    }

    @Test
    void sendLevel() throws Exception {
        Function2_241_14 out = new Function2_241_14();
        out.setChannel(2);
        out.setValue(0x50);

        byte[] payload = out.getMessage();
        assertArrayEquals(new byte[] {0x11, 0x02, (byte) 0xf1, (byte) 0x8e, 0x50}, payload);

        Function2_241_14 in = new Function2_241_14();
        in.process(message(payload), receiver);
        assertEquals(2, in.getChannel());
        assertEquals(0x50, in.getValue());
        assertFalse(in.isFlag());

        // the 0x2d / 0xad / 0xed form real files contain
        Function2_241_13 flagged = new Function2_241_13();
        flagged.process(message(new byte[] {0x11, 0x02, (byte) 0xf1, (byte) 0xed, 0x7f}), receiver);
        assertEquals(3, flagged.getChannel());
        assertTrue(flagged.isFlag());
        assertEquals(0x7f, flagged.getValue());
    }

    @Test
    void filter() throws Exception {
        // the corpus centres both of them on 64
        Function1_241_10 resonance = new Function1_241_10();
        resonance.setChannel(1);
        resonance.setValue(64);
        assertArrayEquals(new byte[] {0x11, 0x01, (byte) 0xf1, 0x4a, 0x40}, resonance.getMessage());

        Function2_241_11 brightness = new Function2_241_11();
        brightness.setChannel(3);
        brightness.setValue(100);
        assertArrayEquals(new byte[] {0x11, 0x02, (byte) 0xf1, (byte) 0xcb, 0x64}, brightness.getMessage());

        Function2_241_11 in = new Function2_241_11();
        in.process(message(brightness.getMessage()), receiver);
        assertEquals(3, in.getChannel());
        assertEquals(100, in.getValue());
    }

    /** the level 0x02 f1 messages that carry one byte where level 0x01 carries none */
    @Test
    void level2Switches() throws Exception {
        assertInstanceOf(Function2_241_7.class, MachineDependentFunction.Factory.getFunction("16.2_241_7"));
        assertInstanceOf(Function2_241_8.class, MachineDependentFunction.Factory.getFunction("16.2_241_8"));
        assertInstanceOf(Function2_241_9.class, MachineDependentFunction.Factory.getFunction("16.2_241_9"));
        assertInstanceOf(Function2_241_12.class, MachineDependentFunction.Factory.getFunction("16.2_241_12"));

        Function2_241_8 monoOn = new Function2_241_8();
        monoOn.setChannel(1);
        monoOn.setValue(1);
        assertArrayEquals(new byte[] {0x11, 0x02, (byte) 0xf1, 0x48, 0x01}, monoOn.getMessage());

        Function2_241_8 in = new Function2_241_8();
        in.process(message(monoOn.getMessage()), receiver);
        assertEquals(1, in.getChannel());
        assertEquals(1, in.getValue());
    }

    @Test
    void effectData() throws Exception {
        byte[] block = new byte[EffectDataFunction.BLOCK];
        block[0] = 0x11;
        block[2] = 0x30;

        Function2_240_14 out = new Function2_240_14();
        out.setData(block);

        Function2_240_14 in = new Function2_240_14();
        in.process(message(out.getMessage()), receiver);
        assertEquals(1, in.getBlockCount());
        assertArrayEquals(block, in.getBlock(0));

        assertThrows(vavi.sound.mfi.InvalidMfiDataException.class, () -> new Function2_240_15().setData(new byte[31]));
    }

    @Test
    void maxGainAndSfxChange() throws Exception {
        Function2_243_3 gain = new Function2_243_3();
        gain.setMaxGain(6);
        Function2_243_3 gainIn = new Function2_243_3();
        gainIn.process(message(gain.getMessage()), receiver);
        assertEquals(6, gainIn.getMaxGain());

        Function2_243_11 sfx = new Function2_243_11();
        sfx.setSfxId(0x40);
        assertArrayEquals(new byte[] {0x11, 0x02, (byte) 0xf3, 0x0b, 0x40, (byte) 0xf7}, sfx.getMessage());
        Function2_243_11 sfxIn = new Function2_243_11();
        sfxIn.process(message(sfx.getMessage()), receiver);
        assertEquals(0x40, sfxIn.getSfxId());
        assertThrows(IllegalArgumentException.class, () -> new Function2_243_11().setSfxId(0x20));
    }
}
