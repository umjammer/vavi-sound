/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.dxm;

import java.util.HexFormat;


/**
 * An event in a DXM {@code CTrk} chunk.
 * <p>
 * {@code CTrk} is a SMF {@code MTrk} (variable length delta time, running status)
 * except that
 * <ul>
 *  <li>note off (0x8n) has the key only, no velocity</li>
 *  <li>pitch bend (0xen) has the MSB only</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public sealed interface DxmEvent permits DxmEvent.ChannelEvent, DxmEvent.MetaEvent, DxmEvent.SysexEvent {

    /** delta time from the previous event in the track */
    int delta();

    /** absolute time from the top of the track */
    long tick();

    /**
     * Channel message, 0x80 ~ 0xef.
     *
     * @param status status byte including the channel
     * @param data1 first data byte
     * @param data2 second data byte, -1 for the ones with one data byte
     *              (note off, program change, channel pressure and pitch bend)
     */
    record ChannelEvent(int delta, long tick, int status, int data1, int data2) implements DxmEvent {

        /** @return 0x80, 0x90, ... */
        public int command() {
            return status & 0xf0;
        }

        /** @return 0 ~ 15 */
        public int channel() {
            return status & 0x0f;
        }

        /** @return true when note on with velocity > 0 */
        public boolean isNoteOn() {
            return command() == 0x90 && data2 > 0;
        }

        /** @return true when note off, or note on with velocity 0 */
        public boolean isNoteOff() {
            return command() == 0x80 || (command() == 0x90 && data2 == 0);
        }

        /** @return 14 bit pitch bend value (0x2000 is center) when this is pitch bend, the LSB is 0 */
        public int pitchBend() {
            return command() == 0xe0 ? data1 << 7 : -1;
        }
    }

    /**
     * Meta event, {@code 0xff type length data}. The same as SMF.
     *
     * @param type 0x51: tempo, 0x2f: end of track, ...
     */
    record MetaEvent(int delta, long tick, int type, byte[] data) implements DxmEvent {

        /** tempo */
        public static final int TEMPO = 0x51;
        /** end of track */
        public static final int END_OF_TRACK = 0x2f;

        /** @return microseconds per quarter note when this is tempo, -1 for others */
        public int tempo() {
            if (type != TEMPO || data.length < 3) {
                return -1;
            }
            return ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
        }

        @Override
        public String toString() {
            return "MetaEvent[delta=%d, tick=%d, type=0x%02x, data=%s]".formatted(delta, tick, type, HexFormat.of().formatHex(data));
        }
    }

    /**
     * System exclusive, {@code 0xf0 length data} or {@code 0xf7 length data}. The same as SMF.
     *
     * @param status 0xf0 or 0xf7
     */
    record SysexEvent(int delta, long tick, int status, byte[] data) implements DxmEvent {

        @Override
        public String toString() {
            return "SysexEvent[delta=%d, tick=%d, status=0x%02x, data=%s]".formatted(delta, tick, status, HexFormat.of().formatHex(data));
        }
    }
}
