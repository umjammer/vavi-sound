/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;


/**
 * Play.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030711 nsano initial version <br>
 */
public class t150_1 {

    /**
     * usage: java t150_1 file ...
     */
    public static void main(String[] args) throws Exception {

        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.err.println(type);
        }

        // play
        t150_1 player = new t150_1();
        for (String arg : args) {
            player.play(arg);
        }
    }

    /** */
    void play(String filename) throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(filename));
        AudioFormat format = ais.getFormat();
System.err.println(format);
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
        int l;
        while (ais.available() > 0) {
            l = ais.read(buf, 0, 1024);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
    }
}

/* */
