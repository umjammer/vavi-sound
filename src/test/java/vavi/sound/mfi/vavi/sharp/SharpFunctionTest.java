/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sharp;

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
import vavi.sound.mfi.vavi.sequencer.UndefinedFunction;
import vavi.sound.mfi.vavi.track.ChangeBankMessage;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests the Sharp machine dependent functions.
 * <p>
 * The 0xb0 / 0xb1 payloads are the ones {@code Ringtones (MLD)/DSどうぶつ_たぬきﾃﾞﾊﾟｰﾄ.mld}
 * really has, the file whose log the mission started from.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
class SharpFunctionTest {

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
        assertInstanceOf(Function16.class, MachineDependentFunction.Factory.getFunction("112.16"));
        assertInstanceOf(Function17.class, MachineDependentFunction.Factory.getFunction("112.17"));
        assertInstanceOf(Function18.class, MachineDependentFunction.Factory.getFunction("112.18"));
        assertInstanceOf(Function64.class, MachineDependentFunction.Factory.getFunction("112.64"));
        assertInstanceOf(Function144.class, MachineDependentFunction.Factory.getFunction("112.144"));
        assertInstanceOf(Function145.class, MachineDependentFunction.Factory.getFunction("112.145"));
        assertInstanceOf(Function146.class, MachineDependentFunction.Factory.getFunction("112.146"));
        assertInstanceOf(Function147.class, MachineDependentFunction.Factory.getFunction("112.147"));
        assertInstanceOf(Function160.class, MachineDependentFunction.Factory.getFunction("112.160"));
        assertInstanceOf(Function176.class, MachineDependentFunction.Factory.getFunction("112.176"));
        assertInstanceOf(Function177.class, MachineDependentFunction.Factory.getFunction("112.177"));
        // the fujitsu ones are still where they were
        assertInstanceOf(vavi.sound.mfi.vavi.fujitsu.Function176.class, MachineDependentFunction.Factory.getFunction("32.176"));
    }

    @Test
    void parameters() throws Exception {
        // what "DSどうぶつ_たぬきﾃﾞﾊﾟｰﾄ.mld" writes, the same as the Fujitsu plug in does
        Function176 f176 = new Function176();
        f176.process(message(hex("71 b0 01 00 03 01 00")), null);
        assertEquals(1, f176.getTarget());
        assertEquals(3, f176.getParameter());
        assertEquals(1, f176.getValue());
        assertArrayEquals(hex("71 b0 01 00 03 01 00"), f176.getMessage());
        assertEquals("112.176", f176.getId());

        Function177 f177 = new Function177();
        f177.process(message(hex("71 b1 01 01 02 00 01")), null);
        assertEquals(1, f177.getTarget());
        assertEquals(2, f177.getParameter());
        assertEquals(1, f177.getValue());
        assertArrayEquals(hex("71 b1 01 01 02 00 01"), f177.getMessage());
    }

    @Test
    void waveData() throws Exception {
        // "My Treasure.mld", its wave 1
        Function16 header = new Function16();
        header.process(message(hex("71 10 01 01 00 00 40 00 00 00 00 00 3d")), null);
        assertEquals(1, header.getWaveNumber());
        assertEquals(Function16.PART_HEADER, header.getPart());
        assertEquals(0x40, header.getLength());
        assertEquals(0, header.getLoopStart());
        assertEquals(0x3d, header.getLoopEnd());
        assertArrayEquals(hex("71 10 01 01 00 00 40 00 00 00 00 00 3d"), header.getMessage());

        byte[] wave = new byte[0x40];
        for (int i = 0; i < wave.length; i++) {
            wave[i] = (byte) (i * 5);
        }
        byte[] payload = new byte[6 + wave.length];
        System.arraycopy(hex("71 10 01 02 00 40"), 0, payload, 0, 6);
        System.arraycopy(wave, 0, payload, 6, wave.length);
        Function16 samples = new Function16();
        samples.process(message(payload), null);
        assertEquals(Function16.PART_SAMPLES, samples.getPart());
        assertEquals(0x40, samples.getLength());
        assertArrayEquals(wave, samples.getWave());

        Function16 out = new Function16();
        out.setWaveNumber(1);
        out.setWave(wave);
        assertArrayEquals(payload, out.getMessage());
    }

    @Test
    void voice() throws Exception {
        byte[] record = hex("01 00 01 02 00 26 4b 48 00 00 33 00 3f fc 26 40 33 30 7e 3f fc 20 00 20 00 40 00 30 00 10 00 20 00 3c 20 00 00 23 00 00 00 00 00 00");
        byte[] payload = new byte[5 + record.length];
        System.arraycopy(hex("71 11 00 02 2c"), 0, payload, 0, 5);
        System.arraycopy(record, 0, payload, 5, record.length);

        Function17 in = new Function17();
        in.process(message(payload), null);
        assertEquals(0, in.getVoiceNumber());
        assertEquals(2, in.getType());
        assertArrayEquals(record, in.getRecord());
        assertArrayEquals(payload, in.getMessage());

        // "ｻﾎﾞﾃﾝの花.mld", a drum voice and a melody voice
        Function18 drum = new Function18();
        drum.process(message(hex("71 12 00 00 04 80 01 02 03")), null);
        assertEquals(0, drum.getVoiceNumber());
        assertEquals(Function18.PARAMETER_PROGRAM, drum.getParameter());
        assertTrue(drum.isDrum());
        assertEquals(2, drum.getBank());
        assertEquals(3, drum.getProgram());

        Function18 melody = new Function18();
        melody.process(message(hex("71 12 02 00 04 80 00 03 01")), null);
        assertFalse(melody.isDrum());
        assertEquals(3, melody.getBank());
        assertEquals(1, melody.getProgram());

        Function18 other = new Function18();
        other.process(message(hex("71 12 02 10 01 80")), null);
        assertEquals(Function18.PARAMETER_10, other.getParameter());
        assertArrayEquals(hex("80"), other.getValue());
        assertArrayEquals(hex("71 12 02 10 01 80"), other.getMessage());

        Function18 out = new Function18();
        out.setVoiceNumber(2);
        out.setProgram(3, false, 1);
        assertArrayEquals(hex("71 12 02 00 04 80 00 03 01"), out.getMessage());
    }

    @Test
    void unidentified() throws Exception {
        Function64 in = new Function64();
        in.process(message(hex("71 40 80 00 0d 00 00 00 20 0c 00 08 04")), null);
        assertArrayEquals(hex("80 00 0d 00 00 00 20 0c 00 08 04"), in.getData());
        assertArrayEquals(hex("71 40 80 00 0d 00 00 00 20 0c 00 08 04"), in.getMessage());
    }

    /**
     * Runs every Sharp machine dependent message of the corpus through its function.
     * <p>
     * Checks the wave table voices the way FujitsuFunctionTest checks Fujitsu's, both the
     * 0x9# ones (the MFi 4.0 plug in, shared with Fujitsu) and the 0x1# ones: each wave's
     * samples follow its header, each voice has its wave and each voice setting names a
     * bank the song selects.
     * </p>
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

        int[] counts = new int[4]; // files, messages, waves, voice settings
        List<String> errors = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mld")).toList()) {
                List<byte[]> sharp = new ArrayList<>();
                Set<Integer> banks = new HashSet<>();
                try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
                    for (Track track : VaviMfiFileFormat.readFrom(is).getSequence().getTracks()) {
                        for (MfiEvent event : track) {
                            switch (event.getMessage()) {
                                case MachineDependentMessage m when m.getVendor() == 0x70 -> sharp.add(m.getMessage());
                                case ChangeBankMessage b -> banks.add(b.getBank());
                                default -> { }
                            }
                        }
                    }
                } catch (Exception e) {
                    continue; // a broken file is ChunkTest's business, not this one
                }
                if (sharp.isEmpty()) continue;
                counts[0]++;

                int nextOffset = 0;
                Map<Integer, Integer> headers = new HashMap<>();
                Set<Integer> waves = new HashSet<>();
                Set<Integer> voices = new HashSet<>();
                for (byte[] data : sharp) {
                    counts[1]++;
                    int function = data[6] & 0xff;
                    if (function >= 0x81 && function <= 0x8f) {
                        continue; // the adpcm ones were there before, and they would play
                    }
                    MachineDependentFunction f = MachineDependentFunction.Factory.getFunction("112." + function);
                    if (f instanceof UndefinedFunction) {
                        errors.add("%s: no function for 0x%02x".formatted(path.getFileName(), function));
                        continue;
                    }
                    f.process(data, null);
                    switch (f) {
                        case Function16 wave when wave.getPart() == Function16.PART_HEADER -> {
                            headers.put(wave.getWaveNumber(), wave.getLength());
                            // an empty wave (3 of them) has its loop at 0 ~ 0
                            if (wave.getLoopStart() > wave.getLoopEnd() || (wave.getLength() > 0 && wave.getLoopEnd() >= wave.getLength())) {
                                errors.add("%s: wave %d loop %d ~ %d outside 0 ~ %d".formatted(path.getFileName(),
                                        wave.getWaveNumber(), wave.getLoopStart(), wave.getLoopEnd(), wave.getLength()));
                            }
                        }
                        case Function16 wave -> {
                            counts[2]++;
                            Integer length = headers.get(wave.getWaveNumber());
                            if (length == null || length != wave.getLength() || wave.getLength() != wave.getWave().length) {
                                errors.add("%s: wave %d of %d bytes, header %s".formatted(path.getFileName(),
                                        wave.getWaveNumber(), wave.getWave().length, length));
                            }
                            waves.add(wave.getWaveNumber());
                        }
                        case Function17 voice -> {
                            if (!waves.contains(voice.getVoiceNumber())) {
                                errors.add("%s: voice %d has no wave".formatted(path.getFileName(), voice.getVoiceNumber()));
                            }
                            voices.add(voice.getVoiceNumber());
                        }
                        case Function18 setting -> {
                            counts[3]++;
                            if (!voices.contains(setting.getVoiceNumber())) {
                                errors.add("%s: setting for voice %d, registered %s".formatted(path.getFileName(), setting.getVoiceNumber(), voices));
                            }
                            if (setting.getParameter() == Function18.PARAMETER_PROGRAM && !banks.contains(setting.getBank())) {
                                errors.add("%s: setting bank %d, the song selects %s".formatted(path.getFileName(), setting.getBank(), banks));
                            }
                        }
                        case Function144 wave -> {
                            if (wave.getOffset() != nextOffset) {
                                errors.add("%s: 0x90 wave at %04x, expected %04x".formatted(path.getFileName(), wave.getOffset(), nextOffset));
                            }
                            nextOffset = wave.getNextOffset();
                        }
                        case Function146 voice -> {
                            byte[] record = voice.getVoiceParameter();
                            if ((record[0] & 0xff) != 0xff || (record[1] & 0xff) != (0xc0 | voice.getVoiceNumber())) {
                                errors.add("%s: 0x92 voice %d record starts %02x %02x"
                                        .formatted(path.getFileName(), voice.getVoiceNumber(), record[0], record[1]));
                            }
                        }
                        default -> { }
                    }
                }
            }
        }
Debug.println("sharp files: %d, messages: %d, waves: %d, voice settings: %d"
        .formatted(counts[0], counts[1], counts[2], counts[3]));
errors.forEach(System.err::println);
        assertEquals(List.of(), errors);
    }
}
