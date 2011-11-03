/*
 * http://www.hundredsoft.jp/wav2mld/source/p211cnv.c
 */

package vavi.sound.adpcm.vox;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import vavi.sound.adpcm.Codec;
import vavi.util.Debug;


/**
 * Vox ADPCM codec.
 * 
 * @author Furuhon
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030715 nsano port to java <br>
 * @see "http://www.hundredsoft.jp/wav2mld/source/p211cnv.c"
 */
class Vox implements Codec {

    /** */
    private class State {
        int index;
        int last;
    }

    /** */
    private static final int table[] = {
        16, 17, 19, 21, 23, 25, 28, 31, 34, 37,
        41, 45, 50, 55, 60, 66, 73, 80, 88, 97,
        107, 118, 130, 143, 157, 173, 190, 209, 230, 253,
        279, 307, 337, 371, 408, 449, 494, 544, 598, 658,
        724, 796, 876, 963, 1060, 1166, 1282, 1408, 1552
    };

    /** */
    private State state = new State();

    /** */
    public int encode(int samp) {
        int code = 0;
        int ss = table[state.index];
        int diff = samp - state.last;
        int e = (diff < 0) ? -diff : diff;

        if (diff < 0) {
            code |= 8;
        }
        if (e >= ss) {
            code |= 4;
            e -= ss;
        }
        if (e >= ss / 2) {
            code |= 2;
            e -= ss / 2;
        }
        if (e >= ss / 4) {
            code |= 1;
        }

        state.last = decode(code);
//System.err.printf("%04X -> %02X\n", samp, code);
        return code;
    }

    /** */
    public int decode(int code) {
        int diff;
        int ss = table[state.index];
        int e = ss / 8;

        if ((code & 1) != 0) {
            e += ss / 4;
        }
        if ((code & 2) != 0) {
            e += ss / 2;
        }
        if ((code & 4) != 0) {
            e += ss;
        }

        if ((code & 8) != 0) {
            diff = -e;
        } else {
            diff = e;
        }
        int samp = state.last + diff;

        if (samp > 2048) {
            samp = 2048;
        }
        else if (samp < -2048) {
            samp = -2048;
        }

        state.last = samp;
        state.index += adjust(code);
        if (state.index < 0) {
            state.index = 0;
        } else if (state.index > 48) {
            state.index = 48;
        }

        return samp;
    }

    /** */
    private int adjust(int code) {
        int c = code & 0x07;
        if (c < 4) {
            return -1;
        }
        return (c - 3) * 2;
    }

    //-------------------------------------------------------------------------

    /** */
    public static void main(String[] args) throws Exception {

        int sampleRate = 8000;
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println(audioFormat);

        InputStream is = new VoxInputStream(new FileInputStream(args[0]),
                                            byteOrder);
System.err.println("available: " + is.available());

//  OutputStream os =
//   new BufferedOutputStream(new FileOutputStream(args[1]));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.addLineListener(new LineListener() {
            public void update(LineEvent ev) {
Debug.println(ev.getType());
        		if (LineEvent.Type.STOP == ev.getType()) {
        		    System.exit(0);
        		}
            }
        });
        line.start();
        byte[] buf = new byte[1024];
        int l = 0;

        while (is.available() > 0) {
            l = is.read(buf, 0, 1024);
            line.write(buf, 0, l);
//  os.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
//  os.close();
    }
}

/* */
