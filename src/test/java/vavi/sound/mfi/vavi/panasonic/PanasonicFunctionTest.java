/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.panasonic;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.Track;
import vavi.sound.mfi.vavi.VaviMfiFileFormat;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.sequencer.UndefinedFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


/**
 * Tests the Panasonic machine dependent functions this artifact has.
 * <p>
 * The payloads are the ones {@code Ringtones (MLD)/幸せをありがとう.mld} really has, the
 * file whose log the mission started from.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
class PanasonicFunctionTest {

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
        assertInstanceOf(Function16.class, MachineDependentFunction.Factory.getFunction("64.16"));
        assertInstanceOf(Function17.class, MachineDependentFunction.Factory.getFunction("64.17"));
        assertInstanceOf(Function18.class, MachineDependentFunction.Factory.getFunction("64.18"));
        assertInstanceOf(Function144.class, MachineDependentFunction.Factory.getFunction("64.144"));
        assertInstanceOf(Function145.class, MachineDependentFunction.Factory.getFunction("64.145"));
        assertInstanceOf(Function146.class, MachineDependentFunction.Factory.getFunction("64.146"));
        assertInstanceOf(Function147.class, MachineDependentFunction.Factory.getFunction("64.147"));
        assertInstanceOf(Function148.class, MachineDependentFunction.Factory.getFunction("64.148"));
        assertInstanceOf(Function160.class, MachineDependentFunction.Factory.getFunction("64.160"));
        assertInstanceOf(Function176.class, MachineDependentFunction.Factory.getFunction("64.176"));
        assertInstanceOf(Function177.class, MachineDependentFunction.Factory.getFunction("64.177"));
        assertInstanceOf(Function240.class, MachineDependentFunction.Factory.getFunction("64.240"));
    }

    @Test
    void voiceBlock() throws Exception {
        Function148 start = new Function148();
        start.process(message(hex("41 94 00")), null);
        assertEquals(0, start.getValue());
        assertArrayEquals(hex("41 94 00"), start.getMessage());

        // the second voice of the mission file
        byte[] payload = hex("41 91 0a 0a 82 6f 00 00 00 7a 60 61 02 6f");
        Function145 param = new Function145();
        param.process(message(payload), null);
        assertEquals(1, param.getWaveNumber());
        // the 0x90 before it lands at 0x09bc
        assertEquals(0x09bc, param.getStartAddress());
        assertEquals(0, param.getLoopStart());
        assertEquals(0x7a, param.getLoopEnd());
        assertArrayEquals(payload, param.getMessage());

        Function147 f147 = new Function147();
        f147.process(message(hex("41 93 05 01 02 00 03")), null);
        assertEquals(5, f147.getTarget());
        assertEquals(2, f147.getParameter());
        assertEquals(3, f147.getValue());
        assertArrayEquals(hex("41 93 05 01 02 00 03"), f147.getMessage());

        Function176 f176 = new Function176();
        f176.process(message(hex("41 b0 01 00 02 02 00")), null);
        assertEquals(2, f176.getValue());
        assertEquals("64.176", f176.getId());
    }

    @Test
    void waveTable() throws Exception {
        Function18 setting = new Function18();
        setting.process(message(hex("41 12 05 00 04 80 01 02 03")), null);
        assertEquals(5, setting.getVoiceNumber());
        assertEquals(2, setting.getBank());
        assertEquals(3, setting.getProgram());
        assertArrayEquals(hex("41 12 05 00 04 80 01 02 03"), setting.getMessage());
        assertEquals("64.18", setting.getId());
    }

    @Test
    void unidentified() throws Exception {
        Function240 in = new Function240();
        in.process(message(hex("41 f0 80")), null);
        assertArrayEquals(hex("80"), in.getData());
        assertArrayEquals(hex("41 f0 80"), in.getMessage());
    }

    /**
     * Runs every Panasonic machine dependent message of the corpus through its function
     * and checks the wave table voice blocks as FujitsuFunctionTest does: each 0x91 plays
     * the 0x90 of its own voice and loops inside it, each 0x92 record carries its voice
     * number and each 0x94 comes right before a 0x90.
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

        int[] counts = new int[3]; // files, messages, voices
        List<String> errors = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mld")).toList()) {
                List<byte[]> panasonic = new ArrayList<>();
                try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
                    for (Track track : VaviMfiFileFormat.readFrom(is).getSequence().getTracks()) {
                        for (MfiEvent event : track) {
                            if (event.getMessage() instanceof MachineDependentMessage m && m.getVendor() == 0x40) {
                                panasonic.add(m.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    continue; // a broken file is ChunkTest's business, not this one
                }
                if (panasonic.isEmpty()) continue;
                counts[0]++;

                List<Integer> waveOffsets = new ArrayList<>();
                List<Integer> waveLengths = new ArrayList<>();
                int previous = -1;
                for (byte[] data : panasonic) {
                    counts[1]++;
                    int function = data[6] & 0xff;
                    if (previous == 0x94 && function != 0x90) {
                        errors.add("%s: 0x94 followed by 0x%02x".formatted(path.getFileName(), function));
                    }
                    previous = function;
                    if (function >= 0x81 && function <= 0x8f) {
                        continue; // vavi-sound-nda's, and they would play
                    }
                    MachineDependentFunction f = MachineDependentFunction.Factory.getFunction("64." + function);
                    if (f instanceof UndefinedFunction) {
                        errors.add("%s: no function for 0x%02x".formatted(path.getFileName(), function));
                        continue;
                    }
                    f.process(data, null);
                    switch (f) {
                        case Function144 wave -> {
                            waveOffsets.add(wave.getOffset());
                            waveLengths.add(wave.getWave().length);
                        }
                        case Function145 param -> {
                            // a file may lack a wave (one does), so match by the address
                            int n = waveOffsets.indexOf(param.getStartAddress());
                            if (n < 0) {
                                errors.add("%s: wave %d starts at %04x, no 0x90 there %s"
                                        .formatted(path.getFileName(), param.getWaveNumber(), param.getStartAddress(), waveOffsets));
                            } else if (!(param.getLoopStart() <= param.getLoopEnd() && param.getLoopEnd() < waveLengths.get(n))) {
                                errors.add("%s: wave %d loop %04x ~ %04x outside 0 ~ %04x"
                                        .formatted(path.getFileName(), param.getWaveNumber(), param.getLoopStart(), param.getLoopEnd(), waveLengths.get(n)));
                            }
                        }
                        case Function146 voice -> {
                            counts[2]++;
                            byte[] record = voice.getVoiceParameter();
                            if ((record[0] & 0xff) != 0xff || (record[1] & 0xff) != (0xc0 | voice.getVoiceNumber())) {
                                errors.add("%s: voice %d record starts %02x %02x"
                                        .formatted(path.getFileName(), voice.getVoiceNumber(), record[0], record[1]));
                            }
                        }
                        default -> { }
                    }
                }
            }
        }
Debug.println("panasonic files: %d, messages: %d, wave table voices: %d".formatted(counts[0], counts[1], counts[2]));
errors.forEach(System.err::println);
        assertEquals(List.of(), errors);
    }
}
