/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ima;

import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.sound.sampled.adpcm.AdpcmWaveAudioFileReader;
import vavi.util.ByteUtil;


/**
 * Provider IMA(DVI) ADPCM audio file reading services. This implementation can parse
 * the format information from WAVE audio file, and can produce audio input
 * streams from files of this type.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 240316 nsano initial version <br>
 */
public class ImaWaveAudioFileReader extends AdpcmWaveAudioFileReader {

    @Override
    protected int getFormatCode() {
        return 0x11;
    }

    @Override
    protected Encoding getEncoding() {
        return ImaEncoding.IMA;
    }

    @Override
    protected int getBufferSize() {
        return super.getBufferSize() + 2 + 2;
    }

    @Override
    protected Map<String, Object> toProperties(byte[] b) {
        Map<String, Object> properties = new HashMap<>();
        int samplesPerBlock = ByteUtil.readLeShort(b, 38);
        int blockSize = ByteUtil.readLeShort(b, 32);
        properties.put("samplesPerBlock", samplesPerBlock);
        properties.put("blockSize", blockSize);
        return properties;
    }
}
