/*
 * https://github.com/wackypack/mtex/blob/master/tonesniffer.py
 */

import java.util.ArrayList;
import java.util.List;


/**
 * ToneSniffer
 * <p>
 * <pre>
 * Currently supports:
 *
 * - DLS: Downloadable Sounds
 * - DXM: Feelsound DXM files
 * - IMY: iMelody ringtones
 * - MFM: Faith MFM files
 * - MID: MIDI files
 * - MLD: MLD ringtones
 * - MMF: SMAF ringtones
 * - NRT: Nokia ringtones
 * - PMD: AU-PMD ringtones
 * - QCP: Qualcomm audio container
 * - RMI: RIFF MIDI
 * - SF2: SoundFont banks
 * - WAV: RIFF Wave
 * </pre>
 * @version 2.1
 */
public final class ToneSniffer {

    private ToneSniffer() {
    }

    public record Hit(long offset, int size, String extension) {}

    public static List<Hit> scan(
            byte[] bin,
            boolean findMmf,
            boolean findImy,
            boolean findMld,
            boolean findNrt) {

        List<Hit> hits = new ArrayList<>();
        int size = bin.length;

        for (int x = 0; x < size; x++) {
            if (!isCandidate(bin, x)) {
                continue;
            }

            int chunkSize = 0;

            //
            // MIDI
            //
            if (match(bin, x, "MThd") && match(bin, x + 14, "MTrk")) {
                int mtrkSize = readIntBE(bin, x + 18);
                chunkSize = mtrkSize + 22;
                boolean test = false;

                while (!test) {
                    if (match(bin, x + chunkSize, "MTrk")) {
                        mtrkSize = readIntBE(bin, x + chunkSize + 4);
                        chunkSize += mtrkSize + 8;
                    }

                    if (!match(bin, x + chunkSize, "MTrk")) {
                        test = true;
                    }
                }

                hits.add(new Hit(x, chunkSize, "mid"));
            }

            //
            // DXM
            //
            if (match(bin, x, "MCDF")) {

                byte[] readByte = new byte[0];

                while (!match(readByte, "CTrk")) {
                    chunkSize++;
                    if (x + chunkSize + 4 > size) {
                        break;
                    }
                    readByte = slice(bin, x + chunkSize, 4);

                    if (chunkSize >= size - x) {
                        break;
                    }
                }

                if (match(readByte, "CTrk")) {
                    int trkLen = readIntBE(bin, x + chunkSize + 4);
                    chunkSize += trkLen + 8;
                    hits.add(new Hit(x, chunkSize, "dxm"));
                }
            }

            //
            // PMD
            //
            if (match(bin, x, "cmid")) {
                chunkSize = readIntBE(bin, x + 4);
                if (chunkSize >= 16 && chunkSize <= 0x10_0000) {
                    hits.add(new Hit(x, chunkSize + 8, "pmd"));
                }
            }

            //
            // MLD
            //
            if (findMld && match(bin, x, "melo")) {
                chunkSize = readIntBE(bin, x + 4);
                if (chunkSize >= 16 && chunkSize <= 0x10_0000) {
                    hits.add(new Hit(x, chunkSize + 8, "mld"));
                }
            }

            //
            // MFM
            //
            if (match(bin, x, "mfmp")) {
                chunkSize = readIntBE(bin, x + 4);
                if (chunkSize >= 16 && chunkSize <= 0x10_0000) {
                    hits.add(new Hit(x, chunkSize + 8, "mfm"));
                }
            }

            //
            // SMAF
            //
            if (findMmf && match(bin, x, "MMMD")) {
                chunkSize = readIntBE(bin, x + 4);
                if (chunkSize >= 16 && chunkSize <= 0x10_0000) {
                    hits.add(new Hit(x, chunkSize + 8, "mmf"));
                }
            }

            //
            // RIFF family
            //
            if (match(bin, x, "RIFF")) {
                chunkSize = readIntLE(bin, x + 4);
                if (chunkSize >= 16) {
                    if (match(bin, x + 8, "WAVE")) {
                        hits.add(new Hit(x, chunkSize + 8, "wav"));
                    }
                    if (match(bin, x + 8, "DLS ")) {
                        hits.add(new Hit(x, chunkSize + 8, "dls"));
                    }
                    if (match(bin, x + 8, "sfbk")) {
                        hits.add(new Hit(x, chunkSize + 8, "sf2"));
                    }
                    if (match(bin, x + 8, "RMID")) {
                        hits.add(new Hit(x, chunkSize + 8, "rmi"));
                    }
                    if (match(bin, x + 8, "QLCM")) {
                        hits.add(new Hit(x, chunkSize + 8, "qcp"));
                    }
                }
            }

            //
            // IMY
            //
            if (findImy && match(bin, x, "BEGIN:IMELODY")) {
                while (true) {
                    if (match(bin, x + chunkSize, "END:IMELODY")) {
                        hits.add(new Hit(x, chunkSize + 11, "imy"));
                        break;
                    }
                    chunkSize++;
                    if (chunkSize >= size - x || chunkSize >= 16384) {
                        break;
                    }
                }
            }

            //
            // NRT
            //
            if (findNrt
                    && (match(bin, x, new byte[] {0x00, 0x0A, 0x08})
                    || match(bin, x, new byte[] {0x00, 0x02, (byte) 0xFC}))) {
                while (true) {
                    chunkSize++;
                    if (match(bin, x + chunkSize, new byte[] {0x07, 0x0B})) {
                        hits.add(new Hit(x,chunkSize + 2, "nrt"));
                        break;
                    }
                    if (chunkSize >= size - x) {
                        break;
                    }
                }
            }
        }

        return hits;
    }

    private static boolean isCandidate(byte[] bin, int off) {
        return match(bin, off, "MTh")
                || match(bin, off, "MCD")
                || match(bin, off, "cmi")
                || match(bin, off, "mel")
                || match(bin, off, "mfm")
                || match(bin, off, "MMM")
                || match(bin, off, "RIF")
                || match(bin, off, "BEG")
                || match(bin, off, new byte[] {0x00, 0x0A, 0x08})
                || match(bin, off, new byte[] {0x00, 0x02, (byte) 0xFC});
    }

    private static boolean match(byte[] data, int off, String s) {
        return match(data, off, s.getBytes());
    }

    private static boolean match(byte[] data, int off, byte[] sig) {
        if (off < 0 || off + sig.length > data.length) {
            return false;
        }

        for (int i = 0; i < sig.length; i++) {
            if (data[off + i] != sig[i]) {
                return false;
            }
        }

        return true;
    }

    private static boolean match(byte[] data, String s) {
        return java.util.Arrays.equals(data, s.getBytes());
    }

    private static byte[] slice(byte[] data, int off, int len) {
        if (off + len > data.length) {
            return new byte[0];
        }

        byte[] result = new byte[len];
        System.arraycopy(data, off, result, 0, len);
        return result;
    }

    private static int readIntBE(byte[] data, int off) {
        return ((data[off] & 0xff) << 24)
                | ((data[off + 1] & 0xff) << 16)
                | ((data[off + 2] & 0xff) << 8)
                | (data[off + 3] & 0xff);
    }

    private static int readIntLE(byte[] data, int off) {
        return (data[off] & 0xff)
                | ((data[off + 1] & 0xff) << 8)
                | ((data[off + 2] & 0xff) << 16)
                | ((data[off + 3] & 0xff) << 24);
    }
}
