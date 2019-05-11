/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
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
public interface Synthesizer extends MfiDevice {

    /** for volume controle */
    MidiChannel[] getChannels() throws MfiUnavailableException;

    /** */
    Soundbank getDefaultSoundbank();

    /** */
    Instrument[] getAvailableInstruments();

    /** */
    boolean loadAllInstruments(Soundbank soundbank);

    /** */
    void unloadAllInstruments(Soundbank soundbank);
}

/* */
