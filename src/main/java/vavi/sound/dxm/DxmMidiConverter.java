/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.dxm;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import vavi.sound.dxm.DxmEvent.ChannelEvent;
import vavi.sound.dxm.DxmEvent.MetaEvent;
import vavi.sound.dxm.DxmEvent.SysexEvent;


/**
 * Converts {@link Dxm} to a MIDI sequence.
 * <p>
 * {@code CTrk} is almost SMF, so the events are copied except
 * <ul>
 *  <li>note off gets velocity 0</li>
 *  <li>pitch bend gets LSB 0</li>
 *  <li>a title meta event is added when the sequence has none and {@link Dxm#title()} exists</li>
 *  <li>a truncated track gets end of track by {@link Track}</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public final class DxmMidiConverter {

    private DxmMidiConverter() {
    }

    /** converts */
    public static Sequence toMidiSequence(Dxm dxm) throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, dxm.division());
        for (DxmTrack dxmTrack : dxm.tracks()) {
            Track track = sequence.createTrack();
            if (dxmTrack.number() == 0 && dxm.title().isPresent()
                    && dxmTrack.events().stream().noneMatch(e -> e instanceof MetaEvent m && m.type() == 0x03)) {
                byte[] title = dxm.title().get().getBytes(Dxm.ENCODING);
                track.add(new MidiEvent(new MetaMessage(0x03, title, title.length), 0));
            }
            for (DxmEvent event : dxmTrack.events()) {
                MidiMessage message = switch (event) {
                case ChannelEvent c -> {
                    // a damaged file may have data bytes >= 0x80
                    int data1 = c.data1() & 0x7f;
                    int data2 = c.data2() < 0 ? 0 : c.data2() & 0x7f;
                    if (c.command() == ShortMessage.PITCH_BEND) {
                        data2 = data1;
                        data1 = 0;
                    }
                    yield new ShortMessage(c.status(), data1, data2);
                }
                case MetaEvent m when m.type() > 0x7f -> null;
                case MetaEvent m -> new MetaMessage(m.type(), m.data(), m.data().length);
                case SysexEvent s -> {
                    byte[] data = new byte[s.data().length + 1];
                    data[0] = (byte) s.status();
                    System.arraycopy(s.data(), 0, data, 1, s.data().length);
                    yield new SysexMessage(data, data.length);
                }
                };
                if (message != null) {
                    track.add(new MidiEvent(message, event.tick()));
                }
            }
        }
        return sequence;
    }
}
