/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.Test;

import vavi.sound.mfi.MfiSystemTest;
import vavi.util.Debug;


/**
 * BasicAudioEngineTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class BasicAudioEngineTest {

    static final float volume = (float) Double.parseDouble(System.getProperty("vavi.test.volume.midi",  "0.2"));

    @Test
    public void test() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);
        Path inPath = Paths.get(MfiSystemTest.class.getResource("/test.mld").toURI());
        vavi.sound.mfi.Sequencer sequencer = vavi.sound.mfi.MfiSystem.getSequencer();
        sequencer.open();
        vavi.sound.mfi.Sequence sequence = vavi.sound.mfi.MfiSystem.getSequence(new BufferedInputStream(Files.newInputStream(inPath)));
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(meta -> {
Debug.println(meta.getType());
            if (meta.getType() == 47) {
                cdl.countDown();
            }
        });
        sequencer.start();
        cdl.await();
        sequencer.close();
    }

    //-------------------------------------------------------------------------

    /** */
    private static String fileName;

    /** */
    private static String pcmFileName;


    /** */
    public static class WrappedLineOutputStream extends OutputStream {
        SourceDataLine line;
        OutputStream out;
        public WrappedLineOutputStream(SourceDataLine line, OutputStream out) {
            this.out = out;
            this.line = line;
        }
        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b });
        }
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            line.write(b, off, len);
            if (out != null) {
                out.write(b, off, len);
            }
        }
        @Override
        public void flush() throws IOException {
            if (out != null) {
                out.flush();
            }
        }
        /** {@link #line} not closed */
        @Override
        public void close() throws IOException {
            if (out != null) {
                out.close();
            }
        }
    }

    /** */
    void debug1(byte[] adpcm) {
        try {
            OutputStream os;
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
            os = new BufferedOutputStream(Files.newOutputStream(Paths.get(pcmFileName)));
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

        CountDownLatch cdl = new CountDownLatch(1);
        vavi.sound.mfi.Sequencer sequencer = vavi.sound.mfi.MfiSystem.getSequencer();
        sequencer.open();
        vavi.sound.mfi.Sequence sequence = vavi.sound.mfi.MfiSystem.getSequence(new File(args[0]));
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(meta -> {
Debug.println(meta.getType());
            if (meta.getType() == 47) {
                cdl.countDown();
            }
        });
        sequencer.start();
        cdl.await();
        sequencer.close();
    }
}
