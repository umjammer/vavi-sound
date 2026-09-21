package vavi.sound.mobile;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


class ScratchRateTest {
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ai")
    void test() throws Exception {
        System.setProperty("vavi.sound.mobile.AudioEngine.volume", "0.0");
        Sequencer sequencer = MidiSystem.getSequencer(); // connected to the default synthesizer
        sequencer.open();
        System.out.println("SCRATCH attached after sequencer open: " + AudioEngineMixer.attached());
        Sequence sequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(Path.of(
                "/Users/nsano/Public/np2/mfi/Ringtones (MLD)/90s anime songs from Cuebus N506iS/region_16934.mld"))));
        sequencer.setSequence(sequence);
        long t0 = System.nanoTime();
        sequencer.start();
        boolean seen = false;
        while (System.nanoTime() - t0 < 4_000_000_000L) {
            boolean playing = AudioEngineMixer.isPlaying();
            if (playing && !seen) { seen = true; System.out.printf("SCRATCH voice start %.3f s%n", (System.nanoTime() - t0) / 1e9); }
            if (!playing && seen) { System.out.printf("SCRATCH voice end %.3f s, attached %d%n", (System.nanoTime() - t0) / 1e9, AudioEngineMixer.attached()); break; }
            Thread.sleep(5);
        }
        sequencer.close();
    }
}
