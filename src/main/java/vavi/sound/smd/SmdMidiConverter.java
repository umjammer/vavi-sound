/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smd;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import vavi.sound.smd.SmdEvent.NoteEvent;
import vavi.sound.smd.SmdEvent.TempoEvent;
import vavi.sound.smd.SmdEvent.VolumeEvent;


/**
 * Converts {@link Smd} to a MIDI sequence (format 1) as PsmPlay does.
 * <ul>
 *  <li>resolution is {@link Smd#RESOLUTION}</li>
 *  <li>part n is played at channel n with the default program, velocity 127</li>
 *  <li>tempo of the first part only</li>
 *  <li>volume is {@code level * 8 + 95}</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public final class SmdMidiConverter {

    private SmdMidiConverter() {
    }

    /** converts */
    public static Sequence toMidiSequence(Smd smd) throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, Smd.RESOLUTION);
        for (SmdPart part : smd.parts()) {
            Track track = sequence.createTrack();
            int channel = part.number();
            if (part.number() == 0 && !smd.title().isEmpty()) {
                byte[] title = smd.title().getBytes(Smd.ENCODING);
                track.add(new MidiEvent(new MetaMessage(0x03, title, title.length), 0));
            }
            for (SmdEvent event : part.events()) {
                switch (event) {
                case NoteEvent n -> {
                    if (n.key() < 0 || n.key() > 127 || n.length() == 0) {
                        continue;
                    }
                    track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, channel, n.key(), 127), n.tick()));
                    track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, channel, n.key(), 0), n.tick() + n.length()));
                }
                case TempoEvent t when part.number() == 0 -> {
                    int mpq = 60_000_000 / t.bpm();
                    byte[] data = {(byte) (mpq >> 16), (byte) (mpq >> 8), (byte) mpq};
                    track.add(new MidiEvent(new MetaMessage(0x51, data, 3), t.tick()));
                }
                case VolumeEvent v ->
                    track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 7, v.midiVolume()), v.tick()));
                default -> {
                }
                }
            }
            track.add(new MidiEvent(new MetaMessage(0x2f, new byte[0], 0), part.length()));
        }
        return sequence;
    }
}
