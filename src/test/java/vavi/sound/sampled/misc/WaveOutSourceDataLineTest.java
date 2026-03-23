/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.misc;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * WaveOutSourceDataLineTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-19 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class WaveOutSourceDataLineTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static long time = onIde ? 1000 * 1000 : 10 * 1000;

    @Property(name = "vavi.test.volume.midi")
    float volume = 0.2f;

    @Property
    String midi = "src/test/resources/test.mid";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

Debug.println("volume: " + volume);
    }

    @BeforeAll
    static void setupAll() {
        System.setProperty("javax.sound.midi.Synthesizer", "#Gervill");
        System.setProperty("javax.sound.sampled.SourceDataLine", "#WaveOut Mixer");
//        System.setProperty("javax.sound.sampled.SourceDataLine", "#Null Mixer");
    }

    @Test
    void test() throws Exception {
Debug.println(midi);

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        Receiver receiver = synthesizer.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.open();
Debug.println("sequencer: " + sequencer + ", " + sequencer.getClass().getName());

        Path file = Paths.get(midi);

        Sequence seq = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(file)));

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
Debug.println("START");
        sequencer.start();

        volume(receiver, volume);

if (!onIde) {
 Thread.sleep(time);
 sequencer.stop();
 Debug.println("STOP");
} else {
        cdl.await();
}
Debug.println("END");
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        synthesizer.close();

        if ("#WaveOut Mixer".equals(System.getProperty("javax.sound.sampled.SourceDataLine")))
            Files.move(Path.of(System.getProperty("vavi.sound.sampled.misc.waveout")), Path.of("tmp", "waveout.wav"), StandardCopyOption.REPLACE_EXISTING);
    }

    @AfterAll
    static void tearDownAll() {
        System.setProperty("javax.sound.sampled.SourceDataLine", "");
    }
}
