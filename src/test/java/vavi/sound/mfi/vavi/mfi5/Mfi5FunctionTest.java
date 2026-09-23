/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mfi5;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.Track;
import vavi.sound.mfi.vavi.VaviMfiFileFormat;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;
import vavi.sound.mfi.vavi.sequencer.UndefinedFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests the MFi 5 machine dependent functions.
 * <p>
 * The payloads are the ones {@code Ringtones (MLD)/川の流れのように.mld} really has, the
 * file whose log the mission started from.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
class Mfi5FunctionTest {

    /** wraps a function payload (vendor byte first) into the message the sequencer would feed back */
    private static byte[] message(byte[] payload) throws Exception {
        MachineDependentMessage message = new MachineDependentMessage().init();
        message.setMessage(0, payload);
        return message.getMessage();
    }

    /** */
    private static byte[] hex(String hex) {
        return HexFormat.ofDelimiter(" ").parseHex(hex);
    }

    @Test
    void factory() {
        assertInstanceOf(Mfi5Sequencer.class, MachineDependentSequencer.Factory.getSequencer(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0x01 }));
        assertInstanceOf(Function16.class, MachineDependentFunction.Factory.getFunction("0.16"));
        assertInstanceOf(Function17.class, MachineDependentFunction.Factory.getFunction("0.17"));
        assertInstanceOf(Function18.class, MachineDependentFunction.Factory.getFunction("0.18"));
        assertInstanceOf(Function64.class, MachineDependentFunction.Factory.getFunction("0.64"));
        assertInstanceOf(Function176.class, MachineDependentFunction.Factory.getFunction("0.176"));
        assertInstanceOf(Function177.class, MachineDependentFunction.Factory.getFunction("0.177"));
        // the sharp ones are still where they were
        assertEquals(vavi.sound.mfi.vavi.sharp.Function16.class, MachineDependentFunction.Factory.getFunction("112.16").getClass());
    }

    @Test
    void parameters() throws Exception {
        Function176 f176 = new Function176();
        f176.process(message(hex("01 b0 01 01 02 00 2c")), null);
        assertEquals(1, f176.getTarget());
        assertEquals(2, f176.getParameter());
        assertEquals(0x2c, f176.getValue());
        assertArrayEquals(hex("01 b0 01 01 02 00 2c"), f176.getMessage());
        assertEquals("0.176", f176.getId());

        Function177 f177 = new Function177();
        f177.process(message(hex("01 b1 01 01 02 00 2b")), null);
        assertEquals(0x2b, f177.getValue());
        assertArrayEquals(hex("01 b1 01 01 02 00 2b"), f177.getMessage());
    }

    @Test
    void waveData() throws Exception {
        Function16 header = new Function16();
        header.process(message(hex("01 10 00 01 00 0b 0f 00 00 00 00 0b 0c")), null);
        assertEquals(0, header.getWaveNumber());
        assertEquals(Function16.PART_HEADER, header.getPart());
        assertEquals(0x0b0f, header.getLength());
        assertEquals(0, header.getLoopStart());
        assertEquals(0x0b0c, header.getLoopEnd());
        assertArrayEquals(hex("01 10 00 01 00 0b 0f 00 00 00 00 0b 0c"), header.getMessage());
    }

    @Test
    void voice() throws Exception {
        byte[] record = hex("01 00 00 02 00 20 53 48 00 00 32 00 21 e0 3e 20 3c 30 40 3f fc 20 00 20 00 40 00 30 00 10 00 20 00 3c 20 00 00 23 10 20 00 20 00 00");
        byte[] payload = new byte[5 + record.length];
        System.arraycopy(hex("01 11 00 02 2c"), 0, payload, 0, 5);
        System.arraycopy(record, 0, payload, 5, record.length);

        Function17 in = new Function17();
        in.process(message(payload), null);
        assertEquals(0, in.getVoiceNumber());
        assertEquals(Function17.PARAMETER_RECORD, in.getParameter());
        assertFalse(in.isSecond());
        assertArrayEquals(record, in.getRecord());
        assertArrayEquals(payload, in.getMessage());

        // voices 5 and 6, a pair of a built in tone
        Function17 first = new Function17();
        first.process(message(hex("01 11 05 10 06 00 00 00 02 3d 41")), null);
        assertEquals(Function17.PARAMETER_PRESET, first.getParameter());
        assertFalse(first.isSecond());
        assertEquals(2, first.getPresetBank());
        assertEquals(0x3d, first.getPresetProgram());

        Function17 second = new Function17();
        second.process(message(hex("01 11 06 10 06 02 00 00 02 3d 41")), null);
        assertTrue(second.isSecond());

        Function17 pair = new Function17();
        pair.process(message(hex("01 11 05 20 01 06")), null);
        assertEquals(Function17.PARAMETER_PAIR, pair.getParameter());
        assertEquals(6, pair.getPairVoiceNumber());
        assertArrayEquals(hex("01 11 05 20 01 06"), pair.getMessage());

        Function18 setting = new Function18();
        setting.process(message(hex("01 12 05 00 04 80 00 02 3d")), null);
        assertEquals(5, setting.getVoiceNumber());
        assertFalse(setting.isDrum());
        assertEquals(2, setting.getBank());
        assertEquals(0x3d, setting.getProgram());
        assertArrayEquals(hex("01 12 05 00 04 80 00 02 3d"), setting.getMessage());
    }

    @Test
    void channel() throws Exception {
        Function64 in = new Function64();
        in.process(message(hex("01 40 83 00 30 00 00 00 23 00 40 05 00")), null);
        assertEquals(2, in.getKind());
        assertEquals(3, in.getChannel());
        assertArrayEquals(hex("83 00 30 00 00 00 23 00 40 05 00"), in.getData());
        assertArrayEquals(hex("01 40 83 00 30 00 00 00 23 00 40 05 00"), in.getMessage());
    }

    /**
     * Runs every MFi 5 machine dependent message of the corpus through its function and
     * checks what the classes say about them: nothing is undefined, each wave's samples
     * follow its header, each voice has a wave or a built in tone, a pair's second voice
     * has no voice setting of its own and a built in tone voice is the one its setting names.
     */
    @Test
    @DisplayName("the whole corpus")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void corpus() throws Exception {
        if (!Files.exists(Paths.get("local.properties"))) return;
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(Paths.get("local.properties"))) {
            props.load(r);
        }
        Path dir = Paths.get(props.getProperty("mfi.dir", "src/test/resources"));
        System.setProperty("vavi.sound.mfi.vavi.parserLevel", props.getProperty("mfi.parserLevel", "loose"));

        int[] counts = new int[3]; // files, messages, waves
        List<String> errors = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mld")).toList()) {
                List<byte[]> mfi5 = new ArrayList<>();
                try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
                    for (Track track : VaviMfiFileFormat.readFrom(is).getSequence().getTracks()) {
                        for (MfiEvent event : track) {
                            if (event.getMessage() instanceof MachineDependentMessage m && (m.getMessage()[5] & 0xff) == 0x01) {
                                mfi5.add(m.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    continue; // a broken file is ChunkTest's business, not this one
                }
                if (mfi5.isEmpty()) continue;
                counts[0]++;

                Map<Integer, Integer> headers = new HashMap<>();
                Set<Integer> waves = new HashSet<>();
                // the factory's functions are shared, what they read is taken out as it comes
                Map<Integer, Boolean> seconds = new HashMap<>(); // of the records and the presets
                Map<Integer, List<Integer>> presets = new HashMap<>(); // bank, program
                Map<Integer, Integer> pairs = new HashMap<>();
                Map<Integer, List<Integer>> settings = new HashMap<>(); // bank, program
                for (byte[] data : mfi5) {
                    counts[1]++;
                    int function = data[6] & 0xff;
                    MachineDependentFunction f = MachineDependentFunction.Factory.getFunction("0." + function);
                    if (f instanceof UndefinedFunction) {
                        errors.add("%s: no function for 0x%02x".formatted(path.getFileName(), function));
                        continue;
                    }
                    f.process(data, null);
                    switch (f) {
                        case Function16 wave when wave.getPart() == Function16.PART_HEADER ->
                            headers.put(wave.getWaveNumber(), wave.getLength());
                        case Function16 wave -> {
                            counts[2]++;
                            Integer length = headers.get(wave.getWaveNumber());
                            if (length == null || length != wave.getLength() || wave.getLength() != wave.getWave().length) {
                                errors.add("%s: wave %d of %d bytes, header %s".formatted(path.getFileName(),
                                        wave.getWaveNumber(), wave.getWave().length, length));
                            }
                            waves.add(wave.getWaveNumber());
                        }
                        case Function17 voice when voice.getParameter() == Function17.PARAMETER_RECORD -> {
                            if (!waves.contains(voice.getVoiceNumber())) {
                                errors.add("%s: voice %d has no wave".formatted(path.getFileName(), voice.getVoiceNumber()));
                            }
                            seconds.put(voice.getVoiceNumber(), voice.isSecond());
                        }
                        case Function17 voice when voice.getParameter() == Function17.PARAMETER_PRESET -> {
                            if (waves.contains(voice.getVoiceNumber())) {
                                errors.add("%s: preset voice %d has a wave".formatted(path.getFileName(), voice.getVoiceNumber()));
                            }
                            seconds.put(voice.getVoiceNumber(), voice.isSecond());
                            presets.put(voice.getVoiceNumber(), List.of(voice.getPresetBank(), voice.getPresetProgram()));
                        }
                        case Function17 voice when voice.getParameter() == Function17.PARAMETER_PAIR ->
                            pairs.put(voice.getVoiceNumber(), voice.getPairVoiceNumber());
                        case Function18 setting -> settings.put(setting.getVoiceNumber(), List.of(setting.getBank(), setting.getProgram()));
                        default -> { }
                    }
                }
                pairs.forEach((first, second) -> {
                    if (second != first + 1 || !seconds.getOrDefault(second, false)) {
                        errors.add("%s: voice %d pairs with %d".formatted(path.getFileName(), first, second));
                    }
                });
                seconds.forEach((number, second) -> {
                    if (second && settings.containsKey(number)) {
                        errors.add("%s: second voice %d has a setting".formatted(path.getFileName(), number));
                    }
                });
                presets.forEach((number, preset) -> {
                    List<Integer> own = settings.get(seconds.get(number) ? number - 1 : number);
                    if (!preset.equals(own)) {
                        errors.add("%s: preset voice %d is %s, its setting %s".formatted(path.getFileName(), number, preset, own));
                    }
                });
            }
        }
Debug.println("mfi5 files: %d, messages: %d, waves: %d".formatted(counts[0], counts[1], counts[2]));
errors.forEach(System.err::println);
        assertEquals(List.of(), errors);
    }
}
