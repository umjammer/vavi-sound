/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import vavi.util.win32.WAVE;


/**
 * extract data from .wav file.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020708 nsano initial version <br>
 *          0.01 030711 nsano new version compliant <br>
 *          0.02 030715 nsano fix <br>
 */
public class ExtractWave {

    /**
     * usage: java ExtractWave in_file out_file
     */
    public static void main(String[] args) throws Exception {
        InputStream is = new BufferedInputStream(Files.newInputStream(Paths.get(args[0])));
        WAVE wave = WAVE.readFrom(is, WAVE.class);
        WAVE.data data = wave.findChildOf(WAVE.data.class);
        is.close();

        OutputStream os = new BufferedOutputStream(Files.newOutputStream(Paths.get(args[1])));
//Debug.println("wave.data: " + data.getWave().length);
        is = new BufferedInputStream(new ByteArrayInputStream(data.getWave()));

        byte[] buf = new byte[1024];
        int l;
        while (is.available() > 0) {
            l = is.read(buf, 0, 1024);
            os.write(buf, 0, l);
        }
        os.flush();
        os.close();
        is.close();
    }
}
