/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;

import vavi.util.Debug;

import static org.junit.Assert.*;


/**
 * BasicAudioEngineTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class BasicAudioEngineTest {

    @Test
    public void test() {
        fail("Not yet implemented");
    }

    //-------------------------------------------------------------------------

    /** */
    private static String fileName;

    /** */
    private static String pcmFileName;

    /** */
    void debug1(byte[] adpcm) {
        try {
            OutputStream os = null;
            if (fileName != null) {
Debug.println("★★★★★★★★ adpcm out to file: " + fileName);
                os = new BufferedOutputStream(new FileOutputStream(fileName, true));
                os.write(adpcm, 0, adpcm.length);
                os.flush();
                os.close();
            }
        } catch (IOException e) {
            Debug.printStackTrace(e);
        }
    }

    /** */
    OutputStream debug2() throws IOException {
        OutputStream os = null;
        if (pcmFileName != null) {
Debug.println("★★★★★★★★ output PCM to file: " + pcmFileName);
            os = new BufferedOutputStream(new FileOutputStream(pcmFileName));
        }
        return os;
    }

    /** */
    void debug3(OutputStream os, byte[] buf, int l) throws IOException {
        if (os != null) {
            os.write(buf, 0, l);
        }
    }

    /** */
    void debug4(OutputStream os) throws IOException {
        if (os != null) {
            os.flush();
            os.close();
        }
    }

    /**
     * Tests this class.
     *
     * usage: java $0 mfi_file adpcm
     */
    public static void main(String[] args) throws Exception {

        if (args.length >= 2) {
            fileName = args[1];
        }
        if (args.length >= 3) {
            pcmFileName = args[2];
        }

        vavi.sound.mfi.Sequencer sequencer = vavi.sound.mfi.MfiSystem.getSequencer();
        sequencer.open();
        vavi.sound.mfi.Sequence sequence = vavi.sound.mfi.MfiSystem.getSequence(new File(args[0]));
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(new vavi.sound.mfi.MetaEventListener() {
            public void meta(vavi.sound.mfi.MetaMessage meta) {
Debug.println(meta.getType());
                if (meta.getType() == 47) {
                    System.exit(0);
                }
            }
        });
        sequencer.start();
    }
}

/* */
