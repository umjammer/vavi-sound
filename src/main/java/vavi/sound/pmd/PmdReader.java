/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pmd;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vavi.sound.pmd.PmdEvent.ControlEvent;
import vavi.sound.pmd.PmdEvent.LongEvent;
import vavi.sound.pmd.PmdEvent.NoteEvent;

import static java.lang.System.getLogger;


/**
 * PMD (au CMX, {@code cmid}) reader.
 * <p>
 * The layout comes from the PMD reader in PsmPlay.exe (PsmPlayer 5.0),
 * which parses {@code cmid} with the same routine as MFi {@code melo}.
 * <pre>
 * "cmid"
 * u32   length (following bytes)
 * u16   header length (following 3 bytes + sub chunks)
 * u8    major type
 * u8    contents type
 * u8    number of tracks
 * sub chunks ... tag (4 bytes), u16 length, data
 * "trac" u32 length, events ...
 * </pre>
 * Damaged data is read as far as it goes and flagged {@code truncated}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public final class PmdReader {

    private static final Logger logger = getLogger(PmdReader.class.getName());

    private PmdReader() {
    }

    /** @return true when the data starts with {@code cmid} */
    public static boolean isPmd(byte[] data) {
        return data.length >= 4 && tag(data, 0).equals(Pmd.TYPE);
    }

    /** reads all of the stream. */
    public static Pmd read(InputStream is) throws IOException {
        return read(is.readAllBytes());
    }

    /** @throws InvalidPmdDataException when the data is not PMD */
    public static Pmd read(byte[] data) throws IOException {
        if (data.length < 13 || !isPmd(data)) {
            throw new InvalidPmdDataException("not cmid");
        }
        int length = u32(data, 4);
        boolean truncated = Integer.toUnsignedLong(length) + 8 > data.length;
        int end = truncated ? data.length : length + 8;
        int headerLength = u16(data, 8);
        int majorType = data[10] & 0xff;
        int contentsType = data[11] & 0xff;
        int tracksCount = data[12] & 0xff;

        List<Pmd.Chunk> subChunks = new ArrayList<>();
        int p = 13;
        int headerEnd = Math.min(10 + headerLength, end);
        while (p + 6 <= headerEnd) {
            String tag = tag(data, p);
            int l = u16(data, p + 4);
            if (p + 6 + l > headerEnd) {
                logger.log(Level.DEBUG, "sub chunk " + tag + " overruns the header: " + (p + 6 + l) + " > " + headerEnd);
                truncated = true;
                l = headerEnd - p - 6;
            }
            subChunks.add(new Pmd.Chunk(tag, Arrays.copyOfRange(data, p + 6, p + 6 + l)));
            p += 6 + l;
        }
        boolean extendedNote = subChunks.stream().anyMatch(c -> c.tag().equals("note") && isNotZero(c));

        List<PmdTrack> tracks = new ArrayList<>();
        List<Pmd.Chunk> chunks = new ArrayList<>();
        p = 10 + headerLength;
        while (p + 8 <= end) {
            String tag = tag(data, p);
            long l = Integer.toUnsignedLong(u32(data, p + 4));
            int chunkEnd = (int) Math.min(p + 8 + l, end);
            if (p + 8 + l > end) {
                truncated = true;
            }
            if (tag.equals("trac")) {
                tracks.add(readTrack(tracks.size(), data, p + 8, chunkEnd, extendedNote));
            } else {
                chunks.add(new Pmd.Chunk(tag, Arrays.copyOfRange(data, p + 8, chunkEnd)));
            }
            p = chunkEnd;
        }
        if (tracks.size() != tracksCount) {
            logger.log(Level.DEBUG, "tracks: " + tracks.size() + ", header says: " + tracksCount);
        }
        truncated |= tracks.stream().anyMatch(PmdTrack::truncated);

        return new Pmd(length, majorType, contentsType, tracksCount, subChunks, tracks, chunks, truncated);
    }

    /** a track is [delta, event]... */
    private static PmdTrack readTrack(int number, byte[] data, int p, int end, boolean extendedNote) {
        List<PmdEvent> events = new ArrayList<>();
        long tick = 0;
        boolean truncated = false;
        while (p < end) {
            int delta = data[p++] & 0xff;
            tick += delta;
            if (p >= end) {
                truncated = true;
                break;
            }
            int b0 = data[p] & 0xff;
            if (b0 != 0xff) {
                int size = extendedNote ? 3 : 2;
                if (p + size > end) {
                    truncated = true;
                    break;
                }
                int gate = data[p + 1] & 0xff;
                int velocity = -1;
                int octave = 0;
                if (extendedNote) {
                    int b2 = data[p + 2] & 0xff;
                    velocity = b2 >> 2;
                    octave = (b2 & 0x03) >= 2 ? (b2 & 0x03) - 4 : b2 & 0x03;
                }
                events.add(new NoteEvent(delta, tick, b0 >> 6, b0 & 0x3f, gate, velocity, octave));
                p += size;
            } else {
                if (p + 3 > end) {
                    truncated = true;
                    break;
                }
                int status = data[p + 1] & 0xff;
                if (status < 0xf0) {
                    events.add(new ControlEvent(delta, tick, status, data[p + 2] & 0xff));
                    p += 3;
                } else {
                    if (p + 4 > end) {
                        truncated = true;
                        break;
                    }
                    int l = u16(data, p + 2);
                    if (p + 4 + l > end) {
                        truncated = true;
                        break;
                    }
                    events.add(new LongEvent(delta, tick, status, Arrays.copyOfRange(data, p + 4, p + 4 + l)));
                    p += 4 + l;
                }
            }
        }
        if (truncated) {
            logger.log(Level.DEBUG, "track " + number + " is truncated at " + p);
        }
        return new PmdTrack(number, events, truncated);
    }

    /** the {@code note} sub chunk is 2 bytes, PsmPlay sees the second one */
    static boolean isNotZero(Pmd.Chunk c) {
        byte[] d = c.data();
        return d.length > 0 && d[d.length > 1 ? 1 : 0] != 0;
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
