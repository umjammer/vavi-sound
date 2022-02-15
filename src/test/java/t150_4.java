/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import static vavi.sound.SoundUtil.volume;


/**
 * Play PCM.
 *
 * <pre>
 * 4bit PCM NG
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030714 nsano initial version <br>
 */
public class t150_4 {

    /**
     * usage: java t150_4 pcm_file
     */
    public static void main(String[] args) throws Exception {

        // 0
        File file = new File(args[0]);

        // 1
        int sampleRate = 24000;
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

        // 2

        t150_4 player = new t150_4();
        player.play(file, sampleRate, byteOrder);
    }

    /**
     *
     */
    void play(File file, int sampleRate, ByteOrder byteOrder) throws Exception {
        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_UNSIGNED,
            sampleRate,
            8,
            1,
            1,
            sampleRate,
            ByteOrder.BIG_ENDIAN.equals(byteOrder));
System.err.println(format);

        InputStream is = new BufferedInputStream(new FileInputStream(file));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.addLineListener(new LineListener() {
            public void update(LineEvent ev) {
                if (LineEvent.Type.STOP == ev.getType()) {
                    System.exit(0);
                }
            }
        });
        volume(line, .2d);
        line.start();
        byte[] buf = new byte[line.getBufferSize()];
        int l = 0;
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

/* */
