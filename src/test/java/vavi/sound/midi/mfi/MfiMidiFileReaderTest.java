/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * MfiMidiFileReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class MfiMidiFileReaderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property
    String mfi = "src/test/resources/test.mld";

    @BeforeEach
    public void setup() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @DisplayName("get sequence directly")
    void test0() throws Exception {
        InputStream is = MfiMidiFileReaderTest.class.getResourceAsStream("/test.mld");
        Sequence sequence = new MfiMidiFileReader().getSequence(is);
        assertNotNull(sequence);
    }

    @Test
    @DisplayName("get sequence via spi")
    void test2() throws Exception {
        InputStream is = MfiMidiFileReaderTest.class.getResourceAsStream("/test.mld");
        Sequence sequence = MidiSystem.getSequence(is);
        assertNotNull(sequence);
    }

    @Test
    @DisplayName("get file format directly")
    void test3() throws Exception {
        InputStream is = MfiMidiFileReaderTest.class.getResourceAsStream("/test.mld");
        MidiFileFormat format = new MfiMidiFileReader().getMidiFileFormat(is);
        assertNotNull(format);
    }

    @Test
    @DisplayName("get file format via spi")
    void test4() throws Exception {
        InputStream is = MfiMidiFileReaderTest.class.getResourceAsStream("/test.mld");
        MidiFileFormat format = MidiSystem.getMidiFileFormat(is);
        assertNotNull(format);
    }

    @Test
    @DisplayName("sequencer")
    void test5() throws Exception {
Debug.print(mfi);
        Sequence sequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(Path.of(mfi))));
        Track track = sequence.getTracks()[0];
        boolean flag = false;
        for (int i = 0; i < track.size(); i++) {
            MidiEvent event = track.get(i);
            if (event.getMessage() instanceof MetaMessage metaMessage) {
                if (metaMessage.getType() == MetaEvent.META_MARKER.number()) {
Debug.print(StringUtil.getDump(metaMessage.getData()));
                    flag = true;
                }
            }
        }
        if (!flag)
            Debug.print(Level.WARNING, "no supt defined in this mfi: " + mfi);
    }

    // ----

    /** load MFi */
    public static void main(String[] args) throws Exception {
        MidiFileFormat mff = MidiSystem.getMidiFileFormat(new File(args[0]));
Debug.print(StringUtil.paramString(mff));
    }
}
