/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smd;


/**
 * An event in a SMD part, with ties, triplets and octave already applied as PsmPlay does.
 * <p>
 * Time is in ticks of {@link Smd#RESOLUTION} per quarter note.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public sealed interface SmdEvent permits SmdEvent.NoteEvent, SmdEvent.RestEvent, SmdEvent.TempoEvent,
        SmdEvent.VolumeEvent, SmdEvent.CommandEvent {

    /** absolute time from the top of the part */
    long tick();

    /**
     * Note, a byte 0x27 ~ 0x7f.
     *
     * @param key MIDI note number, {@code ((c + 0x30) % 0x58) / 4 + 69 + octave * 12}
     * @param length in ticks, ties are joined
     */
    record NoteEvent(long tick, int key, int length) implements SmdEvent {
    }

    /**
     * Rest, a byte 0x23 ~ 0x26.
     *
     * @param length in ticks, ties are joined
     */
    record RestEvent(long tick, int length) implements SmdEvent {
    }

    /**
     * Tempo, {@code !(} ~ {@code !+}.
     *
     * @param bpm 150, 126, 108 or 96
     */
    record TempoEvent(long tick, int bpm) implements SmdEvent {
    }

    /**
     * Volume, {@code !,} ~ {@code !/} change the level by -2, -1, +2, +1.
     *
     * @param level -4 ~ 4 after the change, 0 is the default
     */
    record VolumeEvent(long tick, int level) implements SmdEvent {

        /** @return MIDI volume as PsmPlay, {@code level * 8 + 95} */
        public int midiVolume() {
            return level * 8 + 0x5f;
        }
    }

    /**
     * Other {@code !} commands.
     * <pre>
     * !"  octave up, !#  octave down, !$  octave reset
     * !%  the next note is played three times as a triplet
     * !&amp;  the next three notes are a triplet
     * </pre>
     *
     * @param command the byte after {@code !}
     */
    record CommandEvent(long tick, int command) implements SmdEvent {
    }
}
