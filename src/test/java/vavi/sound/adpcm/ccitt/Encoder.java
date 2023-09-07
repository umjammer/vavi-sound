/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;

import vavi.io.BitOutputStream;


/**
 * CCITT ADPCM encoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030713 nsano port to java <br>
 *          0.01 030901 nsano add byteOrder <br>
 */
public class Encoder {

    /**
     * Usage : java vavi...Encoder [-2|-3|4|5] [-a|u|l] [-x] < infile > outfile
     */
    public static void main(String[] args) throws Exception {

        // Set defaults to u-law input, G.721 output
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        String encoderName = "vavi.sound.adpcm.ccitt.G721";

        InputStream in = System.in;
        OutputStream out = System.out;

        // Process encoding argument, if any
        int argc = 0;
        do {
            switch (args[argc].charAt(0)) {
            case '-':
                switch (args[argc].charAt(1)) {
                case '2':
                    encoderName = "vavi.sound.adpcm.ccitt.G723_16";
                    break;
                case '3':
                    encoderName = "vavi.sound.adpcm.ccitt.G723_24";
                    break;
                case '4':
                    encoderName = "vavi.sound.adpcm.ccitt.G721";
                    break;
                case '5':
                    encoderName = "vavi.sound.adpcm.ccitt.G723_40";
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
System.err.println("CCITT ADPCM Encoder -- usage:");
System.err.println("\tencode [-2|-3|4|5] [-a|u|l] [-x] < infile > outfile");
System.err.println("where:");
System.err.println("\t-2\tGenerate G.726 16kbps (2-bit) data");
System.err.println("\t-3\tGenerate G.723 24kbps (3-bit) data");
System.err.println("\t-4\tGenerate G.721 32kbps (4-bit) data [default]");
System.err.println("\t-5\tGenerate G.723 40kbps (5-bit) data");
System.err.println("\t-a\tProcess 8-bit A-law input data");
System.err.println("\t-u\tProcess 8-bit u-law input data");
System.err.println("\t-l\tProcess 16-bit linear PCM input data [default]");
System.err.println("\t-x\tProcess 16-bit PCM little endian");
                    System.exit(1);
                }
                break;
            default:
                if (in == System.in) {
                    in = Files.newInputStream(Paths.get(args[argc]));
System.err.println("input: " + args[argc]);
                } else {
                    out = Files.newOutputStream(Paths.get(args[argc]));
System.err.println("output: " + args[argc]);
                }
                break;
            }
        } while (++argc < args.length);

        G711 encoder = (G711) Class.forName(encoderName).getDeclaredConstructor().newInstance();
        encoder.setEncoding(encoding);

        InputStream is = new BufferedInputStream(in);
        OutputStream os = new BitOutputStream(
            new BufferedOutputStream(out),
            encoder.getEncodingBits(),
            ByteOrder.LITTLE_ENDIAN);

        while (is.available() > 0) {
            int pcm1 = is.read();
            int pcm2 = is.read();
            int pcm;
            if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                pcm = (pcm1 << 8) | pcm2;
            } else {
                pcm = (pcm2 << 8) | pcm1;
            }
            if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
                if ((pcm & 0x8000) != 0) {
                    pcm -= 0x10000;
                }
            }
//System.err.println("current: " + StringUtil.toHex4(pcm) + ": " + pcm);
            int adpcm = encoder.encode(pcm);
            os.write(adpcm);
        }

        os.flush();
        os.close();
    }
}

/* */
