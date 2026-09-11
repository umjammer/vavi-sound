/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.util.Arrays;

import javax.sound.midi.SysexMessage;

import org.junit.jupiter.api.Test;

import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.smaf.message.MidiContext;
import vavi.sound.smaf.message.yamaha.YamahaMessage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests the SMAF exclusives an MFi tone message is handed over as.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
class SmafExclusiveTest {

    private static byte[] bytes(int... values) {
        byte[] b = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            b[i] = (byte) values[i];
        }
        return b;
    }

    @Test
    void voice() {
        byte[] image = new byte[17];
        image[0] = 0x2e;
        image[16] = (byte) 0xff;

        byte[] exclusive = SmafExclusive.voice(0, 3, 0x41, 36, SmafExclusive.VoiceType.FM, image);

        assertEquals(10 + 17 + 1, exclusive.length);
        assertArrayEquals(bytes(0x43, 0x79, 0x07, 0x7f, 0x01, 0x00, 0x03, 0x41, 0x24, 0x00),
                Arrays.copyOf(exclusive, 10));
        assertArrayEquals(image, Arrays.copyOfRange(exclusive, 10, 27));
        assertEquals((byte) 0xf7, exclusive[exclusive.length - 1]);
    }

    @Test
    void voiceType() {
        // the ordinal is the wire value, see smaf825 / vavi-sound-ma
        assertEquals(0, SmafExclusive.VoiceType.FM.ordinal());
        assertEquals(1, SmafExclusive.VoiceType.PCM.ordinal());
        assertEquals(2, SmafExclusive.VoiceType.AL.ordinal());
    }

    @Test
    void wave() {
        byte[] adpcm = new byte[1526];
        adpcm[0] = (byte) 0xf0;

        byte[] exclusive = SmafExclusive.wave(3, adpcm);

        assertArrayEquals(bytes(0x43, 0x05, 0x00, 0x03), Arrays.copyOf(exclusive, 4));
        assertArrayEquals(adpcm, Arrays.copyOfRange(exclusive, 4, 4 + adpcm.length));
        assertEquals((byte) 0xf7, exclusive[exclusive.length - 1]);
    }

    /** the packing must come out byte for byte like the smaf player's */
    @Test
    void packIsTheSameAsYamahaMessage() throws Exception {
        byte[] exclusive = SmafExclusive.voice(0, 0, 0x41, 0, SmafExclusive.VoiceType.PCM, new byte[16]);

        YamahaMessage yamahaMessage = new YamahaMessage();
        yamahaMessage.setMessage(0xf0, exclusive, exclusive.length);

        assertArrayEquals(yamahaMessage.getMidiEvents(new MidiContext())[0].getMessage().getMessage(),
                SmafExclusive.pack(exclusive).getMessage());
    }

    /** every length must survive the 8 -> 7 bit packing, the boundaries above all */
    @Test
    void packRoundTrip() throws Exception {
        SmafExclusiveCapture capture = new SmafExclusiveCapture();

        for (int length = 1; length < 64; length++) {
            byte[] exclusive = new byte[length];
            for (int i = 0; i < length - 1; i++) {
                exclusive[i] = (byte) (0x80 | i); // the 8th bit is what the packing is about
            }
            exclusive[length - 1] = (byte) 0xf7;

            capture.clear();
            SmafExclusive.send(capture, exclusive);
            assertArrayEquals(exclusive, capture.getOnly(), "length " + length);
        }
    }

    @Test
    void packedSysexIsAVaviSysex() throws Exception {
        SysexMessage message = SmafExclusive.pack(SmafExclusive.wave(0, new byte[] {0x01, 0x02}));

        assertEquals(0xf0, message.getStatus());
        assertEquals(VaviMidiDeviceProvider.MANUFACTURER_ID, message.getData()[0]);
        assertEquals(SmafExclusive.SYSEX_PACKED, message.getData()[1]);
    }

    /** a null receiver is not an error, the message it comes from is decoded either way */
    @Test
    void sendToNothing() {
        SmafExclusive.send(null, SmafExclusive.wave(0, new byte[0]));
    }
}
