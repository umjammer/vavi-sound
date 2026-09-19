/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.util.Arrays;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.SysexMessage;

import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mobile.MobileExclusive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


/**
 * AudioChannelVolumeMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
class AudioChannelVolumeMessageTest {

    /** the exclusive it is converted into goes back to it, which takes its own command (0x13) */
    @Test
    void sequencesItsOwnExclusive() throws Exception {
        AudioChannelVolumeMessage message = new AudioChannelVolumeMessage().init(0, 0x7f, 0x80, 0x40 | 0x20);
        MidiEvent[] events = message.getMidiEvents(new MidiContext());
        SysexMessage sysex = assertInstanceOf(SysexMessage.class, events[0].getMessage());
        byte[] data = MobileExclusive.unpack(sysex.getData());
        AudioDataSequencer sequencer = assertInstanceOf(AudioChannelVolumeMessage.class, AudioDataSequencer.factory(data));
        assertDoesNotThrow(() -> sequencer.sequence(Arrays.copyOfRange(data, 2, data.length), null));
    }
}
