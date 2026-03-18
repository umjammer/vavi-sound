/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

import vavi.sound.midi.MidiConstants;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static vavi.sound.midi.MidiUtil.volume;


/**
 * MfiMidiFileReaderTest (javax.midi.spi for Mfi).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class MfiSynthesizerTest {

    static {
        // ensure synthesizer using default
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
        // should be set for playing adpcm (implemented as a meta event listener)
        System.setProperty("javax.sound.midi.Synthesizer", "#Java MIDI(MFi) Synthesizer");
        // prior user volume setting than setting in a sequence
        System.setProperty("vavi.sound.mfi.ignoreMasterVolume", "true");
    }

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume.midi")
    float midiVolume = 0.2f;

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String mfi = "src/test/resources/test.mld";

    @Property(name = "mfi.dir")
    String dir = "src/test/resources";

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
        play(this.mfi);
    }

    @Test
    @DisplayName("play recursive")
    void test3() throws Exception {
        Path dir = Paths.get(this.dir);
        AtomicInteger c = new AtomicInteger();
        Files.walk(dir)
                .filter(p -> p.getFileName().toString().endsWith(".mld"))
                .forEach(path -> {
Debug.println("---- path: " + path);
                    try {
                        play(path.toString());
                        c.getAndIncrement();
                    } catch (Exception e) {
Debug.println(e.getMessage());
                    }
                });
Debug.println("mfis: " + c.get());
    }

    void play(String mfi) throws Exception {
Debug.println("mld: " + mfi);

        CountDownLatch cdl = new CountDownLatch(1);
        Sequencer sequencer = MidiSystem.getSequencer(false); // "false" is important for volume!
Debug.println("@@@ sequencer: " + sequencer.getClass().getName());
        sequencer.open();
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println("@@@ synthesizer: " + synthesizer);
        assertInstanceOf(vavi.sound.midi.mfi.MfiSynthesizer.class, synthesizer);
        synthesizer.open();
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        Sequence sequence;
        if (mfi.startsWith("http"))
            sequence = MidiSystem.getSequence(URI.create(mfi).toURL());
        else
            sequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(Path.of(mfi))));
Debug.println("@@@ sequence: " + sequence);
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
     * Play MFi file.
     * @param args [0] filename
     */
    public static void main(String[] args) throws Exception {
        MfiSynthesizerTest app = new MfiSynthesizerTest();
        app.setup();
        app.play(args[0]);
    }
}
