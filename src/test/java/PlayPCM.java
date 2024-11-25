/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

import static vavi.sound.SoundUtil.volume;


/**
 * Play PCM.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030714 nsano initial version <br>
 */
public class PlayPCM {

    static final double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    /**
     * usage: java PlayPCM pcm_file [sampleRate] [byteOrder(le,be)]
     */
    public static void main(String[] args) throws Exception {

        // 0
        File file = new File(args[0]);

        // 1
        int sampleRate = 16000;
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        if (args.length >= 2) {
            sampleRate = Integer.parseInt(args[1]);
System.err.println("sampleRate: " + sampleRate);
        }

        // 2
        if (args.length >= 3) {
            byteOrder = "le".equalsIgnoreCase(args[2]) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
System.err.println("byteOrder: " + byteOrder);
        }

        PlayPCM player = new PlayPCM();
        player.play(file, sampleRate, byteOrder);
    }

    /** */
    void play(File file, int sampleRate, ByteOrder byteOrder) throws Exception {
        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            ByteOrder.BIG_ENDIAN.equals(byteOrder));
System.err.println(format);

        InputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.addLineListener(ev -> {
            if (LineEvent.Type.STOP == ev.getType()) {
                System.exit(0);
            }
        });
        volume(line, volume);
        line.start();
        byte[] buf = new byte[line.getBufferSize()];
        int l;
        while (is.available() > 0) {
            l = is.read(buf, 0, buf.length);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();

        is.close();
    }
}
