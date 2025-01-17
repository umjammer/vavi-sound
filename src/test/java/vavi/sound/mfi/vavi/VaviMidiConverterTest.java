/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.MfiSystemTest;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * VaviMidiConverterTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class VaviMidiConverterTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property(name = "vavi.test.volume.midi")
    float midiVolume = 0.2f;

    @BeforeEach
    public void setup() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        System.setProperty("vavi.sound.mobile.AudioEngine.volume", String.valueOf(volume));
Debug.println("adpcm volume: " + System.getProperty("vavi.sound.mobile.AudioEngine.volume"));
    }

    @Test
    public void test() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);
        Path inPath = Paths.get(MfiSystemTest.class.getResource("/test.mld").toURI());
        vavi.sound.mfi.Sequence mfiSequence = MfiSystem.getSequence(new BufferedInputStream(Files.newInputStream(inPath)));
        Sequence midiSequence = MfiSystem.toMidiSequence(mfiSequence);

        Sequencer midiSequencer = MidiSystem.getSequencer();
Debug.println("midiSequencer: " + midiSequencer);
Debug.println("midiSequencer:T: " + midiSequencer.getTransmitter());
Debug.println("midiSequencer:R: " + midiSequencer.getReceiver());
        midiSequencer.open();
        midiSequencer.setSequence(midiSequence);
        midiSequencer.addMetaEventListener(meta -> {
Debug.println(meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        });
        midiSequencer.start();
        cdl.await();
        midiSequencer.close();
    }

    // ----

    boolean convert = false;
    boolean play = false;

    /**
     * Tests this class.
     * <pre>
     * usage:
     *  % java VaviMidiConverter -p in_mld_file
     *  % java VaviMidiConverter -c in_mld_file out_mid_file
     * </pre>
     * @param args 0: -p|-c, 1: in_mld, 2: [out_mid]
     */
    public static void main(String[] args) throws Exception {
        VaviMidiConverterTest app = new VaviMidiConverterTest();
        app.setup();

        if (args[0].equals("-c")) {
            app.convert = true;
        } else if (args[0].equals("-p")) {
            app.play = true;
        } else {
            throw new IllegalArgumentException(args[0]);
        }

        File in = new File(args[1]);
        File out = new File(args[2]);
        app.exec(in, out);
    }

    /** */
    void exec(File in, File out) throws Exception {

        vavi.sound.mfi.Sequence mfiSequence = MfiSystem.getSequence(in);
        Sequence midiSequence = MfiSystem.toMidiSequence(mfiSequence);

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();

        Sequencer midiSequencer = MidiSystem.getSequencer();
Debug.println("midiSequencer: " + midiSequencer);
Debug.println("midiSequencer:T: " + midiSequencer.getTransmitter());
Debug.println("midiSequencer:R: " + midiSequencer.getReceiver());
        midiSequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        midiSequencer.open();
        volume(synthesizer.getReceiver(), midiVolume);
        midiSequencer.setSequence(midiSequence);

        if (play) {
            CountDownLatch cdl = new CountDownLatch(1);
            midiSequencer.addMetaEventListener(meta -> {
Debug.println(meta.getType());
                if (meta.getType() == 47) cdl.countDown();
            });
            midiSequencer.start();
            cdl.await();
            midiSequencer.stop();
        }

        midiSequencer.close();
        synthesizer.close();

        if (convert) {
            int[] ts = MidiSystem.getMidiFileTypes(midiSequence);
Debug.println("types: " + ts.length);
            if (ts.length == 0) {
                throw new IllegalArgumentException("no support type");
            }
            for (int t : ts) {
Debug.printf("type: 0x%02x\n", t);
            }

            int r = MidiSystem.write(midiSequence, 0, out);
Debug.println("write: " + r);
        }
    }
}
