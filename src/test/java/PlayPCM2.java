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
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

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
@PropsEntity(url = "file:local.properties")
public class PlayPCM2 {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

    /**
     * usage: java PlayPCM2 pcm_file
     */
    public static void main(String[] args) throws Exception {

        // 0
        File file = new File(args[0]);

        // 1
        int sampleRate = 24000;
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

        // 2
        PlayPCM2 player = new PlayPCM2();
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(player);
        }
        player.play(file, sampleRate, byteOrder);
    }

    /** */
    void play(File file, int sampleRate, ByteOrder byteOrder) throws Exception {
        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_UNSIGNED,
            sampleRate,
            8,
            1,
            1,
            sampleRate,
            ByteOrder.BIG_ENDIAN.equals(byteOrder));
Debug.println(format);

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
