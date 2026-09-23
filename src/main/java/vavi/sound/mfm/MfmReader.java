/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfm;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vavi.sound.mfm.MfmEvent.LongEvent;
import vavi.sound.mfm.MfmEvent.NoteEvent;
import vavi.sound.mfm.MfmEvent.PitchBendEvent;
import vavi.sound.mfm.MfmEvent.ShortEvent;

import static java.lang.System.getLogger;


/**
 * MFM (FueTrek MFMP, {@code mfmp}) reader.
 * <p>
 * Reverse engineered from rt_parser_std.dll of Faith Ring Tone Authoring Tool 1.5.0
 * (functions at 0x1000cb82, the track loop, and 0x1000cfda, the header),
 * checked with the output of rt_smf2mfmp.exe.
 * <pre>
 * "mfmp"
 * u32   length (following bytes)
 * u8    major type (1)
 * u8    minor type (0)
 * u8    0
 * u8    number of chunks after the header
 * u16   header length (sub chunks)
 * sub chunks ... tag (4 bytes), u16 length, data
 * chunks ... "ucs " / "wave" / "trac", u32 length, data
 * </pre>
 * Damaged data is read as far as it goes and flagged {@code truncated}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public final class MfmReader {

    private static final Logger logger = getLogger(MfmReader.class.getName());

    private MfmReader() {
    }

    /** @return true when the data starts with {@code mfmp} */
    public static boolean isMfm(byte[] data) {
        return data.length >= 4 && tag(data, 0).equals(Mfm.TYPE);
    }

    /** reads all of the stream. */
    public static Mfm read(InputStream is) throws IOException {
        return read(is.readAllBytes());
    }

    /** extra data lengths of the classes, the sub chunks {@code note}, {@code exta}, {@code extb}, {@code extc} */
    private record Extra(int note, int a, int b, int c) {
    }

    /** @throws InvalidMfmDataException when the data is not MFM */
    public static Mfm read(byte[] data) throws IOException {
        if (data.length < 14 || !isMfm(data)) {
            throw new InvalidMfmDataException("not mfmp");
        }
        int length = u32(data, 4);
        boolean truncated = Integer.toUnsignedLong(length) + 8 > data.length;
        int end = truncated ? data.length : length + 8;
        int majorType = data[8] & 0xff;
        int minorType = data[9] & 0xff;
        int chunksCount = data[11] & 0xff;
        int headerLength = u16(data, 12);

        List<Mfm.Chunk> subChunks = new ArrayList<>();
        int p = 14;
        int headerEnd = Math.min(14 + headerLength, end);
        while (p + 6 <= headerEnd) {
            String tag = tag(data, p);
            int l = u16(data, p + 4);
            if (p + 6 + l > headerEnd) {
                truncated = true;
                l = headerEnd - p - 6;
            }
            subChunks.add(new Mfm.Chunk(tag, Arrays.copyOfRange(data, p + 6, p + 6 + l)));
            p += 6 + l;
        }
        // rt_parser_std.dll takes these only when they are 2 bytes
        Extra extra = new Extra(extra(subChunks, "note"), extra(subChunks, "exta"), extra(subChunks, "extb"), extra(subChunks, "extc"));

        List<MfmTrack> tracks = new ArrayList<>();
        List<Mfm.Wave> waves = new ArrayList<>();
        List<byte[]> voices = new ArrayList<>();
        List<Mfm.Chunk> chunks = new ArrayList<>();
        p = 14 + headerLength;
        while (p + 8 <= end) {
            String tag = tag(data, p);
            long l = Integer.toUnsignedLong(u32(data, p + 4));
            int chunkEnd = (int) Math.min(p + 8 + l, end);
            if (p + 8 + l > end) {
                truncated = true;
            }
            switch (tag) {
            case "trac" -> tracks.add(readTrack(tracks.size(), data, p + 8, chunkEnd, extra));
            case "wave" -> {
                for (byte[] entry : entries(data, p + 8, chunkEnd)) {
                    if (entry.length >= 3) {
                        waves.add(new Mfm.Wave(entry[0] & 0xff, entry[1] & 0xff, entry[2] & 0xff, Arrays.copyOfRange(entry, 3, entry.length)));
                    } else {
                        truncated = true;
                    }
                }
            }
            case "ucs " -> voices.addAll(entries(data, p + 8, chunkEnd));
            default -> chunks.add(new Mfm.Chunk(tag, Arrays.copyOfRange(data, p + 8, chunkEnd)));
            }
            p = chunkEnd;
        }
        truncated |= tracks.stream().anyMatch(MfmTrack::truncated);

        return new Mfm(length, majorType, minorType, chunksCount, subChunks, tracks, waves, voices, chunks, truncated);
    }

    private static int extra(List<Mfm.Chunk> subChunks, String tag) {
        return subChunks.stream().filter(c -> c.tag().equals(tag) && c.data().length == 2)
                .mapToInt(c -> (int) c.value()).findFirst().orElse(0);
    }

    /** {@code u16 count, (u32 size, data) * count} */
    private static List<byte[]> entries(byte[] data, int p, int end) {
        List<byte[]> entries = new ArrayList<>();
        if (p + 2 > end) {
            return entries;
        }
        int count = u16(data, p);
        p += 2;
        for (int i = 0; i < count && p + 4 <= end; i++) {
            long size = Integer.toUnsignedLong(u32(data, p));
            int e = (int) Math.min(p + 4 + size, end);
            entries.add(Arrays.copyOfRange(data, p + 4, e));
            p = e;
        }
        if (entries.size() < count) {
            logger.log(Level.DEBUG, "entries: " + entries.size() + " < " + count);
        }
        return entries;
    }

    /** a track is [delta, event]... */
    private static MfmTrack readTrack(int number, byte[] data, int p, int end, Extra extra) {
        List<MfmEvent> events = new ArrayList<>();
        long tick = 0;
        boolean truncated = false;
        loop:
        while (p < end) {
            int delta = data[p++] & 0xff;
            tick += delta;
            if (p + 2 > end) {
                truncated = p < end;
                break;
            }
            int status = data[p] & 0xff;
            int b1 = data[p + 1] & 0xff;
            p += 2;
            if ((status & 0x3f) != 0x3f) {
                int velocity = -1;
                int base = 0;
                if (extra.note() > 0) {
                    if (p + extra.note() > end) {
                        truncated = true;
                        break;
                    }
                    int x = data[p] & 0xff;
                    velocity = x >> 2;
                    base = x & 3;
                    p += extra.note();
                }
                events.add(new NoteEvent(delta, tick, status >> 6, MfmEvent.key(base, status), b1, velocity));
                continue;
            }
            switch (status) {
            case 0x3f -> {
                if (p + 1 > end) {
                    truncated = true;
                    break loop;
                }
                events.add(new PitchBendEvent(delta, tick, b1 >> 6, ((b1 & 0x3f) << 8) | (data[p] & 0xff)));
                p++;
            }
            default -> {
                int l;
                if (b1 >= 0xf0) {
                    if (p + 2 > end) {
                        truncated = true;
                        break loop;
                    }
                    l = u16(data, p);
                    p += 2;
                    if (p + l > end) {
                        truncated = true;
                        break loop;
                    }
                    events.add(new LongEvent(delta, tick, status, b1, Arrays.copyOfRange(data, p, p + l)));
                    p += l;
                    continue;
                }
                l = b1 >= 0x80 ? 1 : 2 + switch (status) {
                    case 0x7f -> extra.a();
                    case 0xbf -> extra.b();
                    default -> extra.c();
                };
                if (p + l > end) {
                    truncated = true;
                    break loop;
                }
                ShortEvent event = new ShortEvent(delta, tick, status, b1, Arrays.copyOfRange(data, p, p + l));
                events.add(event);
                p += l;
                if (status == ShortEvent.CLASS_NORMAL && b1 == ShortEvent.TIME_SKIP) {
                    tick += (long) event.value() << 8;
                }
            }
            }
        }
        if (truncated) {
            logger.log(Level.DEBUG, "track " + number + " is truncated at " + p);
        }
        return new MfmTrack(number, events, truncated);
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
