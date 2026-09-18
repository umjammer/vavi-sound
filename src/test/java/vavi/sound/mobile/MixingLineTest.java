/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * MixingLineTest.
 * <p>
 * gervill played through a line of our own with the adpcm in it; needs
 * {@code --add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED} and an audio device,
 * the adpcm is a constant at the volume of 0.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
class MixingLineTest {

    private String previousOutput, previousVolume;

    @BeforeEach
    void setUp() {
        previousOutput = System.getProperty(AudioEngineMixer.OUTPUT_KEY);
        previousVolume = System.getProperty("vavi.sound.mobile.AudioEngine.volume");
        System.clearProperty(AudioEngineMixer.OUTPUT_KEY);
        // silent: this is about where the stream goes, not how it sounds
        System.setProperty("vavi.sound.mobile.AudioEngine.volume", "0");
        AudioEngineMixer.clear();
    }

    @AfterEach
    void tearDown() {
        AudioEngineMixer.clear();
        if (previousOutput == null) System.clearProperty(AudioEngineMixer.OUTPUT_KEY);
        else System.setProperty(AudioEngineMixer.OUTPUT_KEY, previousOutput);
        if (previousVolume == null) System.clearProperty("vavi.sound.mobile.AudioEngine.volume");
        else System.setProperty("vavi.sound.mobile.AudioEngine.volume", previousVolume);
    }

    static Synthesizer gervill() throws MidiUnavailableException {
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            if (info.getName().equals("Gervill")) return (Synthesizer) MidiSystem.getMidiDevice(info);
        }
        return null;
    }

    static MixingLine open(Synthesizer synthesizer) {
        try {
            return MixingLine.open(synthesizer);
        } catch (MidiUnavailableException e) {
            return null; // no audio device
        }
    }

    @Test
    void theAdpcmIsPlayedInTheSynthesizersLine() throws Exception {
        Synthesizer synthesizer = gervill();
        assumeTrue(synthesizer != null, "no gervill");
        int before = AudioEngineMixer.attached(); // synthesizers another test left open
        MixingLine line = open(synthesizer);
        assumeTrue(line != null, "com.sun.media.sound not exported or no audio device");
        try {
            assertTrue(synthesizer.isOpen());
            assertTrue(AudioEngineMixer.isEnabled());

            AudioEngineMixerTest.TestEngine engine = AudioEngineMixerTest.engine(AudioEngineMixerTest.constant(800, (short) 1000));
            AudioEngine.Sync.schedule(() -> engine.start(0));
            assertNull(engine.line);
            assertTrue(AudioEngineMixer.isPlaying());
            // 100 ms of it, taken by the line's loop
            long until = System.currentTimeMillis() + 3000;
            while (AudioEngineMixer.isPlaying() && System.currentTimeMillis() < until) {
                Thread.sleep(20);
            }
            assertFalse(AudioEngineMixer.isPlaying());
        } finally {
            line.close();
        }
        assertFalse(synthesizer.isOpen());
        assertEquals(before, AudioEngineMixer.attached());
    }

    @Test
    void lineKeepsTheSynthesizerAsItWas() throws Exception {
        System.setProperty(AudioEngineMixer.OUTPUT_KEY, "line");
        Synthesizer synthesizer = gervill();
        assumeTrue(synthesizer != null, "no gervill");
        assertNull(MixingLine.open(synthesizer));
        assertFalse(synthesizer.isOpen());
    }

    @Test
    void anotherSynthesizerIsNotTaken() throws Exception {
        Synthesizer other = new vavi.sound.midi.mfi.MfiSynthesizer();
        assertNull(MixingLine.open(other));
        assertNotNull(other);
    }
}
