/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.dvi;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import vavi.io.BitInputStream;
import vavi.io.LittleEndianDataOutputStream;


/**
 * T400 ADPCM decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030713 nsano completes <br>
 */
public class Decoder {

    /**
     * java Decoder adpcm pcm
     */
    public static void main(String[] args) throws Exception {
        b(args);
    }

    /**
     * big-endian 出力
     */
    static void b(String[] args) throws IOException {

        Dvi decoder = new Dvi();

        BitInputStream is = new BitInputStream(new FileInputStream(args[0]));
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[1])));

        while (true) {
            int abuf = is.read();
            if (abuf == -1) {
                break;
            }
            int sbuf = decoder.decode(abuf);
            os.writeShort(sbuf);
        }

        os.flush();
        os.close();
        is.close();
    }

    /**
     * little-endian 出力
     */
    static void a(String[] args) throws IOException {

        BitInputStream is = new BitInputStream(new FileInputStream(args[0]));
        LittleEndianDataOutputStream os = new LittleEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(args[1])));

        Dvi decoder = new Dvi();

        while (true) {
            int abuf = is.read();
            if (abuf == -1) {
                break;
            }
            int sbuf = decoder.decode(abuf);
            os.writeShort(sbuf);
        }

        os.flush();
        os.close();
        is.close();
    }
}

/* */
