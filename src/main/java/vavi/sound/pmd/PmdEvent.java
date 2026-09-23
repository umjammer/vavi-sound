/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pmd;

import java.util.HexFormat;


/**
 * An event in a PMD {@code trac} chunk.
 * <p>
 * Every event is preceded by a one byte delta time.
 * The encoding is the one of MFi (i-mode {@code melo}) tracks.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public sealed interface PmdEvent permits PmdEvent.NoteEvent, PmdEvent.ControlEvent, PmdEvent.LongEvent {

    /** delta time from the previous event in the track, 0 ~ 255 */
    int delta();

    /** absolute time from the top of the track */
    long tick();

    /**
     * Note.
     * <pre>
     * byte 0: vv kkkkkk ... v: voice (channel in the track), k: key
     * byte 1: gate time
     * byte 2: VVVVVV oo ... V: velocity, o: octave shift (only when "note" sub chunk is not 0)
     * </pre>
     *
     * @param voice 0 ~ 3, the channel is {@code track * 4 + voice} until {@link ControlEvent#CHANNEL_ASSIGN}
     * @param key 0 ~ 63, 0 is A2 (MIDI 45)
     * @param gateTime note length in ticks
     * @param velocity 0 ~ 63, -1 when the file uses 2 byte notes
     * @param octaveShift -2 ~ 1, 0 when the file uses 2 byte notes
     */
    record NoteEvent(int delta, long tick, int voice, int key, int gateTime, int velocity, int octaveShift) implements PmdEvent {

        /** @return MIDI note number */
        public int midiNote() {
            return key + 45 + octaveShift * 12;
        }

        /** @return MIDI velocity, 127 when the file has no velocity */
        public int midiVelocity() {
            return velocity < 0 ? 127 : velocity * 2;
        }
    }

    /**
     * Short control event, {@code 0xff, status, data}. status is 0x00 ~ 0xef.
     *
     * @param status second byte
     * @param data third byte
     */
    record ControlEvent(int delta, long tick, int status, int data) implements PmdEvent {

        /** master volume, data: 0 ~ 127 */
        public static final int MASTER_VOLUME = 0xb0;
        /** channel configuration, data: channel {@code >> 3}, flag {@code & 1} (as PsmPlay reads it) */
        public static final int CHANNEL_CONFIGURATION = 0xba;
        /** tempo 0xc0 ~ 0xcf, lower nibble is the timebase, data is the tempo [bpm] */
        public static final int TEMPO = 0xc0;
        /** cue point, data: 0 start, 1 end */
        public static final int CUE_POINT = 0xd0;
        /** nop2 */
        public static final int NOP2 = 0xdc;
        /** loop point */
        public static final int LOOP_POINT = 0xdd;
        /** nop, used as a long rest */
        public static final int NOP = 0xde;
        /** end of track */
        public static final int END_OF_TRACK = 0xdf;
        /** program (tone number) change, data: vv pppppp */
        public static final int PROGRAM_CHANGE = 0xe0;
        /** tone bank change, data: vv bbbbbb */
        public static final int BANK_CHANGE = 0xe1;
        /** volume, data: vv VVVVVV */
        public static final int VOLUME = 0xe2;
        /** pan pot, data: vv pppppp, 32 is center */
        public static final int PANPOT = 0xe3;
        /** pitch bend, data: vv pppppp, 32 is center */
        public static final int PITCH_BEND = 0xe4;
        /** channel assignment, data: vv 00cccc, voice v of the track is played at channel c */
        public static final int CHANNEL_ASSIGN = 0xe5;
        /** expression (relative volume), data: vv eeeeee */
        public static final int EXPRESSION = 0xe6;
        /** pitch bend range, data: vv rrrrrr */
        public static final int PITCH_BEND_RANGE = 0xe7;
        /** fine pitch bend A */
        public static final int FINE_PITCH_BEND_A = 0xe8;
        /** fine pitch bend B */
        public static final int FINE_PITCH_BEND_B = 0xe9;
        /** modulation depth, data: vv mmmmmm */
        public static final int MODULATION = 0xea;

        /**
         * @return true when this is the 13 bit pitch bend of CMX, status 0x00 ~ 0x7f.
         * PsmPlay skips it (that is why its MIDI output has no bends),
         * the layout is inferred from the corpus: status {@code 0vvhhhhh}, data {@code llllllll},
         * value {@code hhhhh llllllll}, center 0x1000.
         */
        public boolean isWidePitchBend() {
            return status < 0x80;
        }

        /** @return true when this is a tempo event */
        public boolean isTempo() {
            return (status & 0xf0) == TEMPO;
        }

        /** @return timebase (ticks per quarter note) of a tempo event, 0 for reserved */
        public int timebase() {
            if (!isTempo() || (status & 0x07) == 0x07) {
                return 0;
            }
            return 3 * (1 << (status & 0x07)) * ((status & 0x08) != 0 ? 5 : 2);
        }

        /**
         * @return voice (channel in the track) for 0xe0 ~ 0xea and the wide pitch bend, -1 for others
         */
        public int voice() {
            if (isWidePitchBend()) {
                return (status >> 5) & 0x03;
            } else if (status >= PROGRAM_CHANGE && status <= MODULATION) {
                return data >> 6;
            } else {
                return -1;
            }
        }

        /** @return the value without voice bits */
        public int value() {
            if (isWidePitchBend()) {
                return ((status & 0x1f) << 8) | data;
            } else if (status >= PROGRAM_CHANGE && status <= MODULATION) {
                return data & 0x3f;
            } else {
                return data;
            }
        }
    }

    /**
     * Long event, {@code 0xff, status, length (2 bytes), data}. status is 0xf0 ~ 0xff.
     *
     * @param status second byte
     * @param data without the length
     */
    record LongEvent(int delta, long tick, int status, byte[] data) implements PmdEvent {

        /**
         * embedded wave data of "SONG;WAVE" contents.
         * the body is not known yet, PsmPlay does not play it.
         * <pre>
         * 0: wave number?
         * 1: format? (0x44, 0x45, 0x84, 0x85 are seen)
         * </pre>
         */
        public static final int WAVE = 0xf1;

        @Override
        public String toString() {
            return "LongEvent[delta=%d, tick=%d, status=0x%02x, length=%d, data=%s%s]".formatted(
                    delta, tick, status, data.length,
                    HexFormat.of().formatHex(data, 0, Math.min(16, data.length)), data.length > 16 ? "..." : "");
        }
    }
}
