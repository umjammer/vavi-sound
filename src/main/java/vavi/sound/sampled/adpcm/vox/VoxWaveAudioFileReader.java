/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.vox;

import java.util.Map;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.sound.sampled.adpcm.AdpcmWaveAudioFileReader;
import vavi.util.win32.WAVE.fmt;


/**
 * Provider for VOX ADPCM audio file reading services. This implementation can parse
 * the format information from WAVE audio file, and can produce audio input
 * streams from files of this type.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 240316 nsano initial version <br>
 * @see "https://en.wikipedia.org/wiki/Dialogic_ADPCM"
 */
public class VoxWaveAudioFileReader extends AdpcmWaveAudioFileReader {

    @Override
    protected int getFormatCode() {
        return 0x17;
    }

    @Override
    protected Encoding getEncoding() {
        return VoxEncoding.VOX;
    }

    @Override
    protected Map<String, Object> toProperties(fmt fmt) {
        return null;
    }
}
