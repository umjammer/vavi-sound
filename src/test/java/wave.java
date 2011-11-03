/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import vavi.util.win32.WAVE;


/**
 * wave ファイルからデータだけを抜き出すプログラム．
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020708 nsano initial version <br>
 *          0.01 030711 nsano new version compliant <br>
 *          0.02 030715 nsano fix <br>
 */
public class wave {
    /**
     * usage: java wave in_file out_file
     */
    public static void main(String[] args) throws Exception {
        InputStream is = new BufferedInputStream(new FileInputStream(args[0]));
        WAVE wave = (WAVE) WAVE.readFrom(is);
        WAVE.data data = (WAVE.data) wave.findChildOf(WAVE.data.class);
        is.close();

        OutputStream os = new BufferedOutputStream(new FileOutputStream(args[1]));
//System.err.println("wave.data: " + data.getWave().length);
        is = new BufferedInputStream(new ByteArrayInputStream(data.getWave()));

        byte[] buf = new byte[1024];
        int l = 0;
        while (is.available() > 0) {
            l = is.read(buf, 0, 1024);
            os.write(buf, 0, l);
        }
        os.flush();
        os.close();
        is.close();
    }
}

/* */
