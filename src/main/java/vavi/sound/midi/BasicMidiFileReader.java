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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.spi.MidiFileReader;

import vavi.util.Debug;


/**
 * Should implement {@link #getSequence(InputStream)} at your subclass.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public abstract class BasicMidiFileReader extends MidiFileReader {

    /**
     * Gets MIDI Sequence converted by a method {@link #getSequence(InputStream)}
     * implemented in a sub class.
     * @param stream a midi stream
     * @throws IOException when the I/O does not support marking.
     */
    public MidiFileFormat getMidiFileFormat(InputStream stream)
        throws InvalidMidiDataException,
               IOException {

//Debug.println("here ★");
        Sequence midiSequence = getSequence(stream);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MidiSystem.write(midiSequence, 0, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
//Debug.println("temporary midi:\n" + StringUtil.getDump(os.toByteArray(), 128));
        MidiFileFormat midiFF = MidiSystem.getMidiFileFormat(is);
        return midiFF;
    }

    @Override
    public MidiFileFormat getMidiFileFormat(File file)
        throws InvalidMidiDataException,
               IOException {

Debug.println(Level.FINE, "file: " + file);
        InputStream is = new BufferedInputStream(new FileInputStream(file));
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

        InputStream is = new BufferedInputStream(new FileInputStream(file));
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

/* */
