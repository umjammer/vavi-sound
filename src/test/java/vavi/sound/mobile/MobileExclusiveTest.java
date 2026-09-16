/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.util.Arrays;
import java.util.List;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.sound.midi.MidiUtil.decode87;


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
        assertEquals(MobileExclusive.SYSEX_FUNCTION_ID_PACKED, data[1]);
        byte[] encoded = Arrays.copyOfRange(data, 2, data.length - 1);
        byte[] decoded = new byte[encoded.length];
        return Arrays.copyOf(decoded, decode87(encoded, decoded, 0, encoded.length));
    }

    @Test
    void layout() {
        byte[] wave = MobileExclusive.wave(3, MobileExclusive.Format.ADPCM, 1, 4, 8000, new byte[] {(byte) 0x81, 0x02});
        assertArrayEquals(new byte[] {0x45, 0x10, 3, 0, 1, 4, 0x1f, 0x40, (byte) 0x81, 0x02, (byte) 0xf7}, wave);
        assertArrayEquals(new byte[] {0x45, 0x11, 3, 100, 0x7f, (byte) 0xf7}, MobileExclusive.on(3, 100, MobileExclusive.NO_CHANNEL));
        assertArrayEquals(new byte[] {0x45, 0x12, 3, (byte) 0xf7}, MobileExclusive.off(3));

        assertEquals(MobileExclusive.Format.ADPCM, MobileExclusive.Format.valueOf(1));
        assertEquals(MobileExclusive.Format.ADPCM, MobileExclusive.Format.valueOf(0x82));
        assertEquals(MobileExclusive.Format.UNSIGNED, MobileExclusive.Format.valueOf(5));
    }

    /** code written against an engine and a receiver turns into the messages it would play */
    @Test
    void capture() throws Exception {
        byte[] adpcm = {(byte) 0xff, 0x00, 0x7f};
        List<MidiMessage> messages = MobileExclusive.capture(receiver -> {
            MobileExclusive.engine.setData(5, -1, 16000, 4, 1, adpcm, false);
            // the schedule runs at once when the engine is disabled
            AudioEngine.Sync.schedule(() -> MobileExclusive.engine.start(5));
            receiver.send(MobileExclusive.pack(MobileExclusive.volume(0, 64)), -1);
            MobileExclusive.engine.stop(5);
        });

        assertEquals(4, messages.size());
        assertArrayEquals(MobileExclusive.wave(5, MobileExclusive.Format.ADPCM, 1, 4, 16000, adpcm), unpack(messages.get(0)));
        assertArrayEquals(MobileExclusive.on(5, 127, MobileExclusive.NO_CHANNEL), unpack(messages.get(1)));
        assertArrayEquals(MobileExclusive.volume(0, 64), unpack(messages.get(2)));
        assertArrayEquals(MobileExclusive.off(5), unpack(messages.get(3)));

        // outside of a capture the engine drops what it is told
        MobileExclusive.engine.start(5);
    }

    /** a stream wave of a smaf file and the wave of a wave table voice are told apart */
    @Test
    void waveDataMessage() throws Exception {
        byte[] data = {0x12, (byte) 0x34};
        vavi.sound.smaf.vavi.message.MidiContext context = new vavi.sound.smaf.vavi.message.MidiContext();

        MidiEvent[] events = new WaveDataMessage(2, 1, data, 8000, 4, 1).getMidiEvents(context);
        assertEquals(1, events.length);
        assertArrayEquals(MobileExclusive.wave(2, MobileExclusive.Format.ADPCM, 1, 4, 8000, data), unpack(events[0].getMessage()));

        // "EXWV", 43 05 00 id <adpcm> f7
        events = new WaveDataMessage(2, 1, data, 8000, 4, 1).setWaveTable(true).getMidiEvents(context);
        assertArrayEquals(new byte[] {0x43, 0x05, 0x00, 2, 0x12, 0x34, (byte) 0xf7}, unpack(events[0].getMessage()));
    }

    @Test
    void enabled() {
        assertTrue(MobileExclusive.isEnabled());
        System.clearProperty(DISABLED);
        assertFalse(MobileExclusive.isEnabled());
    }
}
