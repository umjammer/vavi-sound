/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.util.Arrays;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.SysexMessage;

import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.smaf.vavi.message.WaveDataMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.mfi.vavi.sequencer.AudioDataSequencer.MFi_SYSEX_FUNCTION_ID_MFi4;
import static vavi.sound.midi.MidiUtil.decode87;
import static vavi.sound.smaf.vavi.sequencer.WaveSequencer.SMAF_SYSEX_FUNCTION_ID_WAVE;


/**
 * StreamExclusiveTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-13 nsano initial version <br>
 */
class MobileExclusiveTest {

    static final String DISABLED = "vavi.sound.mobile.AudioEngine.disabled";

    String old;

    @BeforeEach
    void setUp() {
        old = System.setProperty(DISABLED, "true");
    }

    @AfterEach
    void tearDown() {
        if (old == null) System.clearProperty(DISABLED); else System.setProperty(DISABLED, old);
    }

    /** f0 45 7f &lt;encode87(exclusive)&gt; f7 back to the exclusive */
    static byte[] unpack(MidiMessage message) {
        byte[] data = ((SysexMessage) message).getData();
        assertEquals(VaviMidiDeviceProvider.MANUFACTURER_ID, data[0]);
        assertEquals(MobileExclusive.MIDI_SYSEX_FUNCTION_ID_PACKED, data[1]);
        byte[] encoded = Arrays.copyOfRange(data, 2, data.length - 1);
        byte[] decoded = new byte[encoded.length];
        return Arrays.copyOf(decoded, decode87(encoded, decoded, 0, encoded.length));
    }

    @Test
    void layout() {
        byte[] wave = MobileExclusive.wave(MFi_SYSEX_FUNCTION_ID_MFi4, 3, 0x82, 1, 4, 8000, new byte[] {(byte) 0x81, 0x02});
        assertArrayEquals(new byte[] {0x45, 0x02, 0x10, 3, (byte) 0x82, 1, 4, 0x1f, 0x40, (byte) 0x81, 0x02, (byte) 0xf7}, wave);
        assertArrayEquals(new byte[] {0x45, 0x02, 0x11, 3, 100, 0x7f, (byte) 0xf7}, MobileExclusive.on(MFi_SYSEX_FUNCTION_ID_MFi4, 3, 100, MobileExclusive.NO_CHANNEL));
        assertArrayEquals(new byte[] {0x45, 0x02, 0x12, 3, (byte) 0xf7}, MobileExclusive.off(MFi_SYSEX_FUNCTION_ID_MFi4, 3));
    }

    /** a smaf wave start carries its gate time, the receiver side is stateless */
    @Test
    void onWithGateTime() {
        byte[] on = MobileExclusive.on(0x03, 1, 127, 0, 0x123456);
        assertArrayEquals(new byte[] {0x45, 0x03, 0x11, 1, 127, 0, 0x12, 0x34, 0x56, (byte) 0xf7}, on);
        assertEquals(0x123456, MobileExclusive.gateTime(Arrays.copyOfRange(on, 2, on.length)));
        byte[] plain = MobileExclusive.on(0x03, 1, 127, 0);
        assertEquals(-1, MobileExclusive.gateTime(Arrays.copyOfRange(plain, 2, plain.length)));
    }

    /** a stream wave of a smaf file and the wave of a wave table voice are told apart */
    @Test
    void waveDataMessage() throws Exception {
        byte[] data = {0x12, (byte) 0x34};
        vavi.sound.smaf.vavi.message.MidiContext context = new vavi.sound.smaf.vavi.message.MidiContext();

        MidiEvent[] events = new WaveDataMessage().init(2, 1, data, 8000, 4, 1).getMidiEvents(context);
        assertEquals(1, events.length);
        assertArrayEquals(MobileExclusive.wave(SMAF_SYSEX_FUNCTION_ID_WAVE, 2, 1, 1, 4, 8000, data), unpack(events[0].getMessage()));

        // "EXWV", 43 05 00 id <adpcm> f7
        events = new WaveDataMessage().init(2, 1, data, 8000, 4, 1).setWaveTable(true).getMidiEvents(context);
        assertArrayEquals(new byte[] {0x43, 0x05, 0x00, 2, 0x12, 0x34, (byte) 0xf7}, unpack(events[0].getMessage()));
    }
}
