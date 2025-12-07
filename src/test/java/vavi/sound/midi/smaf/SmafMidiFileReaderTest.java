/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.sound.midi.MidiConstants;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * SmafMidiFileReaderTest (javax.midi.spi for SMAF).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/03/14 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class SmafMidiFileReaderTest {

    static {
        // ensure synthesizer using default
        System.setProperty("javax.sound.midi.Synthesizer", "#Gervill");
        // should be set for playing adpcm (implemented as a meta event listener)
        System.setProperty("javax.sound.midi.Sequencer", "vavi.sound.midi.VaviSequencer");
    }

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume.midi")
    float midiVolume = 0.2f;

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String mmf = "src/test/resources/test.mmf";

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
        play();
    }

    void play() throws Exception {
Debug.println("mmf: " + mmf);

        CountDownLatch cdl = new CountDownLatch(1);
        Sequencer sequencer = MidiSystem.getSequencer(false); // "false" is important for volume!
Debug.println("@@@ sequencer: " + sequencer.getClass().getName());
        sequencer.open();
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println("@@@ synthesizer: " + synthesizer);
        synthesizer.open();
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        Sequence sequence;
        if (mmf.startsWith("http"))
            sequence = MidiSystem.getSequence(URI.create(mmf).toURL());
        else
            sequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(Path.of(mmf))));
Debug.println("@@@ sequence: " + sequence);
MidiSystem.write(sequence, 0, Files.newOutputStream(Path.of("tmp/mmf_out.mid")));

        volume(synthesizer.getReceiver(), midiVolume);
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(meta -> {
Debug.println("meta: " + MidiConstants.MetaEvent.valueOf(meta.getType()));
            if (meta.getType() == 47) cdl.countDown();
        });
        sequencer.start();
        cdl.await();
        sequencer.close();
    }

    // ----

    /**
     * Play SMAF file.
     * @param args [0] filename
     */
    public static void main(String[] args) throws Exception {
        SmafMidiFileReaderTest app = new SmafMidiFileReaderTest();
        app.setup();
        app.mmf = args[0];
        app.play();
    }
}
