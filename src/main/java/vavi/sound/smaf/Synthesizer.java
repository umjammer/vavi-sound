/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Soundbank;


/**
 * Synthesizer.
 * <p>
 * {@link javax.sound.midi} subset compatible.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/04/08 umjammer initial version <br>
 */
public interface Synthesizer extends SmafDevice {

    /** @see javax.sound.midi.Synthesizer#getChannels() */
    MidiChannel[] getChannels();

    /** @see javax.sound.midi.Synthesizer#getDefaultSoundbank() */
    Soundbank getDefaultSoundbank();

    /** @see javax.sound.midi.Synthesizer#getAvailableInstruments() */
    Instrument[] getAvailableInstruments();

    /** @see javax.sound.midi.Synthesizer#getChannels() */
    boolean loadAllInstruments(Soundbank soundbank);

    /** @see javax.sound.midi.Synthesizer#unloadAllInstruments(Soundbank) */
    void unloadAllInstruments(Soundbank soundbank);

    /** @see javax.sound.midi.Synthesizer#getReceiver() */
    Receiver getReceiver() throws MidiUnavailableException;
}

/* */
