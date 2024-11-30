/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.nio.file.Files;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.spi.MidiFileReader;

import static java.lang.System.getLogger;


/**
 * Should implement {@link #getSequence(InputStream)} at your subclass.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public abstract class BasicMidiFileReader extends MidiFileReader {

    private static final Logger logger = getLogger(BasicMidiFileReader.class.getName());

    /**
     * Gets MIDI Sequence converted by a method {@link #getSequence(InputStream)}
     * implemented in a sub class.
     * @param stream a midi stream
     * @throws IOException when the I/O does not support marking.
     */
    @Override
    public MidiFileFormat getMidiFileFormat(InputStream stream)
        throws InvalidMidiDataException,
               IOException {

//logger.log(Level.TRACE, "here ★");
        Sequence midiSequence = getSequence(stream);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MidiSystem.write(midiSequence, 0, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
//logger.log(Level.TRACE, "temporary midi:\n" + StringUtil.getDump(os.toByteArray(), 128));
        MidiFileFormat midiFF = MidiSystem.getMidiFileFormat(is);
        return midiFF;
    }

    @Override
    public MidiFileFormat getMidiFileFormat(File file)
        throws InvalidMidiDataException,
               IOException {

logger.log(Level.DEBUG, "file: " + file);
        InputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()));
        return getMidiFileFormat(is);
    }

    @Override
    public MidiFileFormat getMidiFileFormat(URL url)
        throws InvalidMidiDataException,
               IOException {

        InputStream is = new BufferedInputStream(url.openStream());
        return getMidiFileFormat(is);
    }

    @Override
    public Sequence getSequence(File file)
        throws InvalidMidiDataException,
               IOException {

        InputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()));
        return getSequence(is);
    }

    @Override
    public Sequence getSequence(URL url)
        throws InvalidMidiDataException,
               IOException {

        InputStream is = new BufferedInputStream(url.openStream());
        return getSequence(is);
    }
}
