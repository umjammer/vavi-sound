/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.SoundUtil.volume;


/**
 * Play.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030711 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class PlayLine {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String file;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
Debug.println("volume: " + volume);
    }

    @Test
    void test1() throws Exception {
        play();
    }

    /** */
    void play() throws Exception {
        Path path = Path.of(file);
Debug.println("path: " + path);
        AudioInputStream ais = AudioSystem.getAudioInputStream(Path.of(file).toFile());
        AudioFormat format = ais.getFormat();
Debug.println(format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.addLineListener(ev -> {
Debug.println(ev.getType());
            if (LineEvent.Type.STOP == ev.getType()) {}
        });
        volume(line, volume);
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

    /**
     * usage: java PlayLine file ...
     */
    public static void main(String[] args) throws Exception {
for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
 System.err.println(type);
}

        // play
        PlayLine app = new PlayLine();
        app.setup();
        for (String arg : args) {
            app.file = arg;
            app.play();
        }
    }
}
