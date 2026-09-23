/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfm;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;


/**
 * MFM (FueTrek MFMP, {@code mfmp}) file.
 *
 * @param length the length field of {@code mfmp}, the file is {@code length + 8} bytes
 * @param majorType 1 is seen
 * @param minorType 0 is seen
 * @param chunksCount number of chunks after the header ({@code ucs }, {@code wave}, {@code trac}s)
 * @param subChunks header sub chunks in order of appearance
 * @param tracks {@code trac} chunks
 * @param waves entries of the {@code wave} chunk, the index is the wave number of audio play events
 * @param voices entries of the {@code ucs } chunk (FueTrek UCS synthesizer voices)
 * @param chunks other chunks after the header
 * @param truncated true when the data is shorter than the header says
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public record Mfm(int length, int majorType, int minorType, int chunksCount, List<Chunk> subChunks,
                  List<MfmTrack> tracks, List<Wave> waves, List<byte[]> voices, List<Chunk> chunks, boolean truncated) {

    /** the file magic */
    public static final String TYPE = "mfmp";

    /** default timebase (rt_parser_std.dll) */
    public static final int DEFAULT_TIMEBASE = 48;

    /** text sub chunks are in shift_jis */
    static final Charset ENCODING = Charset.forName("MS932");

    /**
     * a header sub chunk ({@code tag, length (2 bytes), data})
     * or a chunk after the header ({@code tag, length (4 bytes), data}).
     */
    public record Chunk(String tag, byte[] data) {

        /** @return data as a shift_jis string, a trailing NUL is removed */
        public String text() {
            int l = data.length;
            while (l > 0 && data[l - 1] == 0) l--;
            return new String(data, 0, l, ENCODING);
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
            return "Chunk[" + tag + ", " + data.length + "]";
        }
    }

    /**
     * An entry of the {@code wave} chunk, {@code u32 size, format, rate, ?, data}.
     * The header is interpreted by the player dll, the values below are from rt_smf2mfmp.exe output.
     *
     * @param format 0: ADPCM (ROHM original), 2: ADPCM (G.726), others are of other players
     * @param rate 4: 4000Hz, 5: 8000Hz
     * @param parameter not known
     * @param data the encoded audio, without the 3 bytes above
     */
    public record Wave(int format, int rate, int parameter, byte[] data) {

        /** @return sampling rate for format 0 and 2, -1 when unknown */
        public int sampleRate() {
            return (format == 0 || format == 2) && rate >= 4 && rate <= 7 ? 4000 << (rate - 4) : -1;
        }

        @Override
        public String toString() {
            return "Wave[format=%d, rate=%d, parameter=%d, length=%d]".formatted(format, rate, parameter, data.length);
        }
    }

    /** @return the first sub chunk with the tag */
    public Optional<Chunk> subChunk(String tag) {
        return subChunks.stream().filter(c -> c.tag().equals(tag)).findFirst();
    }

    /** @return {@code titl} */
    public Optional<String> title() {
        return subChunk("titl").map(Chunk::text);
    }

    /** @return {@code copy} */
    public Optional<String> copyright() {
        return subChunk("copy").map(Chunk::text);
    }

    /** @return {@code supt} e.g. "1100" */
    public Optional<String> support() {
        return subChunk("supt").map(Chunk::text);
    }

    /** @return {@code tmbs}, ticks per quarter note, {@link #DEFAULT_TIMEBASE} when none or 0 */
    public int timebase() {
        int t = (int) subChunk("tmbs").map(Chunk::value).orElse(0L).longValue();
        return t == 0 ? DEFAULT_TIMEBASE : t;
    }

    /** @return extra bytes of a note, the {@code note} sub chunk, 1: velocity byte exists, 0: compact mode */
    public int noteLength() {
        return (int) subChunk("note").map(Chunk::value).orElse(0L).longValue();
    }
}
