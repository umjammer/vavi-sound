/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.fujitsu;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
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
import vavi.sound.mfi.vavi.track.ChangeVoiceMessage;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests the MFi 4.0 Fujitsu machine dependent functions.
 * <p>
 * The payloads are the ones
 * {@code Ringtones from Cuebus F901iC/68_8981100010347092588F.MLD} really has, the
 * file whose log the mission started from.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
class FujitsuFunctionTest {

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
        assertInstanceOf(Function1.class, MachineDependentFunction.Factory.getFunction("32.1"));
        assertInstanceOf(Function2.class, MachineDependentFunction.Factory.getFunction("32.2"));
        assertInstanceOf(Function130.class, MachineDependentFunction.Factory.getFunction("32.130"));
        assertInstanceOf(Function144.class, MachineDependentFunction.Factory.getFunction("32.144"));
        assertInstanceOf(Function145.class, MachineDependentFunction.Factory.getFunction("32.145"));
        assertInstanceOf(Function146.class, MachineDependentFunction.Factory.getFunction("32.146"));
        assertInstanceOf(Function147.class, MachineDependentFunction.Factory.getFunction("32.147"));
        assertInstanceOf(Function160.class, MachineDependentFunction.Factory.getFunction("32.160"));
        assertInstanceOf(Function161.class, MachineDependentFunction.Factory.getFunction("32.161"));
        assertInstanceOf(Function176.class, MachineDependentFunction.Factory.getFunction("32.176"));
        assertInstanceOf(Function177.class, MachineDependentFunction.Factory.getFunction("32.177"));
    }

    @Test
    void panpot() throws Exception {
        Function130 in = new Function130();
        in.process(message(hex("21 82 20")), null);
        assertEquals(0, in.getChannel());
        assertEquals(0x20, in.getPanpot());

        Function130 out = new Function130();
        out.setChannel(1);
        out.setPanpot(0x3f);
        assertArrayEquals(hex("21 82 7f"), out.getMessage());
    }

    @Test
    void waveData() throws Exception {
        // 21 90 <offset:2> <length:2> <wave ...>, the first wave of the mission file is 0x084a long
        byte[] wave = new byte[0x084a];
        for (int i = 0; i < wave.length; i++) {
            wave[i] = (byte) (i * 3);
        }
        byte[] payload = new byte[6 + wave.length];
        System.arraycopy(hex("21 90 00 00 08 4a"), 0, payload, 0, 6);
        System.arraycopy(wave, 0, payload, 6, wave.length);

        Function144 in = new Function144();
        in.process(message(payload), null);
        assertEquals(0, in.getOffset());
        assertArrayEquals(wave, in.getWave());
        // 0x084a rounded up to a multiple of 4
        assertEquals(0x084c, in.getNextOffset());

        Function144 out = new Function144();
        out.setOffset(0x0c90);
        out.setWave(wave);
        assertArrayEquals(waveMessage(0x0c90, wave), out.getMessage());
    }

    private static byte[] waveMessage(int offset, byte[] wave) {
        byte[] payload = new byte[6 + wave.length];
        payload[0] = 0x21;
        payload[1] = (byte) 0x90;
        payload[2] = (byte) (offset >> 8);
        payload[3] = (byte) offset;
        payload[4] = (byte) (wave.length >> 8);
        payload[5] = (byte) wave.length;
        System.arraycopy(wave, 0, payload, 6, wave.length);
        return payload;
    }

    @Test
    void waveParameter() throws Exception {
        byte[] payload = hex("21 91 00 0a 80 00 08 40 08 48 f8 33 00 b9");

        Function145 in = new Function145();
        in.process(message(payload), null);
        assertEquals(0, in.getWaveNumber());
        assertEquals(0x8000, in.getRawStartAddress());
        assertEquals(0, in.getStartAddress());
        assertEquals(0x0840, in.getLoopStart());
        assertEquals(0x0848, in.getLoopEnd());
        assertEquals(0x00b9_f833, in.getRawPitch());
        assertEquals(0.7264435, in.getPitch(), 1e-7);

        // the second wave of "ピアノ協奏曲第１番.mld", whose 0x90 lands at 0x0c90
        byte[] payload2 = hex("21 91 0a 0a 83 24 00 00 01 50 89 fa 01 ae");
        Function145 in2 = new Function145();
        in2.process(message(payload2), null);
        assertEquals(1, in2.getWaveNumber());
        assertEquals(0x0c90, in2.getStartAddress());

        // round trip
        Function145 out = new Function145();
        out.setWaveNumber(0);
        out.setStartAddress(0);
        out.setLoopStart(0x0840);
        out.setLoopEnd(0x0848);
        out.setPitch(in.getPitch());
        assertArrayEquals(payload, out.getMessage());
    }

    @Test
    void voiceParameter() throws Exception {
        byte[] payload = hex("21 92 00 20 ff c0 3c 00 00 2c 00 00 00 00 00 00 00 80 80 80 80 7f 00 00 00 80 80 80 80 00 00 0f 80 7f 7f 00");

        Function146 in = new Function146();
        in.process(message(payload), null);
        assertEquals(0, in.getVoiceNumber());
        assertEquals(0x3c, in.getKeyNumber());
        assertEquals(Function146.RECORD_LENGTH, in.getVoiceParameter().length);
        assertArrayEquals(Arrays.copyOfRange(payload, 4, payload.length), in.getVoiceParameter());

        Function146 out = new Function146();
        out.setVoiceNumber(0);
        out.setVoiceParameter(in.getVoiceParameter());
        assertArrayEquals(payload, out.getMessage());
    }

    @Test
    void parameters() throws Exception {
        // 21 93 <voice> <width> <parameter> <value:2>
        Function147 f147 = new Function147();
        f147.process(message(hex("21 93 00 01 02 00 05")), null);
        assertEquals(0, f147.getTarget());
        assertEquals(ParameterFunction.WIDTH_16, f147.getWidth());
        assertEquals(2, f147.getParameter());
        assertEquals(5, f147.getValue());

        // the width 0 form, the value is the first byte
        Function147 f147b = new Function147();
        f147b.process(message(hex("21 93 01 00 03 11 00")), null);
        assertEquals(1, f147b.getTarget());
        assertEquals(ParameterFunction.WIDTH_8, f147b.getWidth());
        assertEquals(3, f147b.getParameter());
        assertEquals(0x11, f147b.getValue());
        assertEquals(0x1100, f147b.getRawValue());

        Function176 f176 = new Function176();
        f176.process(message(hex("21 b0 01 00 03 02 00")), null);
        assertEquals(1, f176.getTarget());
        assertEquals(2, f176.getValue());

        Function177 f177 = new Function177();
        f177.process(message(hex("21 b1 01 01 02 00 01")), null);
        assertEquals(1, f177.getTarget());
        assertEquals(1, f177.getValue());

        // round trip, both widths
        Function176 out = new Function176();
        out.setTarget(1);
        out.setWidth(ParameterFunction.WIDTH_8);
        out.setParameter(3);
        out.setValue(0x3f);
        assertArrayEquals(hex("21 b0 01 00 03 3f 00"), out.getMessage());

        Function177 out2 = new Function177();
        out2.setTarget(1);
        out2.setWidth(ParameterFunction.WIDTH_16);
        out2.setParameter(2);
        out2.setValue(1);
        assertArrayEquals(hex("21 b1 01 01 02 00 01"), out2.getMessage());
    }

    @Test
    void fmVoice() throws Exception {
        // the drum voice "09 SuperSonicRacing.mld" registers first
        byte[] payload = hex("21 01 02 41 00 82 03 78 01 00 0b a0 34 a2 00 78 a3 00 a6");

        Function1 in = new Function1();
        in.process(message(payload), null);
        assertEquals(Function1.SUB_VOICE_2, in.getSubFunction());
        assertTrue(in.isVoice());
        assertEquals(0x41, in.getKeyNumber());
        assertEquals(0, in.getIndex());
        assertEquals(2, in.getBank());
        assertTrue(in.isDrum());
        assertEquals(3, in.getProgram());
        assertEquals(Function1.VOICE_LENGTH, in.getVoice().length);
        assertArrayEquals(hex("00 0b a0 34 a2"), in.getOperator(0));
        assertArrayEquals(hex("00 78 a3 00 a6"), in.getOperator(1));

        // a melody voice of the same file, program 48 - one the song really selects
        Function1 melody = new Function1();
        melody.process(message(hex("21 01 02 00 05 02 30 40 01 14 41 b1 6e 80 2c 72 70 04 e4")), null);
        assertEquals(0, melody.getKeyNumber());
        assertFalse(melody.isDrum());
        assertEquals(2, melody.getBank());
        assertEquals(0x30, melody.getProgram());

        // the other subs keep their payload as it is
        Function1 volume = new Function1();
        volume.process(message(hex("21 01 06 3f")), null);
        assertEquals(Function1.SUB_VOLUME, volume.getSubFunction());
        assertFalse(volume.isVoice());
        assertArrayEquals(hex("3f"), volume.getData());

        Function1 out = new Function1();
        out.setSubFunction(Function1.SUB_VOICE_2);
        out.setData(in.getData());
        assertArrayEquals(payload, out.getMessage());
    }

    @Test
    void unidentified() throws Exception {
        Function2 in = new Function2();
        in.process(message(hex("21 02 62 00")), null);
        assertArrayEquals(hex("62 00"), in.getData());

        Function2 out = new Function2();
        out.setData(hex("02 00"));
        assertArrayEquals(hex("21 02 02 00"), out.getMessage());
    }

    @Test
    void settings() throws Exception {
        Function160 f160 = new Function160();
        f160.process(message(hex("21 a0 01")), null);
        assertEquals(1, f160.getValue());

        Function161 f161 = new Function161();
        f161.process(message(hex("21 a1 90")), null);
        assertEquals(2, f161.getVoice());
        assertEquals(0x10, f161.getValue());

        Function161 out = new Function161();
        out.setVoice(3);
        out.setValue(0);
        assertArrayEquals(hex("21 a1 c0"), out.getMessage());
    }

    /**
     * Runs every Fujitsu machine dependent message of the corpus through its function.
     * <p>
     * Each 0x90 says where its wave lands and each 0x91 which wave it plays, so the two
     * can be checked against each other, and the same goes for the record offsets of
     * 0x91 / 0x92 against the order the voices come in - nothing else tells whether the
     * guessed layouts hold.
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

        int[] counts = new int[5]; // files, messages, waves, wave table voices, fm voices
        int melodyMisses = 0;
        List<String> errors = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mld")).toList()) {
                List<byte[]> fujitsu = new ArrayList<>();
                Set<Integer> banks = new TreeSet<>();
                Set<Integer> programs = new TreeSet<>();
                try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
                    for (Track track : VaviMfiFileFormat.readFrom(is).getSequence().getTracks()) {
                        for (MfiEvent event : track) {
                            switch (event.getMessage()) {
                                case MachineDependentMessage m when m.getVendor() == 0x20 -> fujitsu.add(m.getMessage());
                                case ChangeBankMessage b -> banks.add(b.getBank());
                                case ChangeVoiceMessage v -> programs.add(v.getProgram());
                                default -> { }
                            }
                        }
                    }
                } catch (Exception e) {
                    continue; // a broken file is ChunkTest's business, not this one
                }
                if (fujitsu.isEmpty()) continue;
                counts[0]++;

                int nextOffset = 0;
                List<Integer> waveOffsets = new ArrayList<>();
                List<Integer> waveLengths = new ArrayList<>();
                for (byte[] data : fujitsu) {
                    counts[1]++;
                    int function = data[6] & 0xff;
                    MachineDependentFunction f = MachineDependentFunction.Factory.getFunction("32." + function);
                    if (f instanceof UndefinedFunction) {
                        errors.add("%s: no function for 0x%02x".formatted(path.getFileName(), function));
                        continue;
                    }
                    f.process(data, null);
                    switch (f) {
                        case Function144 wave -> {
                            counts[2]++;
                            if (wave.getOffset() != nextOffset) {
                                errors.add("%s: wave at %04x, expected %04x".formatted(path.getFileName(), wave.getOffset(), nextOffset));
                            }
                            waveOffsets.add(wave.getOffset());
                            waveLengths.add(wave.getWave().length);
                            nextOffset = wave.getNextOffset();
                        }
                        case Function145 param -> {
                            int n = param.getWaveNumber();
                            if (n < waveOffsets.size()) {
                                if (param.getStartAddress() != waveOffsets.get(n)) {
                                    errors.add("%s: wave %d starts at %04x, the 0x90 put it at %04x"
                                            .formatted(path.getFileName(), n, param.getStartAddress(), waveOffsets.get(n)));
                                }
                                if (!(param.getLoopStart() <= param.getLoopEnd() && param.getLoopEnd() < waveLengths.get(n))) {
                                    errors.add("%s: wave %d loop %04x ~ %04x outside 0 ~ %04x"
                                            .formatted(path.getFileName(), n, param.getLoopStart(), param.getLoopEnd(), waveLengths.get(n)));
                                }
                            }
                        }
                        case Function146 voice -> {
                            counts[3]++;
                            byte[] record = voice.getVoiceParameter();
                            if ((record[0] & 0xff) != 0xff || (record[1] & 0xff) != (0xc0 | voice.getVoiceNumber())) {
                                errors.add("%s: voice %d record starts %02x %02x"
                                        .formatted(path.getFileName(), voice.getVoiceNumber(), record[0], record[1]));
                            }
                        }
                        case Function1 voice when voice.isVoice() -> {
                            counts[4]++;
                            if (!banks.contains(voice.getBank())) {
                                errors.add("%s: voice bank %d, the song selects %s"
                                        .formatted(path.getFileName(), voice.getBank(), banks));
                            }
                            // a drum voice's program is the kit index, not a ChangeVoice program
                            if (!voice.isDrum() && !programs.contains(voice.getProgram())) {
                                melodyMisses++;
                            }
                            if (voice.isDrum() != (voice.getKeyNumber() != 0)) {
                                errors.add("%s: key %d with drum %b"
                                        .formatted(path.getFileName(), voice.getKeyNumber(), voice.isDrum()));
                            }
                        }
                        default -> { }
                    }
                }
            }
        }
Debug.println("fujitsu files: %d, messages: %d, waves: %d, wave table voices: %d, fm voices: %d"
        .formatted(counts[0], counts[1], counts[2], counts[3], counts[4]));
Debug.println("fm melody voices whose program the song never selects: " + melodyMisses);
errors.forEach(System.err::println);
        assertEquals(List.of(), errors);
    }
}
