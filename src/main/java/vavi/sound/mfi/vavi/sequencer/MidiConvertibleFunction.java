/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.vavi.MidiContext;


/**
 * A {@link MachineDependentFunction} whose message has a plain MIDI counterpart.
 * <p>
 * A machine dependent message normally goes to the synthesizer as a sysex and reaches
 * its function there, with no track - so a function that plays on a channel (a pitch
 * bend, a hold pedal) cannot know which MIDI channel it is for. A function implementing
 * this is asked at conversion time instead, when the {@link MidiContext} still knows the
 * track, and what it returns goes into the sequence in place of the sysex. That also
 * puts it into a MIDI file the sequence is written to.
 * </p>
 * <p>
 * The function instance is shared, so this must not use its fields; whatever has to
 * outlive one message (a half of a value) belongs in the {@link MidiContext}.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 * @see vavi.sound.mfi.vavi.track.MachineDependentMessage#getMidiEvents(MidiContext)
 */
public interface MidiConvertibleFunction {

    /**
     * @param data the machine dependent message as {@link MachineDependentFunction#process}
     *             gets it, 0: delta, 1-2: ff ff, 3-4: length, 5: vendor, 6: function, ...
     * @param context the track of the message is {@link MidiContext#getMfiTrackNumber()}
     * @return the MIDI events, an empty array when the message only changes the context,
     *         null when this message has no MIDI counterpart and should go as the sysex
     */
    MidiEvent[] getMidiEvents(byte[] data, MidiContext context) throws InvalidMidiDataException;
}
