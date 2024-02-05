/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import vavi.util.Debug;


/**
 * TemporaryFiledInputFilter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060124 nsano initial version <br>
 */
public class TemporaryFiledInputFilter implements InputFilter {

    @Override
    public AudioInputStream doFilter(AudioInputStream sourceAis) throws IOException, UnsupportedAudioFileException {
        // 一回 temp ファイルに落とす
        File tmpFile = File.createTempFile("temp", ".wav");
        int r = AudioSystem.write(sourceAis, AudioFileFormat.Type.WAVE, tmpFile);
Debug.println(Level.FINE, "RESULT: " + r);

        return AudioSystem.getAudioInputStream(tmpFile);
    }
}
