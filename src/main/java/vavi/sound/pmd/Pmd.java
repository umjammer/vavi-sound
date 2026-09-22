/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pmd;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;


/**
 * PMD (au CMX, {@code cmid}) file.
 *
 * @param length the length field of {@code cmid}, the file is {@code length + 8} bytes
 * @param majorType 2 is seen
 * @param contentsType bit 0: song, bit 1: wave, the same as the {@code cnts} sub chunk
 * @param tracksCount number of {@code trac} chunks declared in the header
 * @param subChunks header sub chunks in order of appearance
 * @param tracks {@code trac} chunks
 * @param chunks other chunks after the header
 * @param truncated true when the data is shorter than the header says
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public record Pmd(int length, int majorType, int contentsType, int tracksCount,
                  List<Chunk> subChunks, List<PmdTrack> tracks, List<Chunk> chunks, boolean truncated) {

    /** the file magic */
    public static final String TYPE = "cmid";

    /** text sub chunks are in shift_jis */
    static final Charset ENCODING = Charset.forName("MS932");

    /**
     * a header sub chunk ({@code tag, length (2 bytes), data})
     * or a chunk after the header ({@code tag, length (4 bytes), data}).
     */
    public record Chunk(String tag, byte[] data) {

        /** @return data as a shift_jis string */
        public String text() {
            return new String(data, ENCODING);
        }

        @Override
        public String toString() {
            return "Chunk[" + tag + ", " + data.length + "]";
        }
    }

    /** @return the first sub chunk with the tag */
    public Optional<Chunk> subChunk(String tag) {
        return subChunks.stream().filter(c -> c.tag().equals(tag)).findFirst();
    }

    /** @return {@code vers} e.g. "0500" */
    public Optional<String> version() {
        return subChunk("vers").map(Chunk::text);
    }

    /** @return {@code titl} */
    public Optional<String> title() {
        return subChunk("titl").map(Chunk::text);
    }

    /** @return {@code copy} */
    public Optional<String> copyright() {
        return subChunk("copy").map(Chunk::text);
    }

    /** @return {@code date} e.g. "20031224" */
    public Optional<String> date() {
        return subChunk("date").map(Chunk::text);
    }

    /** @return {@code tool} e.g. "3.1.297" */
    public Optional<String> tool() {
        return subChunk("tool").map(Chunk::text);
    }

    /** @return {@code cnts} e.g. "SONG", "SONG;WAVE" */
    public Optional<String> contents() {
        return subChunk("cnts").map(Chunk::text);
    }

    /**
     * @return true when note events are 3 bytes (with velocity and octave shift),
     *         the {@code note} sub chunk is not 0
     */
    public boolean isExtendedNote() {
        return subChunk("note").map(PmdReader::isNotZero).orElse(false);
    }
}
