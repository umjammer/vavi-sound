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
class YamahaExclusiveTest {

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
        assertEquals(YamahaExclusive.SYSEX_PACKED, data[1]);
        byte[] encoded = Arrays.copyOfRange(data, 2, data.length - 1);
        byte[] decoded = new byte[encoded.length];
        return Arrays.copyOf(decoded, decode87(encoded, decoded, 0, encoded.length));
    }

    @Test
    void layout() {
        byte[] wave = YamahaExclusive.wave(3, YamahaExclusive.Format.ADPCM, 1, 4, 8000, new byte[] {(byte) 0x81, 0x02});
        assertArrayEquals(new byte[] {0x45, 0x10, 3, 0, 1, 4, 0x1f, 0x40, (byte) 0x81, 0x02, (byte) 0xf7}, wave);
        assertArrayEquals(new byte[] {0x45, 0x11, 3, 100, 0x7f, (byte) 0xf7}, YamahaExclusive.on(3, 100, YamahaExclusive.NO_CHANNEL));
        assertArrayEquals(new byte[] {0x45, 0x12, 3, (byte) 0xf7}, YamahaExclusive.off(3));

        assertEquals(YamahaExclusive.Format.ADPCM, YamahaExclusive.Format.valueOf(1));
        assertEquals(YamahaExclusive.Format.ADPCM, YamahaExclusive.Format.valueOf(0x82));
        assertEquals(YamahaExclusive.Format.UNSIGNED, YamahaExclusive.Format.valueOf(5));
    }

    /** code written against an engine and a receiver turns into the messages it would play */
    @Test
    void capture() throws Exception {
        byte[] adpcm = {(byte) 0xff, 0x00, 0x7f};
        List<MidiMessage> messages = YamahaExclusive.capture(receiver -> {
            YamahaExclusive.engine.setData(5, -1, 16000, 4, 1, adpcm, false);
            // the schedule runs at once when the engine is disabled
            AudioEngine.Sync.schedule(() -> YamahaExclusive.engine.start(5));
            receiver.send(YamahaExclusive.pack(YamahaExclusive.volume(0, 64)), -1);
            YamahaExclusive.engine.stop(5);
        });

        assertEquals(4, messages.size());
        assertArrayEquals(YamahaExclusive.wave(5, YamahaExclusive.Format.ADPCM, 1, 4, 16000, adpcm), unpack(messages.get(0)));
        assertArrayEquals(YamahaExclusive.on(5, 127, YamahaExclusive.NO_CHANNEL), unpack(messages.get(1)));
        assertArrayEquals(YamahaExclusive.volume(0, 64), unpack(messages.get(2)));
        assertArrayEquals(YamahaExclusive.off(5), unpack(messages.get(3)));

        // outside of a capture the engine drops what it is told
        YamahaExclusive.engine.start(5);
    }

    /** a stream wave of a smaf file and the wave of a wave table voice are told apart */
    @Test
    void waveDataMessage() throws Exception {
        byte[] data = {0x12, (byte) 0x34};
        vavi.sound.smaf.vavi.message.MidiContext context = new vavi.sound.smaf.vavi.message.MidiContext();

        MidiEvent[] events = new WaveDataMessage(2, 1, data, 8000, 4, 1).getMidiEvents(context);
        assertEquals(1, events.length);
        assertArrayEquals(YamahaExclusive.wave(2, YamahaExclusive.Format.ADPCM, 1, 4, 8000, data), unpack(events[0].getMessage()));

        // "EXWV", 43 05 00 id <adpcm> f7
        events = new WaveDataMessage(2, 1, data, 8000, 4, 1).setWaveTable(true).getMidiEvents(context);
        assertArrayEquals(new byte[] {0x43, 0x05, 0x00, 2, 0x12, 0x34, (byte) 0xf7}, unpack(events[0].getMessage()));
    }

    @Test
    void enabled() {
        assertTrue(YamahaExclusive.isEnabled());
        System.clearProperty(DISABLED);
        assertFalse(YamahaExclusive.isEnabled());
    }
}
