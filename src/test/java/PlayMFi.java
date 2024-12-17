/*
 * Copyright (c) 2009 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.sound.mfi.MetaEventListener;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.Sequencer;
import vavi.sound.mfi.Synthesizer;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * test mfi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 090913 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class PlayMFi {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static {
        // should be set for playing adpcm (implemented as a meta event listener)
        System.setProperty("javax.sound.midi.Sequencer", "vavi.sound.midi.VaviSequencer");
    }

    @Property(name = "vavi.test.volume.midi")
    float volume = 0.2f;

    @Property(name = "sf2")
    String sf2 = System.getProperty("user.home") + "/Library/Audio/Sounds/Banks/Orchestra/default.sf2";

    @Property
    String mfi = "src/test/resources/test.mfi";

    Sequencer sequencer;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
Debug.println("volume: " + volume);

        sequencer = MfiSystem.getSequencer();
Debug.println(sequencer.getClass().getName());
        sequencer.open();

Synthesizer synthesizer = (Synthesizer) sequencer;
// sf
Path sf2Path = Path.of(sf2);
if (Files.exists(sf2Path)) {
 Soundbank soundbank = synthesizer.getDefaultSoundbank();
//Instrument[] instruments = synthesizer.getAvailableInstruments();
 Debug.print("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
 synthesizer.unloadAllInstruments(soundbank);
 soundbank = MidiSystem.getSoundbank(sf2Path.toFile());
 synthesizer.loadAllInstruments(soundbank);
//instruments = synthesizer.getAvailableInstruments();
 Debug.print("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
}
    }

    @AfterEach
    void tearDown() throws Exception {
        sequencer.close();
    }

    @Test
    void test1() throws Exception {
        exec();
    }

    /** */
    void exec() throws Exception {
Debug.println("START: " + mfi);
        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        Sequence sequence = MfiSystem.getSequence(Path.of(mfi).toFile());
        volume(((Synthesizer) sequencer).getReceiver(), volume);
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
        sequencer.start();
        cdl.await();
Debug.println("END: " + mfi);
        sequencer.removeMetaEventListener(mel);
    }

    /**
     * @param args mfi files ...
     */
    public static void main(String[] args) throws Exception {
        PlayMFi app = new PlayMFi();
        app.setup();
        for (String arg : args) {
            app.mfi = arg;
            app.exec();
        }
        app.tearDown();
    }
}
