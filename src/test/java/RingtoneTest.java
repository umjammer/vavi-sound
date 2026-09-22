/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
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
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * RingtoneTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class RingtoneTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static {
        // ensure synthesizer using default
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
        // should be set for playing adpcm (implemented as a meta event listener)
        System.setProperty("javax.sound.midi.Synthesizer", "#Gervill");
    }

    @Property(name = "vavi.test.volume.midi")
    float midiVolume = 0.2f;

    @Property(name = "ringtone.file")
    String file = "src/test/resources/test.mfm";

    @Property(name = "ringtone.dir")
    String dir = "src/test/resources";

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static long time = onIde ? 1000 * 1000 : 10 * 1000;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @DisplayName("ToneSniffer")
    void test() throws Exception {
Debug.print(file);
        byte[] data = Files.readAllBytes(Path.of(file));
        List<ToneSniffer.Hit> hits = ToneSniffer.scan(data, true, false, false, false);
Debug.print(hits);
    }

    @Test
    @DisplayName("play ringtone")
    public void test1() throws Exception {
        play(this.file);
    }

    @Test
    @DisplayName("play recursive dir")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test3() throws Exception {
        Path dir = Paths.get(this.dir);
        List<Path> files = Files.walk(dir)
                .filter(p ->
                        p.getFileName().toString().endsWith(".mfm") ||
                        p.getFileName().toString().endsWith(".dxm") ||
                        p.getFileName().toString().endsWith(".smd") ||
                        p.getFileName().toString().endsWith(".smz") ||
                        p.getFileName().toString().endsWith(".pmd")
                )
                .toList();
        Path path = files.get(new Random().nextInt(files.size()));
Debug.println("---- path: " + path);
        play(path.toString());
    }

    /** */
    void play(String file) throws Exception {
Debug.println("mld: " + file);

        CountDownLatch cdl = new CountDownLatch(1);
        Sequencer sequencer = MidiSystem.getSequencer(false); // "false" is important for volume!
Debug.println("@@@ sequencer: " + sequencer.getClass().getName());
        sequencer.open();
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println("@@@ synthesizer: " + synthesizer);
        synthesizer.open();
        Receiver receiver = synthesizer.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);
        Sequence sequence;
        if (file.startsWith("http"))
            sequence = MidiSystem.getSequence(URI.create(file).toURL());
        else
            sequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(Path.of(file))));
Debug.println("@@@ sequence: " + sequence);
        volume(receiver, midiVolume);
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(meta -> {
            Debug.println("meta: " + MidiConstants.MetaEvent.valueOf(meta.getType()));
            if (meta.getType() == 47) cdl.countDown();
        });
        sequencer.start();
if (!onIde) {
 Thread.sleep(time);
 sequencer.stop();
 Debug.println("STOP");
} else {
        cdl.await();
}
        sequencer.close();
    }
}
