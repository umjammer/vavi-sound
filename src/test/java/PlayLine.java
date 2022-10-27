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
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

import static vavi.sound.SoundUtil.volume;


/**
 * Play.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030711 nsano initial version <br>
 */
public class PlayLine {

    /**
     * usage: java PlayLine file ...
     */
    public static void main(String[] args) throws Exception {

        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.err.println(type);
        }

        // play
        PlayLine player = new PlayLine();
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
        line.addLineListener(ev -> {
            if (LineEvent.Type.STOP == ev.getType()) {
                System.exit(0);
            }
        });
        volume(line, .2d);
        line.start();
        byte[] buf = new byte[line.getBufferSize()];
        int l;
        while (ais.available() > 0) {
            l = ais.read(buf, 0, buf.length);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
    }
}

/* */
