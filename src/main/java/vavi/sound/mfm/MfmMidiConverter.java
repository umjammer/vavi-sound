/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfm;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import vavi.sound.mfm.MfmEvent.NoteEvent;
import vavi.sound.mfm.MfmEvent.PitchBendEvent;
import vavi.sound.mfm.MfmEvent.ShortEvent;

import static java.lang.System.getLogger;


/**
 * Converts {@link Mfm} to a MIDI sequence (format 1, one MIDI track for each {@code trac}) as rt_parser_std.dll.
 * <ul>
 *  <li>resolution is {@link Mfm#timebase()}, ticks are kept</li>
 *  <li>channel is {@code track * 4 + voice}</li>
 *  <li>gate extensions ({@code 0xff 0x00 ~ 0x03}) are joined to the notes</li>
 *  <li>6 bit values are doubled</li>
 *  <li>program is {@code p + (bank bit 0) * 64}, bank select MSB is 0x79 (melody), 0x78 (rhythm), ... as the dll,
 *      channel 9 is rhythm until a bank is given</li>
 *  <li>audio (class 0x7f) is dropped, the waves are in {@link Mfm#waves()}</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public final class MfmMidiConverter {

    private static final Logger logger = getLogger(MfmMidiConverter.class.getName());

    private MfmMidiConverter() {
    }

    /** a note to be written, the off time is updated by extensions */
    private static final class Note {
        final int channel;
        final int key;
        final int velocity;
        final long on;
        long off;

        Note(int channel, int key, int velocity, long on, long off) {
            this.channel = channel;
            this.key = key;
            this.velocity = velocity;
            this.on = on;
            this.off = off;
        }
    }

    /** converts */
    public static Sequence toMidiSequence(Mfm mfm) throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, mfm.timebase());
        int[] bank1 = new int[16];
        int[] bank2 = new int[16];
        // rt_smf2mfmp.exe writes no rhythm bank for GM drums at channel 9, so it is rhythm by default (inferred)
        bank1[9] = 1;

        for (MfmTrack mfmTrack : mfm.tracks()) {
            Track track = sequence.createTrack();
            int number = mfmTrack.number();
            if (number == 0) {
                if (mfm.title().isPresent()) {
                    track.add(new MidiEvent(text(0x03, mfm.title().get()), 0));
                }
                if (mfm.copyright().isPresent()) {
                    track.add(new MidiEvent(text(0x02, mfm.copyright().get()), 0));
                }
            }
            List<Note> notes = new ArrayList<>();
            Map<Integer, Note> sounding = new HashMap<>();
            long last = 0;
            for (MfmEvent event : mfmTrack.events()) {
                long tick = event.tick();
                last = Math.max(last, tick);
                switch (event) {
                case NoteEvent n -> {
                    int channel = channel(number, n.voice());
                    if (n.key() > 127) {
                        continue;
                    }
                    Note note = new Note(channel, n.key(), n.midiVelocity(), tick, tick + n.gateTime());
                    notes.add(note);
                    sounding.put(channel << 8 | n.key(), note);
                }
                case PitchBendEvent b -> {
                    int value = Math.min(b.value(), 0x3fff);
                    track.add(new MidiEvent(new ShortMessage(ShortMessage.PITCH_BEND, channel(number, b.voice()), value & 0x7f, value >> 7), tick));
                }
                case ShortEvent s when s.isNoteExtension() -> {
                    int channel = channel(number, s.voice());
                    int key = MfmEvent.key(s.value() >> 6, s.value());
                    Note note = sounding.get(channel << 8 | key);
                    if (note != null && s.data().length > 1) {
                        note.off = tick + (s.data()[1] & 0xff);
                    } else {
                        logger.log(Level.DEBUG, "no note to extend: " + s);
                    }
                }
                case ShortEvent s when s.status() == ShortEvent.CLASS_NORMAL -> {
                    for (MidiMessage message : convert(s, number, mfm.timebase(), sequence.getResolution(), bank1, bank2)) {
                        track.add(new MidiEvent(message, tick));
                    }
                }
                default -> logger.log(Level.TRACE, "skip: " + event);
                }
            }
            for (Note note : notes) {
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, note.channel, note.key, note.velocity), note.on));
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, note.channel, note.key, 0), note.off));
                last = Math.max(last, note.off);
            }
            track.add(new MidiEvent(new MetaMessage(0x2f, new byte[0], 0), last));
        }
        return sequence;
    }

    private static int channel(int track, int voice) {
        return (track * 4 + voice) & 0x0f;
    }

    /** {@code 0xff} class commands */
    private static MidiMessage[] convert(ShortEvent s, int track, int timebase, int resolution, int[] bank1, int[] bank2)
            throws InvalidMidiDataException {

        int channel = channel(track, s.voice());
        int value = s.value6();
        return switch (s.command()) {
        case ShortEvent.TEMPO -> {
            long mpq = 60_000_000L * timebase / ((long) (s.value() + 20) * resolution);
            byte[] data = {(byte) (mpq >> 16), (byte) (mpq >> 8), (byte) mpq};
            yield new MidiMessage[] {new MetaMessage(0x51, data, 3)};
        }
        case ShortEvent.MASTER_VOLUME -> track != 0 ? new MidiMessage[0] : new MidiMessage[] {universal(0x01, s.value() >> 1)};
        case ShortEvent.MASTER_BALANCE -> track != 0 ? new MidiMessage[0] : new MidiMessage[] {universal(0x02, s.value() >> 1)};
        case ShortEvent.MASTER_COARSE_TUNING -> track != 0 ? new MidiMessage[0] : new MidiMessage[] {universal(0x04, Math.clamp(s.value() - 0x40, 0, 127))};
        case ShortEvent.BANK_MSB -> {
            bank1[channel] = value;
            yield new MidiMessage[0];
        }
        case ShortEvent.BANK_LSB -> {
            bank2[channel] = value;
            yield new MidiMessage[0];
        }
        case ShortEvent.PROGRAM_CHANGE -> {
            int program = value + ((bank2[channel] & 1) != 0 ? 0x40 : 0);
            yield new MidiMessage[] {
                    cc(channel, 0, bankSelect(bank1[channel], bank2[channel])), cc(channel, 32, 0),
                    new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0)};
        }
        case ShortEvent.VOLUME -> new MidiMessage[] {cc(channel, 7, value << 1)};
        case ShortEvent.PANPOT -> new MidiMessage[] {cc(channel, 10, value << 1)};
        case ShortEvent.MODULATION -> new MidiMessage[] {cc(channel, 1, value << 1)};
        case ShortEvent.PITCH_BEND_RANGE -> new MidiMessage[] {
                cc(channel, 101, 0), cc(channel, 100, 0), cc(channel, 6, value), cc(channel, 38, 0),
                cc(channel, 101, 127), cc(channel, 100, 127)};
        default -> {
            logger.log(Level.TRACE, "skip: " + s);
            yield new MidiMessage[0];
        }
        };
    }

    /** bank select MSB as rt_parser_std.dll (0x1000c94b) */
    static int bankSelect(int bank1, int bank2) {
        if (bank1 == 0 && (bank2 == 0 || bank2 == 1)) {
            return 0x79;
        } else if (bank1 == 1 && (bank2 == 0 || bank2 == 1)) {
            return 0x78;
        } else if (bank1 == 0 && bank2 == 0x34) {
            return 0x7d;
        } else if (bank1 == 1 && bank2 == 0x34) {
            return 0x14;
        } else if (bank1 == 0) {
            return bank2 == 0x36 ? 0x11 : 0x79;
        } else {
            return 0x78;
        }
    }

    private static ShortMessage cc(int channel, int number, int value) throws InvalidMidiDataException {
        return new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, number, Math.min(value, 127));
    }

    /** universal real time device control, 0x01: master volume, 0x02: balance, 0x04: coarse tuning (msb only) */
    private static SysexMessage universal(int id, int value) throws InvalidMidiDataException {
        byte[] data = {(byte) 0xf0, 0x7f, 0x7f, 0x04, (byte) id, 0x00, (byte) (value & 0x7f), (byte) 0xf7};
        return new SysexMessage(data, data.length);
    }

    private static MetaMessage text(int type, String text) throws InvalidMidiDataException {
        byte[] data = text.getBytes(Mfm.ENCODING);
        return new MetaMessage(type, data, data.length);
    }
}
