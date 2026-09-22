/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sony;

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
import vavi.sound.mfi.vavi.track.ChangeVoiceMessage;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests the Sony machine dependent functions this artifact has.
 * <p>
 * The payloads are the ones the corpus really has, the pitch bends those of
 * {@code upload_melody_2003-08-26-12-42-00-000-00000/0_A645_C194_01_NULL_01_so16.mld},
 * the file whose log the mission started from.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
class SonyFunctionTest {

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
        assertInstanceOf(Function16.class, MachineDependentFunction.Factory.getFunction("48.16"));
        assertInstanceOf(Function48.class, MachineDependentFunction.Factory.getFunction("48.48"));
        assertInstanceOf(Function49.class, MachineDependentFunction.Factory.getFunction("48.49"));
        assertInstanceOf(Function50.class, MachineDependentFunction.Factory.getFunction("48.50"));
        assertInstanceOf(Function176.class, MachineDependentFunction.Factory.getFunction("48.176"));
        assertInstanceOf(Function177.class, MachineDependentFunction.Factory.getFunction("48.177"));
        for (int f = 0xe0; f <= 0xe3; f++) {
            assertInstanceOf(PitchBendFunction.class, MachineDependentFunction.Factory.getFunction("48." + f));
            assertInstanceOf(PitchBendFunction.class, MachineDependentFunction.Factory.getFunction("48." + (f + 8)));
        }
        assertInstanceOf(Function239.class, MachineDependentFunction.Factory.getFunction("48.239"));
    }

    @Test
    void pitchBend() throws Exception {
        // 31 e0 71 / 31 e8 2e, the third point of the first scoop of the mission file
        Function224 lsb = new Function224();
        lsb.process(message(hex("31 e0 71")), null);
        assertEquals(0, lsb.getVoice());
        assertEquals(0x71, lsb.getLsb());
        assertEquals(-1, lsb.getMsb());
        assertThrows(IllegalStateException.class, () -> lsb.getValue(0));

        Function232 msb = new Function232();
        msb.process(message(hex("31 e8 2e")), null);
        assertEquals(0, msb.getVoice());
        assertEquals(-1, msb.getLsb());
        assertEquals(0x2e, msb.getMsb());
        // 6001, the NEC file of the same song has 6000 there
        assertEquals(6001, msb.getValue(lsb.getLsb()));

        // at rest
        Function233 rest = new Function233();
        rest.process(message(hex("31 e9 40")), null);
        assertEquals(1, rest.getVoice());
        assertEquals(PitchBendFunction.CENTER, rest.getValue(0));

        // the two byte form carries both halves
        Function235 both = new Function235();
        both.process(message(hex("31 eb 2b 35")), null);
        assertEquals(3, both.getVoice());
        assertEquals((0x35 << 7) | 0x2b, both.getValue(0x7f));

        // round trip
        Function227 out = new Function227();
        out.setValue(0x71);
        assertArrayEquals(hex("31 e3 71"), out.getMessage());
        Function234 out2 = new Function234();
        out2.setValue(0x40);
        assertArrayEquals(hex("31 ea 40"), out2.getMessage());
        out2.setValue14((0x35 << 7) | 0x2b);
        assertArrayEquals(hex("31 ea 2b 35"), out2.getMessage());
    }

    @Test
    void pitchBendRange() throws Exception {
        Function239 in = new Function239();
        in.process(message(hex("31 ef 62")), null);
        assertEquals(3, in.getVoice());
        assertEquals(2, in.getRange());

        in.process(message(hex("31 ef 4c")), null);
        assertEquals(2, in.getVoice());
        assertEquals(12, in.getRange());

        Function239 out = new Function239();
        out.setVoice(1);
        out.setRange(2);
        assertArrayEquals(hex("31 ef 22"), out.getMessage());
    }

    @Test
    void maExtension() throws Exception {
        // a 10000Hz drum WT tone, bank 4 (drum), note key 0
        byte[] wt = hex("31 10 05 84 00 27 10 79 00 08 f0 f0 10 00 00 00 03 a9 03 a9 80");
        Function16 tone = new Function16();
        tone.process(message(wt), null);
        assertEquals(Function16.SUB_WT_TONE, tone.getSubFunction());
        assertEquals(4, tone.getBank());
        assertTrue(tone.isDrum());
        assertEquals(0, tone.getProgram());
        assertEquals(Function16.WT_VOICE, tone.getVoice().length);
        assertArrayEquals(wt, tone.getMessage());

        // a 4 operator FM tone, the first of "自由[ﾘｱﾙﾊﾞｰｼﾞｮﾝ].mld"
        Function16 fm = new Function16();
        fm.process(message(hex("31 10 04 06 19 00 79 45 13 52 ff 5d 00 c0 02 23 42 ff 3a 03 10 60 02 60 ff b5 00 50 00 53 52 ff 2e 01 10 18")), null);
        assertEquals(Function16.SUB_FM_TONE, fm.getSubFunction());
        assertEquals(6, fm.getBank());
        assertFalse(fm.isDrum());
        assertEquals(0x19, fm.getProgram());
        assertEquals(Function16.FM4_VOICE, fm.getVoice().length);

        // Hold1 on channel 2
        Function16 hold = new Function16();
        hold.process(message(hex("31 10 8c 78")), null);
        assertEquals(Function16.SUB_HOLD1, hold.getSubFunction());
        assertEquals(2, hold.getChannel());
        assertEquals(0x78, hold.getValue());

        // the head of track 0
        Function16 head = new Function16();
        head.process(message(hex("31 10 11 01")), null);
        assertEquals(Function16.SUB_FM_MODE, head.getSubFunction());
        assertEquals(1, head.getValue());

        Function16 out = new Function16();
        out.setChannel(3);
        out.setSubFunction(Function16.SUB_HOLD1);
        out.setData(hex("7f"));
        assertArrayEquals(hex("31 10 cc 7f"), out.getMessage());
    }

    @Test
    void stream() throws Exception {
        // the header of the stream the corpus has in 74 files, 8000Hz mono
        byte[] adpcm = hex("80 80 80 80 80 80 18 89 81 08 90 01");
        byte[] payload = new byte[7 + adpcm.length];
        System.arraycopy(hex("31 10 07 00 01 1f 40"), 0, payload, 0, 7);
        System.arraycopy(adpcm, 0, payload, 7, adpcm.length);

        Function16 wave = new Function16();
        wave.process(message(payload), null);
        assertEquals(Function16.SUB_STREAM_WAVE, wave.getSubFunction());
        assertEquals(0, wave.getStreamNumber());
        assertTrue(wave.isMono());
        assertEquals(8000, wave.getSamplingRate());
        assertArrayEquals(adpcm, wave.getWave());

        Function16 on = new Function16();
        on.setSubFunction(Function16.SUB_STREAM_ON);
        on.setData(hex("00 7f"));
        assertArrayEquals(hex("31 10 09 00 7f"), on.getMessage());
    }

    @Test
    void waveTable() throws Exception {
        // the Sharp message under the Sony vendor byte and 0x20 higher
        Function48 wave = new Function48();
        wave.process(message(hex("31 30 03 01 00 02 ee 00 02 bd 00 02 eb")), null);
        assertEquals(3, wave.getWaveNumber());
        assertEquals(0x02ee, wave.getLength());
        assertEquals(0x02bd, wave.getLoopStart());
        assertEquals(0x02eb, wave.getLoopEnd());
        assertArrayEquals(hex("31 30 03 01 00 02 ee 00 02 bd 00 02 eb"), wave.getMessage());
        assertEquals("48.48", wave.getId());

        Function50 setting = new Function50();
        setting.process(message(hex("31 32 04 00 04 80 01 02 05")), null);
        assertEquals(4, setting.getVoiceNumber());
        assertTrue(setting.isDrum());
        assertEquals(2, setting.getBank());
        assertEquals(5, setting.getProgram());
        assertArrayEquals(hex("31 32 04 00 04 80 01 02 05"), setting.getMessage());
    }

    @Test
    void parameters() throws Exception {
        Function177 f177 = new Function177();
        f177.process(message(hex("31 b1 01 01 02 00 01")), null);
        assertEquals(1, f177.getTarget());
        assertEquals(2, f177.getParameter());
        assertEquals(1, f177.getValue());
        assertArrayEquals(hex("31 b1 01 01 02 00 01"), f177.getMessage());
        assertEquals("48.177", f177.getId());
    }

    /**
     * Runs every Sony machine dependent message of the corpus through its function.
     * <p>
     * Only the checks that tell whether a guessed layout holds are made: that an LSB
     * is always committed by the MSB of the same voice, that a tone names a bank and a
     * program the song selects, that a WT tone's own wave and a StreamOn's stream were
     * sent, and that every wave table voice has its wave.
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

        int[] counts = new int[5]; // files, messages, pitch bends, tones, wave table voices
        List<String> errors = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mld")).toList()) {
                List<List<byte[]>> sony = new ArrayList<>();
                Set<Integer> banks = new HashSet<>();
                Set<Integer> programs = new HashSet<>();
                try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
                    for (Track track : VaviMfiFileFormat.readFrom(is).getSequence().getTracks()) {
                        List<byte[]> messages = new ArrayList<>();
                        for (MfiEvent event : track) {
                            switch (event.getMessage()) {
                                case MachineDependentMessage m when m.getVendor() == 0x30 -> messages.add(m.getMessage());
                                case ChangeBankMessage b -> banks.add(b.getBank());
                                case ChangeVoiceMessage v -> programs.add(v.getProgram());
                                default -> { }
                            }
                        }
                        sony.add(messages);
                    }
                } catch (Exception e) {
                    continue; // a broken file is ChunkTest's business, not this one
                }
                if (sony.stream().allMatch(List::isEmpty)) continue;
                counts[0]++;

                Set<Integer> waveIds = new HashSet<>();
                Set<Integer> streams = new HashSet<>();
                Set<Integer> tableWaves = new HashSet<>();
                List<byte[]> wtVoices = new ArrayList<>();
                List<Integer> streamOns = new ArrayList<>();
                for (List<byte[]> track : sony) {
                    Map<Integer, Integer> pendingLsb = new HashMap<>();
                    for (byte[] data : track) {
                        counts[1]++;
                        int function = data[6] & 0xff;
                        int sub = data.length > 8 ? data[7] & 0x3f : -1;
                        if (function == 0x10 && (sub == Function16.SUB_STREAM_ON || sub == Function16.SUB_STREAM_OFF)) {
                            // processing them would play the stream
                            if (sub == Function16.SUB_STREAM_ON) streamOns.add(data[8] & 0x1f);
                            continue;
                        }
                        MachineDependentFunction f = MachineDependentFunction.Factory.getFunction("48." + function);
                        if (f instanceof UndefinedFunction) {
                            // 0x8# are vavi-sound-nda's
                            if (function < 0x80 || function > 0x8f) {
                                errors.add("%s: no function for 0x%02x".formatted(path.getFileName(), function));
                            }
                            continue;
                        }
                        f.process(data, null);
                        switch (f) {
                            case PitchBendFunction pb when pb.getMsb() < 0 -> {
                                pendingLsb.put(pb.getVoice(), pb.getLsb());
                            }
                            case PitchBendFunction pb -> {
                                counts[2]++;
                                Integer lsb = pendingLsb.remove(pb.getVoice());
                                if (pb.getLsb() < 0 && lsb == null) {
                                    errors.add("%s: MSB without an LSB on voice %d".formatted(path.getFileName(), pb.getVoice()));
                                }
                            }
                            case Function16 ma -> {
                                switch (ma.getSubFunction()) {
                                    case Function16.SUB_FM_TONE, Function16.SUB_WT_TONE -> {
                                        counts[3]++;
                                        if (!banks.contains(ma.getBank())) {
                                            errors.add("%s: tone bank %d, the song selects %s".formatted(path.getFileName(), ma.getBank(), banks));
                                        }
                                        if (!ma.isDrum() && !programs.contains(ma.getProgram())) {
                                            errors.add("%s: tone program %d, the song selects %s".formatted(path.getFileName(), ma.getProgram(), programs));
                                        }
                                        int length = ma.getVoice().length;
                                        if (ma.getSubFunction() == Function16.SUB_WT_TONE) {
                                            wtVoices.add(ma.getVoice());
                                            if (length != Function16.WT_VOICE) {
                                                errors.add("%s: WT voice of %d bytes".formatted(path.getFileName(), length));
                                            }
                                        } else if (length != Function16.FM2_VOICE && length != Function16.FM4_VOICE) {
                                            errors.add("%s: FM voice of %d bytes".formatted(path.getFileName(), length));
                                        }
                                    }
                                    case Function16.SUB_WT_WAVE -> waveIds.add(ma.getWaveNumber());
                                    case Function16.SUB_STREAM_WAVE -> streams.add(ma.getStreamNumber());
                                    default -> { }
                                }
                            }
                            case Function48 wave when wave.getPart() == Function48.PART_SAMPLES -> tableWaves.add(wave.getWaveNumber());
                            case Function49 voice -> {
                                counts[4]++;
                                if (voice.getRecord()[1] != voice.getVoiceNumber()) {
                                    errors.add("%s: voice %d record says %d".formatted(path.getFileName(), voice.getVoiceNumber(), voice.getRecord()[1]));
                                }
                            }
                            default -> { }
                        }
                    }
                    if (!pendingLsb.isEmpty()) {
                        errors.add("%s: LSB never committed on voice(s) %s".formatted(path.getFileName(), pendingLsb.keySet()));
                    }
                }
                for (byte[] voice : wtVoices) {
                    // RM 0: the wave of a 0x06 of this file
                    if ((voice[15] & 0x80) == 0 && !waveIds.contains(voice[15] & 0x7f)) {
                        errors.add("%s: WT tone plays wave %d, sent %s".formatted(path.getFileName(), voice[15] & 0x7f, waveIds));
                    }
                }
                for (int stream : streamOns) {
                    // "ﾆｭｰｽ風.mld" starts streams it never sends, the file carries no stream at all
                    if (!streams.isEmpty() && !streams.contains(stream)) {
                        errors.add("%s: StreamOn %d, sent %s".formatted(path.getFileName(), stream, streams));
                    }
                }
            }
        }
Debug.println("sony files: %d, messages: %d, pitch bends: %d, tones: %d, wave table voices: %d"
        .formatted(counts[0], counts[1], counts[2], counts[3], counts[4]));
errors.forEach(System.err::println);
        assertEquals(List.of(), errors);
    }
}
