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
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;


/**
 * Play PCM.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030714 nsano initial version <br>
 */
public class t150_3 {

    /**
     * usage: java t150_3 pcm_file [sampleRate] [byteOrder(le,be)]
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

        t150_3 player = new t150_3();
        player.play(file, sampleRate, byteOrder);
    }

    /**
     * 
     */
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
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
        line.start();
        byte[] buf = new byte[1024];
        int l = 0;
        while (is.available() > 0) {
            l = is.read(buf, 0, 1024);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
    }
}

/* */
