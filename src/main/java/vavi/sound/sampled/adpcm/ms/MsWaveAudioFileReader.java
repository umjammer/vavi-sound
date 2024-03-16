/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ms;

import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.sound.sampled.adpcm.AdpcmWaveAudioFileReader;
import vavi.util.ByteUtil;
import vavi.util.win32.WAVE;


/**
 * Provider for Microsoft ADPCM audio file reading services. This implementation can parse
 * the format information from WAVE audio file, and can produce audio input
 * streams from files of this type.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 240316 nsano initial version <br>
 */
public class MsWaveAudioFileReader extends AdpcmWaveAudioFileReader {

    @Override
    protected int getFormatCode() {
        return 0x02;
    }

    @Override
    protected Encoding getEncoding() {
        return MsEncoding.MS;
    }

    @Override
    protected int getBufferSize() {
        return super.getBufferSize() + 2 + 32;
    }

    @Override
    protected Map<String, Object> toProperties(WAVE.fmt fmt) {
        Map<String, Object> properties = new HashMap<>();
        byte[] b = fmt.getExtended();
        int samplesPerBlock = ByteUtil.readLeShort(b, 0);
        int nCoefs = ByteUtil.readLeShort(b, 2);
        assert nCoefs < 8 : "cannot deal coefs > 7";
        int[][] iCoefs = new int[nCoefs][2];
        for (int i = 0; i < nCoefs; i++) {
            for (int j = 0; j < 2; j++) {
                iCoefs[i][j] = ByteUtil.readLeShort(b, 4 + (i * 2 + j) * Short.BYTES);
            }
        }
        int blockSize = fmt.getBlockSize();
        properties.put("samplesPerBlock", samplesPerBlock);
        properties.put("nCoefs", nCoefs);
        properties.put("iCoefs", iCoefs);
        properties.put("blockSize", blockSize);
        return properties;
    }
}
