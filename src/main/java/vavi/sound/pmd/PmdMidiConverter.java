/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pmd;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import vavi.sound.pmd.PmdEvent.ControlEvent;
import vavi.sound.pmd.PmdEvent.LongEvent;
import vavi.sound.pmd.PmdEvent.NoteEvent;

import static java.lang.System.getLogger;


/**
 * Converts {@link Pmd} to a MIDI sequence (format 1, one MIDI track for each {@code trac}).
 * <p>
 * The mapping follows PsmPlay.exe where it has one.
 * <ul>
 *  <li>resolution is the timebase of the first tempo event (48 when none), PMD ticks are kept as they are,
 *      a tempo event with another timebase is scaled into the tempo</li>
 *  <li>channel is {@code track * 4 + voice}, reassigned by {@link ControlEvent#CHANNEL_ASSIGN}</li>
 *  <li>0xe# values are doubled (6 bit to 7 bit)</li>
 *  <li>bank 0, 1: presets 0 ~ 5, bank 2: program, bank 3: program + 64, bank 63: drum kit</li>
 *  <li>the wide pitch bend of CMX is converted (PsmPlay drops it)</li>
 *  <li>wave events are dropped</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public final class PmdMidiConverter {

    private static final Logger logger = getLogger(PmdMidiConverter.class.getName());

    /** PsmPlay's default */
    static final int DEFAULT_TIMEBASE = 48;

    /** the bank of drum kits */
    static final int DRUM_BANK = 0x3f;

    /** GM2 rhythm bank MSB, used when a drum kit is not at channel 9 */
    static final int GM2_RHYTHM_BANK = 0x78;

    /** bank 0 and 1 have these (as PsmPlay) */
    private static final int[] PRESETS = {0, 9, 16, 24, 13, 74};

    private PmdMidiConverter() {
    }

    /** converts */
    public static Sequence toMidiSequence(Pmd pmd) throws InvalidMidiDataException {
        int resolution = pmd.tracks().stream().flatMap(t -> t.events().stream())
                .filter(e -> e instanceof ControlEvent c && c.isTempo() && c.timebase() > 0)
                .map(e -> ((ControlEvent) e).timebase()).findFirst().orElse(DEFAULT_TIMEBASE);
        Sequence sequence = new Sequence(Sequence.PPQ, resolution);

        // channel assign is kept over tracks as PsmPlay does
        int[] channels = new int[16];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = i;
        }
        int[] banks = new int[16];
        int[] programs = new int[16];
        boolean[] rhythms = new boolean[16];

        for (PmdTrack pmdTrack : pmd.tracks()) {
            Track track = sequence.createTrack();
            if (pmdTrack.number() == 0) {
                if (pmd.title().isPresent()) {
                    track.add(new MidiEvent(text(0x03, pmd.title().get()), 0));
                }
                if (pmd.copyright().isPresent()) {
                    track.add(new MidiEvent(text(0x02, pmd.copyright().get()), 0));
                }
            }
            long last = 0;
            for (PmdEvent event : pmdTrack.events()) {
                long tick = event.tick();
                switch (event) {
                case NoteEvent note -> {
                    int channel = channel(channels, pmdTrack.number(), note.voice());
                    int key = note.midiNote();
                    if (key < 0 || key > 127 || note.gateTime() == 0 || note.velocity() == 0) {
                        logger.log(Level.DEBUG, "skip note: " + note);
                        continue;
                    }
                    track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, channel, key, note.midiVelocity()), tick));
                    track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, channel, key, 0), tick + note.gateTime()));
                    last = Math.max(last, tick + note.gateTime());
                }
                case ControlEvent control -> {
                    if (control.status() == ControlEvent.END_OF_TRACK) {
                        last = Math.max(last, tick);
                        continue;
                    }
                    if (control.status() == ControlEvent.CHANNEL_ASSIGN) {
                        channels[(pmdTrack.number() * 4 + control.voice()) & 0x0f] = control.value() & 0x0f;
                        continue;
                    }
                    int channel = control.voice() < 0 ? -1 : channel(channels, pmdTrack.number(), control.voice());
                    for (MidiMessage message : convert(control, channel, resolution, banks, programs, rhythms)) {
                        track.add(new MidiEvent(message, tick));
                    }
                    last = Math.max(last, tick);
                }
                case LongEvent l -> logger.log(Level.DEBUG, "skip long event: " + l);
                }
            }
            MetaMessage eot = new MetaMessage(0x2f, new byte[0], 0);
            track.add(new MidiEvent(eot, last));
        }
        return sequence;
    }

    private static int channel(int[] channels, int track, int voice) {
        return channels[(track * 4 + voice) & 0x0f];
    }

    /** @param channel -1 for events without voice */
    private static MidiMessage[] convert(ControlEvent c, int channel, int resolution, int[] banks, int[] programs, boolean[] rhythms)
            throws InvalidMidiDataException {

        if (c.isWidePitchBend()) {
            int value = Math.min(c.value() * 2, 0x3fff);
            return new MidiMessage[] {new ShortMessage(ShortMessage.PITCH_BEND, channel, value & 0x7f, value >> 7)};
        }
        if (c.isTempo()) {
            int timebase = c.timebase();
            if (timebase == 0 || c.data() == 0) {
                return new MidiMessage[0];
            }
            // ticks are of the timebase, so scale when it differs from the resolution
            long mpq = 60_000_000L * timebase / ((long) c.data() * resolution);
            mpq = Math.min(mpq, 0xff_ffff);
            byte[] data = {(byte) (mpq >> 16), (byte) (mpq >> 8), (byte) mpq};
            return new MidiMessage[] {new MetaMessage(0x51, data, 3)};
        }
        int value = c.value();
        return switch (c.status()) {
        case ControlEvent.MASTER_VOLUME -> {
            byte[] data = {(byte) 0xf0, 0x7f, 0x7f, 0x04, 0x01, 0x00, (byte) (c.data() & 0x7f), (byte) 0xf7};
            yield new MidiMessage[] {new SysexMessage(data, data.length)};
        }
        case ControlEvent.CUE_POINT -> new MidiMessage[] {marker(c.data() == 0 ? "cue start" : "cue end")};
        case ControlEvent.LOOP_POINT -> new MidiMessage[] {marker("loop")};
        // PsmPlay makes a program change by both
        case ControlEvent.BANK_CHANGE -> {
            banks[channel] = value;
            yield programChange(channel, banks[channel], programs[channel], rhythms);
        }
        case ControlEvent.PROGRAM_CHANGE -> {
            programs[channel] = value;
            yield programChange(channel, banks[channel], value, rhythms);
        }
        case ControlEvent.VOLUME -> new MidiMessage[] {cc(channel, 7, value * 2)};
        case ControlEvent.PANPOT -> new MidiMessage[] {cc(channel, 10, value * 2)};
        case ControlEvent.EXPRESSION -> new MidiMessage[] {cc(channel, 11, value * 2)};
        case ControlEvent.MODULATION -> new MidiMessage[] {cc(channel, 1, value * 2)};
        case ControlEvent.PITCH_BEND -> new MidiMessage[] {new ShortMessage(ShortMessage.PITCH_BEND, channel, 0, value * 2)};
        case ControlEvent.PITCH_BEND_RANGE -> new MidiMessage[] {
                cc(channel, 101, 0), cc(channel, 100, 0), cc(channel, 6, value), cc(channel, 38, 0),
                cc(channel, 101, 127), cc(channel, 100, 127)};
        default -> {
            logger.log(Level.DEBUG, "skip control: " + c);
            yield new MidiMessage[0];
        }
        };
    }

    /** as PsmPlay: bank 0/1 presets, bank 2 +0, bank 3 +64, bank 63 drum */
    private static MidiMessage[] programChange(int channel, int bank, int program, boolean[] rhythms)
            throws InvalidMidiDataException {
        if (bank == DRUM_BANK) {
            if (channel == 9) {
                return new MidiMessage[] {new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0)};
            }
            logger.log(Level.DEBUG, "drum kit at channel " + channel);
            rhythms[channel] = true;
            return new MidiMessage[] {cc(channel, 0, GM2_RHYTHM_BANK), cc(channel, 32, 0),
                    new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0)};
        }
        int gm;
        if (bank < 2) {
            gm = program < PRESETS.length ? PRESETS[program] : 0;
        } else {
            gm = (program + ((bank & 1) << 6)) & 0x7f;
        }
        ShortMessage change = new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, gm, 0);
        if (rhythms[channel]) {
            rhythms[channel] = false;
            return new MidiMessage[] {cc(channel, 0, 0), cc(channel, 32, 0), change};
        }
        return new MidiMessage[] {change};
    }

    private static ShortMessage cc(int channel, int number, int value) throws InvalidMidiDataException {
        return new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, number, Math.min(value, 127));
    }

    private static MetaMessage marker(String text) throws InvalidMidiDataException {
        return text(0x06, text);
    }

    /** text meta events are in shift_jis as the source */
    private static MetaMessage text(int type, String text) throws InvalidMidiDataException {
        byte[] data = text.getBytes(Pmd.ENCODING);
        return new MetaMessage(type, data, data.length);
    }
}
