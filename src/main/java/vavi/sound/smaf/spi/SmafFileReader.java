/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.Sequence;
import vavi.sound.smaf.vavi.VaviSmafFileFormat;


/**
 * SmafFileReader.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260912 nsano initial version <br>
 * @see javax.sound.midi.spi.MidiFileReader
 */
public abstract class SmafFileReader {

    /** @see javax.sound.midi.spi.MidiFileReader#getMidiFileFormat(InputStream) */
    public abstract VaviSmafFileFormat getSmafFileFormat(InputStream stream)
        throws InvalidSmafDataException, IOException;

    /** @see javax.sound.midi.spi.MidiFileReader#getMidiFileFormat(File) */
    public abstract VaviSmafFileFormat getSmafFileFormat(File file)
        throws InvalidSmafDataException, IOException;

    /** @see javax.sound.midi.spi.MidiFileReader#getMidiFileFormat(URL) */
    public abstract VaviSmafFileFormat getSmafFileFormat(URL url)
        throws InvalidSmafDataException, IOException;

    /** @see javax.sound.midi.spi.MidiFileReader#getSequence(InputStream) */
    public abstract Sequence getSequence(InputStream stream)
        throws InvalidSmafDataException, IOException;

    /** @see javax.sound.midi.spi.MidiFileReader#getSequence(File) */
    public abstract Sequence getSequence(File file)
        throws InvalidSmafDataException, IOException;

    /** @see javax.sound.midi.spi.MidiFileReader#getSequence(URL) */
    public abstract Sequence getSequence(URL url)
        throws InvalidSmafDataException, IOException;
}
