/*
 * http://www.hundredsoft.jp/wav2mld/source/n211cnv.c
 */

package vavi.sound.adpcm.yamaha;

import vavi.sound.adpcm.Codec;


/**
 * YAMAHA MA#.
 *
 * @author Furuhon 19-Apl-2004 Rev.1.00
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 1.10 050402 nsano port to java <br>
 * @see "http://www.hundredsoft.jp/wav2mld/source/n211cnv.c"
 */
class Yamaha implements Codec {

    /** */
    private static class Status {
        int index = 127;
        int last = 0;
    }

    /** You need initialize before reuse methods! */
    private final Status stat = new Status();

    /**
     * @param code ADPCM (LSB 4bit available)
     * @param ss ?
     * @return adjusted ss
     */
    private static int adjust(int code, int ss) {
        switch (code & 0x07) {
        case 0x00:
        case 0x01:
        case 0x02:
        case 0x03:
            ss = (ss * 115) / 128;
            break;
        case 0x04:
            ss = (ss * 307) / 256;
            break;
        case 0x05:
            ss = (ss * 409) / 256;
            break;
        case 0x06:
            ss = (ss * 2);
            break;
        case 0x07:
            ss = (ss * 307) / 128;
            break;
        }

        if (ss < 127) {
            ss = 127;
        }
        if (ss > 32768 * 3 / 4) {
            ss = 32768 * 3 / 4;
        }

        return ss;
    }

    /**
     * @param code ADPCM (LSB 4bit available)
     * @return 16bit PCM signed
     */
    @Override
    public int decode(int code) {

        int ss = stat.index;
        int e = ss / 8;
        if ((code & 0x01) != 0) {
            e += ss / 4;
        }
        if ((code & 0x02) != 0) {
            e += ss / 2;
        }
        if ((code & 0x04) != 0) {
            e += ss;
        }
        int diff = (code & 0x08) != 0 ? -e : e;
        int samp = stat.last + diff;

        if (samp > 32767) {
            samp = 32767;
        }
        if (samp < -32768) {
            samp = -32768;
        }

        stat.last = samp;
        stat.index = adjust(code, ss);

        return stat.last;
    }

    /**
     * @param samp 16bit PCM signed
     * @return ADPCM (LSB 4bit available)
     */
    @Override
    public int encode(int samp) {
        int diff;
        int ss = stat.index;
        int code = 0x00;
        if ((diff = samp - stat.last) < 0) {
            code = 0x08;
        }
        int e = diff < 0 ? -diff : diff;
        if (e >= ss) {
            code = code | 0x04;
            e -= ss;
        }
        if (e >= ss / 2) {
            code = code | 0x02;
            e -= ss / 2;
        }
        if (e >= ss / 4) {
            code = code | 0x01;
        }

        stat.last = decode(code);
//logger.log(Level.TRACE, "%04X -> %02X".formatted(samp, code));
        return code;
    }
}
