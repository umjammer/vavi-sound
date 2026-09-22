/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.dxm;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


/**
 * DXM (Feelsound, {@code MCDF}) file.
 * <p>
 * The ids are grouped by the upper byte, 0x00## is for the whole content,
 * 0x02## is for the sequence part. The meaning of an id is inferred from the corpus
 * unless it is marked "PsmPlay", PsmPlay.exe reads only the sequence.
 *
 * @param entries the index in order of appearance
 * @param format {@code CThd} format, 0 or 1
 * @param division {@code CThd} ticks per quarter note, 24 is seen
 * @param tracks {@code CTrk} chunks
 * @param truncated true when the data is shorter than the index or the tracks say
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public record Dxm(List<Entry> entries, int format, int division, List<DxmTrack> tracks, boolean truncated) {

    /** the file magic */
    public static final String TYPE = "MCDF";

    /** text entries are in shift_jis */
    static final Charset ENCODING = Charset.forName("MS932");

    /** format version, "01.0" */
    public static final int ID_VERSION = 0x0000;
    /** total file size, u32 */
    public static final int ID_CONTENT_SIZE = 0x0081;
    /** date, see {@link #date(int)} */
    public static final int ID_CONTENT_DATE = 0x0083;
    /** content version, "01.0" */
    public static final int ID_CONTENT_VERSION = 0x0084;
    /** content title, shift_jis */
    public static final int ID_CONTENT_TITLE = 0x00c0;
    /** sequence data, {@code CThd} + {@code CTrk}s (PsmPlay) */
    public static final int ID_SEQUENCE = 0x0240;
    /** play time [msec], u32 */
    public static final int ID_PLAY_TIME = 0x0280;
    /** size of {@link #ID_SEQUENCE}, u32 */
    public static final int ID_SEQUENCE_SIZE = 0x0281;
    /** sequence date, see {@link #date(int)} */
    public static final int ID_SEQUENCE_DATE = 0x0283;
    /** sequence version, "01.0" */
    public static final int ID_SEQUENCE_VERSION = 0x0284;
    /** title, shift_jis */
    public static final int ID_TITLE = 0x02c0;
    /** authoring tool, shift_jis, e.g. "PS-PLAYER V7.10" */
    public static final int ID_TOOL = 0x02c4;

    /**
     * An index entry, {@code u16 id, u32 offset, u32 size}. The index ends with id 0xffff.
     *
     * @param data may be shorter than size when the file is truncated
     */
    public record Entry(int id, int offset, int size, byte[] data) {

        /** @return data as a shift_jis string */
        public String text() {
            return new String(data, ENCODING);
        }

        /** @return data as big endian unsigned int */
        public long value() {
            long v = 0;
            for (byte b : data) {
                v = (v << 8) | (b & 0xff);
            }
            return v;
        }

        @Override
        public String toString() {
            return "Entry[0x%04x, offset=%d, size=%d]".formatted(id, offset, size);
        }
    }

    /** @return the entry with the id, entries of size 0 are ignored */
    public Optional<Entry> entry(int id) {
        return entries.stream().filter(e -> e.id() == id && e.data().length > 0).findFirst();
    }

    /** @return {@link #ID_TITLE}, or {@link #ID_CONTENT_TITLE} */
    public Optional<String> title() {
        return entry(ID_TITLE).or(() -> entry(ID_CONTENT_TITLE)).map(Entry::text);
    }

    /** @return {@link #ID_TOOL} */
    public Optional<String> tool() {
        return entry(ID_TOOL).map(Entry::text);
    }

    /** @return {@link #ID_PLAY_TIME} */
    public Optional<Long> playTime() {
        return entry(ID_PLAY_TIME).map(Entry::value);
    }

    /**
     * date entries are {@code u16 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u8 ?},
     * day and time may be 0.
     *
     * @param id {@link #ID_SEQUENCE_DATE} or {@link #ID_CONTENT_DATE}
     */
    public Optional<LocalDateTime> date(int id) {
        return entry(id).filter(e -> e.data().length >= 7).flatMap(e -> {
            byte[] d = e.data();
            try {
                return Optional.of(LocalDateTime.of(((d[0] & 0xff) << 8) | (d[1] & 0xff), d[2], Math.max(1, d[3]), d[4], d[5], d[6]));
            } catch (RuntimeException x) {
                return Optional.empty();
            }
        });
    }
}
