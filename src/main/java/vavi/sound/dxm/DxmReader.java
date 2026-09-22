/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.dxm;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vavi.sound.dxm.DxmEvent.ChannelEvent;
import vavi.sound.dxm.DxmEvent.MetaEvent;
import vavi.sound.dxm.DxmEvent.SysexEvent;

import static java.lang.System.getLogger;


/**
 * DXM (Feelsound, {@code MCDF}) reader.
 * <p>
 * The sequence layout comes from the SMF reader in PsmPlay.exe (PsmPlayer 5.0),
 * which reads {@code CThd}/{@code CTrk} as {@code MThd}/{@code MTrk}
 * with one data byte for note off and pitch bend.
 * <pre>
 * "MCDF"
 * index ... u16 id, u32 offset (from the top of the file), u32 size, ..., 0xffff
 * entry data ...
 * id 0x0240: "CThd" u32 6, u16 format, u16 tracks, u16 division
 *            "CTrk" u32 length, events ...
 * </pre>
 * Damaged data is read as far as it goes and flagged {@code truncated}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public final class DxmReader {

    private static final Logger logger = getLogger(DxmReader.class.getName());

    private DxmReader() {
    }

    /** @return true when the data starts with {@code MCDF} */
    public static boolean isDxm(byte[] data) {
        return data.length >= 4 && tag(data, 0).equals(Dxm.TYPE);
    }

    /** reads all of the stream. */
    public static Dxm read(InputStream is) throws IOException {
        return read(is.readAllBytes());
    }

    /** @throws InvalidDxmDataException when the data is not DXM or has no sequence */
    public static Dxm read(byte[] data) throws IOException {
        if (!isDxm(data)) {
            throw new InvalidDxmDataException("not MCDF");
        }
        boolean truncated = false;
        List<Dxm.Entry> entries = new ArrayList<>();
        int p = 4;
        while (true) {
            if (p + 2 > data.length) {
                throw new InvalidDxmDataException("no end of index");
            }
            int id = u16(data, p);
            if (id == 0xffff) {
                break;
            }
            if (p + 10 > data.length) {
                throw new InvalidDxmDataException("no end of index");
            }
            int offset = u32(data, p + 2);
            int size = u32(data, p + 6);
            byte[] d;
            if (offset < 0 || size < 0 || offset > data.length) {
                truncated = true;
                d = new byte[0];
            } else {
                if (offset + (long) size > data.length) {
                    truncated = true;
                }
                d = Arrays.copyOfRange(data, offset, (int) Math.min(offset + (long) size, data.length));
            }
            entries.add(new Dxm.Entry(id, offset, size, d));
            p += 10;
        }

        Dxm.Entry sequence = entries.stream().filter(e -> e.id() == Dxm.ID_SEQUENCE).findFirst()
                .orElseThrow(() -> new InvalidDxmDataException("no sequence (0x0240)"));
        // PsmPlay does not use the size of the entry but chunk lengths, so do we
        p = sequence.offset();
        if (p + 14 > data.length || !tag(data, p).equals("CThd")) {
            throw new InvalidDxmDataException("no CThd");
        }
        int headerLength = u32(data, p + 4);
        int format = u16(data, p + 8);
        int tracksCount = u16(data, p + 10);
        int division = u16(data, p + 12);
        p += 8 + headerLength;

        List<DxmTrack> tracks = new ArrayList<>();
        while (tracks.size() < tracksCount && p + 8 <= data.length && tag(data, p).equals("CTrk")) {
            long l = Integer.toUnsignedLong(u32(data, p + 4));
            int end = (int) Math.min(p + 8 + l, data.length);
            DxmTrack track = readTrack(tracks.size(), data, p + 8, end);
            tracks.add(track);
            truncated |= track.truncated();
            p = end;
        }
        if (tracks.size() != tracksCount) {
            logger.log(Level.DEBUG, "tracks: " + tracks.size() + ", header says: " + tracksCount);
            truncated = true;
        }

        return new Dxm(entries, format, division, tracks, truncated);
    }

    /** a track is [delta (variable length), event]... */
    private static DxmTrack readTrack(int number, byte[] data, int p, int end) {
        List<DxmEvent> events = new ArrayList<>();
        long tick = 0;
        int runningStatus = 0;
        boolean endOfTrack = false;
        int[] q = {p};
        try {
            while (q[0] < end && !endOfTrack) {
                int delta = readVariable(data, q, end);
                tick += delta;
                int status = data[q[0]] & 0xff;
                if (status >= 0x80) {
                    q[0]++;
                    if (status < 0xf0) {
                        runningStatus = status;
                    }
                } else if (runningStatus != 0) {
                    status = runningStatus;
                } else {
                    logger.log(Level.DEBUG, "no running status at " + q[0]);
                    break;
                }
                switch (status) {
                case 0xff -> {
                    int type = data[q[0]++] & 0xff;
                    int l = readVariable(data, q, end);
                    events.add(new MetaEvent(delta, tick, type, bytes(data, q, l, end)));
                    endOfTrack = type == MetaEvent.END_OF_TRACK;
                }
                case 0xf0, 0xf7 -> {
                    int l = readVariable(data, q, end);
                    events.add(new SysexEvent(delta, tick, status, bytes(data, q, l, end)));
                }
                default -> {
                    if (status >= 0xf0) {
                        logger.log(Level.DEBUG, "unknown status: %02x at %d".formatted(status, q[0] - 1));
                        q[0] = end;
                        break;
                    }
                    int command = status & 0xf0;
                    boolean one = command == 0x80 || command == 0xc0 || command == 0xd0 || command == 0xe0;
                    int data1 = byteAt(data, q, end);
                    int data2 = one ? -1 : byteAt(data, q, end);
                    events.add(new ChannelEvent(delta, tick, status, data1, data2));
                }
                }
            }
        } catch (IndexOutOfBoundsException e) {
            logger.log(Level.DEBUG, "track " + number + " is truncated at " + q[0]);
        }
        return new DxmTrack(number, events, !endOfTrack);
    }

    private static int byteAt(byte[] data, int[] q, int end) {
        if (q[0] >= end) {
            throw new IndexOutOfBoundsException(q[0]);
        }
        return data[q[0]++] & 0xff;
    }

    private static byte[] bytes(byte[] data, int[] q, int l, int end) {
        if (q[0] + l > end) {
            throw new IndexOutOfBoundsException(q[0] + l);
        }
        byte[] b = Arrays.copyOfRange(data, q[0], q[0] + l);
        q[0] += l;
        return b;
    }

    private static int readVariable(byte[] data, int[] q, int end) {
        int v = 0;
        for (int i = 0; i < 4; i++) {
            int b = byteAt(data, q, end);
            v = (v << 7) | (b & 0x7f);
            if (b < 0x80) {
                break;
            }
        }
        return v;
    }

    private static String tag(byte[] data, int p) {
        return new String(data, p, 4, StandardCharsets.ISO_8859_1);
    }

    private static int u16(byte[] data, int p) {
        return ((data[p] & 0xff) << 8) | (data[p + 1] & 0xff);
    }

    private static int u32(byte[] data, int p) {
        return ((data[p] & 0xff) << 24) | ((data[p + 1] & 0xff) << 16) | ((data[p + 2] & 0xff) << 8) | (data[p + 3] & 0xff);
    }
}
