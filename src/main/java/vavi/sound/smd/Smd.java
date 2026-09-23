/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smd;

import java.nio.charset.Charset;
import java.util.List;


/**
 * SMD (J-SKY melody, {@code .smd}, {@code .smz}) file.
 * <p>
 * A 7 bit text: a title in ISO-2022-JP, then parts {@code ESC $ D} ... {@code SI}.
 * An {@code .smz} may be SMAF ({@code MMMD}) instead, which is not this.
 *
 * @param title the title decoded, empty when none
 * @param parts parts in order of appearance
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public record Smd(String title, List<SmdPart> parts) {

    /** ticks per quarter note (PsmPlay) */
    public static final int RESOLUTION = 12;

    /** PsmPlay plays 16 parts at most */
    public static final int MAX_PARTS = 16;

    /** the title is made shift_jis by PsmPlay */
    static final Charset ENCODING = Charset.forName("MS932");

    /** @return true when any part is truncated */
    public boolean truncated() {
        return parts.stream().anyMatch(SmdPart::truncated);
    }
}
