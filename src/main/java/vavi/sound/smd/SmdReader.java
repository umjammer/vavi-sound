/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import vavi.sound.smd.SmdEvent.CommandEvent;
import vavi.sound.smd.SmdEvent.NoteEvent;
import vavi.sound.smd.SmdEvent.RestEvent;
import vavi.sound.smd.SmdEvent.TempoEvent;
import vavi.sound.smd.SmdEvent.VolumeEvent;

import static java.lang.System.getLogger;


/**
 * SMD (J-SKY melody) reader.
 * <p>
 * Reverse engineered from the SMD reader in PsmPlay.exe (PsmPlayer 5.0).
 * <pre>
 * title ... ISO-2022-JP ({@code ESC ( B}, {@code ESC $ B}, {@code SO}/{@code SI} half width kana, others as is)
 * parts ... {@code ESC $ D} data {@code SI}, up to 16
 *
 * data
 *  0x23 ~ 0x26  rest of 16th, 8th, quarter, half
 *  0x27 ~ 0x7f  note, key {@code ((c + 0x30) % 0x58) / 4} from A4, length {@code c & 3}: 16th, 8th, quarter, half
 *  0x22         tie, joins the next rest to a rest, or the next note of the same key to a note
 *  0x21 c       command
 *               !" octave +1, !# octave -1, !$ octave 0
 *               !% the next note three times as a triplet, !&amp; the next three notes as a triplet
 *               !( !) !* !+ tempo 150, 126, 108, 96 (PsmPlay uses the first part's only)
 *               !, !- !. !/ volume -2, -1, +2, +1 (clamped to -4 ~ 4)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public final class SmdReader {

    private static final Logger logger = getLogger(SmdReader.class.getName());

    /** 16th, 8th, quarter, half in {@link Smd#RESOLUTION} */
    private static final int[] LENGTHS = {3, 6, 12, 24};

    /** tempo commands {@code !(} ~ {@code !+} */
    private static final int[] TEMPOS = {150, 126, 108, 96};

    private static final int ESC = 0x1b;
    private static final int SO = 0x0e;
    private static final int SI = 0x0f;

    private SmdReader() {
    }

    /** @return index of {@code ESC $ D} from p, -1 when none */
    static int indexOfPart(byte[] data, int p) {
        for (int i = p; i + 2 < data.length; i++) {
            if (data[i] == ESC && data[i + 1] == '$' && data[i + 2] == 'D') {
                return i;
            }
        }
        return -1;
    }

    /** @return true when the data is not SMAF and has a part */
    public static boolean isSmd(byte[] data) {
        boolean smaf = data.length >= 4 && data[0] == 'M' && data[1] == 'M' && data[2] == 'M' && data[3] == 'D';
        return !smaf && indexOfPart(data, 0) >= 0;
    }

    /** reads all of the stream. */
    public static Smd read(InputStream is) throws IOException {
        return read(is.readAllBytes());
    }

    /** @throws InvalidSmdDataException when the data has no part */
    public static Smd read(byte[] data) throws IOException {
        if (!isSmd(data)) {
            throw new InvalidSmdDataException("no ESC $ D");
        }
        int p = indexOfPart(data, 0);
        String title = readTitle(data, p);

        List<SmdPart> parts = new ArrayList<>();
        while (parts.size() < Smd.MAX_PARTS) {
            int j = indexOfPart(data, p);
            if (j < 0) {
                break;
            }
            int[] q = {j + 3};
            parts.add(readPart(parts.size(), data, q));
            p = q[0];
        }
        return new Smd(title, parts);
    }

    /** as PsmPlay, JIS X 0208 is made into shift_jis, up to 255 bytes */
    private static String readTitle(byte[] data, int end) {
        ByteArrayOutputStream sjis = new ByteArrayOutputStream();
        int mode = 0; // 0: as is, 1: JIS X 0208, 2: half width kana
        for (int p = 0; p < end; p++) {
            int c = data[p] & 0xff;
            if (sjis.size() >= 255) {
                continue;
            }
            if (c == ESC && p + 2 < end && data[p + 1] == '(' && data[p + 2] == 'B') {
                mode = 0;
                p += 2;
            } else if (c == ESC && p + 2 < end && data[p + 1] == '$' && data[p + 2] == 'B') {
                mode = 1;
                p += 2;
            } else if (c == SO) {
                mode = 2;
            } else if (c == SI) {
                mode = 0;
            } else if (mode == 1) {
                if (p + 1 >= end) {
                    break;
                }
                int c2 = data[p + 1] & 0xff;
                sjis.write(c >= 0x21 && c <= 0x5e ? ((c - 0x21) >> 1) + 0x81 : ((c - 0x5f) >> 1) + 0xe0);
                if ((c & 1) != 0) {
                    sjis.write(c2 <= 0x5f ? c2 + 0x1f : c2 + 0x20);
                } else {
                    sjis.write(c2 + 0x7e);
                }
                p++;
            } else if (mode == 2) {
                sjis.write(c + 0x80);
            } else {
                sjis.write(c);
            }
        }
        return sjis.toString(Smd.ENCODING).trim();
    }

    private static boolean isRest(int c) {
        return c >= 0x23 && c <= 0x26;
    }

    private static boolean isNote(int c) {
        return c >= 0x27 && c <= 0x7f;
    }

    /** 0 ~ 21 */
    private static int pitch(int c) {
        return ((c + 0x30) % 0x58) / 4;
    }

    /** @param q in: after {@code ESC $ D}, out: after the part */
    private static SmdPart readPart(int number, byte[] data, int[] q) {
        List<SmdEvent> events = new ArrayList<>();
        long tick = 0;
        int octave = 0;
        int volume = 0;
        int triplet = 0;
        int p = q[0];
        while (p < data.length && data[p] != SI) {
            int c = data[p] & 0xff;
            if (isRest(c) || isNote(c)) {
                boolean rest = isRest(c);
                int total = 0;
                int off = 0;
                while (true) {
                    int x = data[p + off] & 0xff;
                    int length = rest ? LENGTHS[x - 0x23] : LENGTHS[x & 3];
                    if (triplet > 0) {
                        length = length * 2 / 3;
                        if (total == 0) {
                            triplet--;
                        }
                    }
                    total += length;
                    if (p + off + 2 < data.length && data[p + off + 1] == '"') {
                        int next = data[p + off + 2] & 0xff;
                        // PsmPlay compares the key only for notes
                        if (rest ? isRest(next) : pitch(next) == pitch(x)) {
                            off += 2;
                            continue;
                        }
                    }
                    break;
                }
                if (rest) {
                    events.add(new RestEvent(tick, total));
                } else {
                    events.add(new NoteEvent(tick, pitch(c) + octave * 12 + 69, total));
                }
                tick += total;
                // "!%" repeats the same note
                if (((triplet >> 2) & 1) != 0) {
                    if ((triplet & 3) > 0) {
                        continue;
                    }
                    triplet = 0;
                }
                p += off + 1;
            } else if (c == '!' && p + 1 < data.length) {
                int x = data[p + 1] & 0xff;
                switch (x) {
                case '"' -> octave = 1;
                case '#' -> octave = -1;
                case '$' -> octave = 0;
                case '%' -> triplet = 7;
                case '&' -> triplet = 3;
                case '(', ')', '*', '+' -> events.add(new TempoEvent(tick, TEMPOS[x - '(']));
                case ',', '-', '.', '/' -> {
                    volume += switch (x) {
                        case ',' -> -2;
                        case '-' -> -1;
                        case '.' -> 2;
                        default -> 1;
                    };
                    volume = Math.clamp(volume, -4, 4);
                    events.add(new VolumeEvent(tick, volume));
                }
                default -> logger.log(Level.DEBUG, "unknown command: %02x at %d".formatted(x, p));
                }
                if (x == '"' || x == '#' || x == '$' || x == '%' || x == '&') {
                    events.add(new CommandEvent(tick, x));
                }
                p += 2;
            } else {
                p++;
            }
        }
        boolean truncated = p >= data.length;
        q[0] = truncated ? p : p + 1;
        return new SmdPart(number, events, truncated);
    }
}
