/*
 * Copyright (c) 2011 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm;

import java.io.ByteArrayOutputStream;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * AdpcmOutputStreamTest. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2011/11/02 umjammer initial version <br>
 */
public class AdpcmOutputStreamTest {


    @Test
    public void test() throws Exception {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AdpcmOutputStream os = new AdpcmOutputStream(baos, ByteOrder.BIG_ENDIAN, 4, ByteOrder.BIG_ENDIAN) {
            protected Codec getCodec() {
                return new Codec() {
                    public int encode(int pcm) {
                        return pcm & 0xf;
                    }
                    public int decode(int adpcm) {
                        return adpcm | adpcm << 4 | adpcm << 8 | adpcm << 16;
                    }
                };
            }
        };
        os.close();
    }
}

/* */
