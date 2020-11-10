/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;

import vavi.io.BitInputStream;


/**
 * CCITT ADPCM decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030713 nsano port to java <br>
 */
public class Decoder {

    /**
     * Usage : java vavi...Decoder [-2|-3|4|5] [-a|u|l] infile outfile
     */
    public static void main(String[] args) throws Exception {

        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        String decoderName = "vavi.sound.adpcm.ccitt.G721";

        InputStream in = System.in;
        OutputStream out = System.out;

        // Process encoding argument, if any
        int argc = 0;
        do {
            switch (args[argc].charAt(0)) {
            case '-':
                switch (args[argc].charAt(1)) {
                case '2':
                    decoderName = "vavi.sound.adpcm.ccitt.G723_16";
                    break;
                case '3':
                    decoderName = "vavi.sound.adpcm.ccitt.G723_24";
                    break;
                case '4':
                    decoderName = "vavi.sound.adpcm.ccitt.G721";
                    break;
                case '5':
                    decoderName = "vavi.sound.adpcm.ccitt.G723_40";
                    break;
                case 'u':
                    encoding = AudioFormat.Encoding.ULAW;
                    break;
                case 'a':
                    encoding = AudioFormat.Encoding.ALAW;
                    break;
                case 'l':
                    encoding = AudioFormat.Encoding.PCM_SIGNED;
                    break;
                case 'x':
                    byteOrder = ByteOrder.LITTLE_ENDIAN;
                    break;
                default:
System.err.println("CCITT ADPCM Decoder -- usage:");
System.err.println("\tdecode [-2|-3|4|5] [-a|u|l] [-x] infile outfile");
System.err.println("where:");
System.err.println("\t-2\tProcess G.726 16kbps (2-bit) input data");
System.err.println("\t-3\tProcess G.723 24kbps (3-bit) input data");
System.err.println("\t-4\tProcess G.721 32kbps (4-bit) input data [default]");
System.err.println("\t-5\tProcess G.723 40kbps (5-bit) input data");
System.err.println("\t-a\tGenerate 8-bit A-law data");
System.err.println("\t-u\tGenerate 8-bit u-law data");
System.err.println("\t-l\tGenerate 16-bit linear PCM data [default]");
System.err.println("\t-x\tlittle endian");
                    System.exit(1);
                }
                break;
            default:
                if (in == System.in) {
                    in = new FileInputStream(args[argc]);
System.err.println("input: " + args[argc]);
                } else {
                    out = new FileOutputStream(args[argc]);
System.err.println("output: " + args[argc]);
                }
                break;
            }
        } while (++argc < args.length);

        G711 decoder = (G711) Class.forName(decoderName).newInstance();
        decoder.setEncoding(encoding);

        InputStream is = new BitInputStream(
            new BufferedInputStream(in),
            decoder.getEncodingBits(),
            ByteOrder.LITTLE_ENDIAN);
        OutputStream os = new BufferedOutputStream(out);

        while (is.available() > 0) {
            int adpcm = is.read();
            int pcm = decoder.decode(adpcm);
            if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                os.write(pcm & 0xff);
                os.write((pcm & 0xff00) >> 8);
            } else {
                os.write((pcm & 0xff00) >> 8);
                os.write(pcm & 0xff);
            }
        }

        os.flush();
        os.close();

        is.close();
    }
}

/* */
