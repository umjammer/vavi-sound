/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.dvi;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.nio.file.Files;
import java.nio.file.Paths;

import vavi.io.BitOutputStream;


/**
 * T400 ADPCM coder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030713 nsano completes <br>
 */
public class Encoder {

    public static void main(String[] args) throws Exception {

        DataInputStream is = new DataInputStream(new BufferedInputStream(Files.newInputStream(Paths.get(args[0]))));
        BitOutputStream os = new BitOutputStream(Files.newOutputStream(Paths.get(args[1])));

        Dvi encoder = new Dvi();

        while (true) {
            try {
                os.write(encoder.encode(is.readShort()));
            } catch (EOFException e) {
                break;
            }
        }

        os.flush();
        os.close();
        is.close();
    }
}
