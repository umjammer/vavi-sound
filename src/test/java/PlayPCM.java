/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.SoundUtil.volume;


/**
 * Play PCM.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030714 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class PlayPCM {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String file;

    @Property
    int sampleRate = 16000;

    @Property
    boolean bigEndian = true;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
Debug.println("volume: " + volume);
    }

    /** */
    void play() throws Exception {
        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            bigEndian);
Debug.println(format);

        Path path = Path.of(file);
        InputStream is = new BufferedInputStream(Files.newInputStream(path));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.addLineListener(ev -> Debug.println(ev.getType()));
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

    /**
     * usage: java PlayPCM pcm_file [sampleRate] [byteOrder(le,be)]
     */
    public static void main(String[] args) throws Exception {
        PlayPCM app = new PlayPCM();
        app.setup();
        app.file = args[0];
        if (args.length >= 2) {
            app.sampleRate = Integer.parseInt(args[1]);
Debug.println("sampleRate: " + app.sampleRate);
        }
        if (args.length >= 3) {
            app.bigEndian = !"le".equalsIgnoreCase(args[2]);
Debug.println("bigEndian: " + app.bigEndian);
        }
        app.play();
    }
}
