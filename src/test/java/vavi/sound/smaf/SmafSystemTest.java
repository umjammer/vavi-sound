/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * SmafSystemTest (javax.midi.spi for SMAF).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class SmafSystemTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property(name = "vavi.test.volume.midi")
    float midiVolume = 0.2f;

    @Property
    String mmf = "src/test/resources/test.mmf";

    @Property
    String out = "tmp/out.mid";

    Sequencer sequencer;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

Debug.println("volume: " + volume);
        System.setProperty("vavi.sound.mobile.AudioEngine.volume", String.valueOf(volume));
Debug.println("adpcm volume: " + System.getProperty("vavi.sound.mobile.AudioEngine.volume"));

        sequencer = SmafSystem.getSequencer();
        sequencer.open();
    }

    @AfterEach
    void tearDown() throws Exception {
        sequencer.close();
    }

    @Test
    public void test() throws Exception {
        play();
        convert();
    }

    /** */
    void play() throws Exception {
        Path path = Path.of(mmf);
Debug.println("path: " + path);
        CountDownLatch cdl = new CountDownLatch(1);
        vavi.sound.smaf.Sequence sequence = SmafSystem.getSequence(new BufferedInputStream(Files.newInputStream(path)));
        volume(((Synthesizer) sequencer).getReceiver(), midiVolume); // TODO interlock mid adpcm volume
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(meta -> {
Debug.println(meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        });
        sequencer.start();
        cdl.await();
    }

    /** */
    void convert() throws Exception {
        Path path = Path.of(mmf);
Debug.println("path: " + path);
        vavi.sound.smaf.Sequence smafSequence = SmafSystem.getSequence(new BufferedInputStream(Files.newInputStream(path)));
        Sequence midiSequence = SmafSystem.toMidiSequence(smafSequence);
//Debug.println("☆☆☆ here: " + midiSequence);
        int[] ts = MidiSystem.getMidiFileTypes(midiSequence);
//Debug.println("★★★ here");
//Debug.println("types: " + ts.length);
        if (ts.length == 0) {
            throw new IllegalArgumentException("no support type");
        }
        for (int i = 0; i < ts.length; i++) {
//Debug.println("type: 0x" + StringUtil.toHex2(ts[i]));
        }

        int r = MidiSystem.write(midiSequence, 0, Path.of(out).toFile());
Debug.println("write: " + r + " bytes as '" + out + "'");
    }

    // ----

    /**
     * Tests this class.
     * <pre>
     * usage:
     *  % java -Djavax.sound.midi.Sequencer="#Java MIDI(MFi/SMAF) ADPCM Sequencer" SmafSystem -p in_mmf_file
     *  % java SmafSystem -c in_mmf_file out_mid_file
     * </pre>
     */
    public static void main(String[] args) throws Exception {
        boolean convert = false;
        boolean play = false;

        if (args[0].equals("-c")) {
            convert = true;
        } else if (args[0].equals("-p")) {
            play = true;
        } else {
            throw new IllegalArgumentException(args[0]);
        }

        SmafSystemTest app = new SmafSystemTest();
        app.setup();
        app.mmf = args[1];

        if (play) {
            app.play();
        }

        if (convert) {
            app.out = args[2];
            app.convert();
        }

        app.tearDown();
    }
}
