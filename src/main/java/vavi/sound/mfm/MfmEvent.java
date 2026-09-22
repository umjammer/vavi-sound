/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfm;

import java.util.HexFormat;


/**
 * An event in a MFM {@code trac} chunk.
 * <p>
 * A track is {@code [delta (1 byte), status, data, ...]...}. When the lower 6 bits of the status are not
 * 0x3f it is a note, otherwise the upper 2 bits select a class (0x3f, 0x7f, 0xbf, 0xff) and the next byte
 * is a command.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public sealed interface MfmEvent permits MfmEvent.NoteEvent, MfmEvent.PitchBendEvent, MfmEvent.ShortEvent, MfmEvent.LongEvent {

    /** delta time from the previous event in the track, 0 ~ 255 */
    int delta();

    /** absolute time from the top of the track, {@link ShortEvent#TIME_SKIP} is included */
    long tick();

    /** key bases selected by 2 bits of the velocity byte (as rt_parser_std.dll) */
    int[] KEY_BASES = {45, 65, 0, 0};

    /**
     * @param base 2 bits, the lower bits of the velocity byte of a note, or the upper bits of an extension's key byte
     * @param key lower 6 bits are used
     * @return MIDI key
     */
    static int key(int base, int key) {
        return (key & 0x3f) + KEY_BASES[base & 3];
    }

    /**
     * Note, {@code vvkkkkkk gate [VVVVVVbb]}.
     *
     * @param voice 0 ~ 3, the channel is {@code track * 4 + voice}
     * @param key MIDI key, {@code k + (45, 65, 0, 0)[b]}
     * @param gateTime note length, extended by {@link ShortEvent#isNoteExtension()}
     * @param velocity 0 ~ 63, -1 for the compact mode (2 byte notes)
     */
    record NoteEvent(int delta, long tick, int voice, int key, int gateTime, int velocity) implements MfmEvent {

        /** @return MIDI velocity, 126 for the compact mode as rt_parser_std.dll */
        public int midiVelocity() {
            return velocity < 0 ? 126 : velocity * 2;
        }
    }

    /**
     * Pitch bend, class 0x3f: {@code 0x3f vvhhhhhh llllllll}.
     *
     * @param value 14 bit, 0x2000 is center
     */
    record PitchBendEvent(int delta, long tick, int voice, int value) implements MfmEvent {
    }

    /**
     * A command with fixed length data, class 0x7f, 0xbf or 0xff.
     * <ul>
     *  <li>command 0x00 ~ 0x7f: 2 bytes + the extra length of the class ({@code exta}, {@code extb}, {@code extc})</li>
     *  <li>command 0x80 ~ 0xef: 1 byte</li>
     * </ul>
     *
     * @param status 0x7f, 0xbf or 0xff
     * @param command the byte after the status
     */
    record ShortEvent(int delta, long tick, int status, int command, byte[] data) implements MfmEvent {

        /** class of audio */
        public static final int CLASS_AUDIO = 0x7f;
        /** class not used */
        public static final int CLASS_C = 0xbf;
        /** class of normal commands */
        public static final int CLASS_NORMAL = 0xff;

        // 0xff

        /** time skip, time += data * 256 */
        public static final int TIME_SKIP = 0xb0;
        /** end of track */
        public static final int END_OF_TRACK = 0xb1;
        /** tempo, bpm = data + 20 */
        public static final int TEMPO = 0xbf;
        /** master volume, data / 2 (track 0 only) */
        public static final int MASTER_VOLUME = 0xc0;
        /** master balance, data / 2 (track 0 only) */
        public static final int MASTER_BALANCE = 0xc1;
        /** master coarse tuning, data - 0x40 (track 0 only) */
        public static final int MASTER_COARSE_TUNING = 0xc2;
        /** bank select 1, vv 00000r, r: rhythm */
        public static final int BANK_MSB = 0xd0;
        /** bank select 2, vv bbbbbb, bit 0 is the upper bit of the program */
        public static final int BANK_LSB = 0xd1;
        /** program change, vv pppppp */
        public static final int PROGRAM_CHANGE = 0xd2;
        /** volume, vv VVVVVV (expression is folded into this by the converter) */
        public static final int VOLUME = 0xd3;
        /** pan pot, vv pppppp */
        public static final int PANPOT = 0xd4;
        /** pitch bend range, vv rrrrrr */
        public static final int PITCH_BEND_RANGE = 0xd5;
        /** modulation, vv mmmmmm */
        public static final int MODULATION = 0xd6;

        // 0x7f

        /** audio play, data: vv VVVVVV, wave index */
        public static final int AUDIO_PLAY = 0x00;
        /** audio volume, vv VVVVVV */
        public static final int AUDIO_VOLUME = 0xd0;
        /** audio pan pot, vv pppppp */
        public static final int AUDIO_PANPOT = 0xd1;

        /** @return true when {@code 0xff 0x00 ~ 0x03}, gate extension of a note: voice = command, data: key, gate */
        public boolean isNoteExtension() {
            return status == CLASS_NORMAL && command < 4;
        }

        /** @return the first data byte */
        public int value() {
            return data.length > 0 ? data[0] & 0xff : 0;
        }

        /** @return voice of {@code vv xxxxxx} data */
        public int voice() {
            return isNoteExtension() ? command : value() >> 6;
        }

        /** @return lower 6 bits of the first data byte */
        public int value6() {
            return value() & 0x3f;
        }

        @Override
        public String toString() {
            return "ShortEvent[delta=%d, tick=%d, status=0x%02x, command=0x%02x, data=%s]".formatted(
                    delta, tick, status, command, HexFormat.of().formatHex(data));
        }
    }

    /**
     * A command with u16 length data, command 0xf0 ~ 0xff of class 0x7f, 0xbf or 0xff.
     *
     * @param status 0x7f, 0xbf or 0xff
     * @param command 0xf0 ~ 0xff
     */
    record LongEvent(int delta, long tick, int status, int command, byte[] data) implements MfmEvent {

        /** 0x7f 0xf0 with 4 bytes: loads the UCS voice of the u32 index */
        public static final int UCS_LOAD = 0xf0;

        @Override
        public String toString() {
            return "LongEvent[delta=%d, tick=%d, status=0x%02x, command=0x%02x, length=%d, data=%s%s]".formatted(
                    delta, tick, status, command, data.length,
                    HexFormat.of().formatHex(data, 0, Math.min(16, data.length)), data.length > 16 ? "..." : "");
        }
    }
}
