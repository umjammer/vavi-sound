/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage;
import vavi.sound.mfi.vavi.sequencer.MfiValueExclusive;

import static java.lang.System.getLogger;


/**
 * ChannelConfigurationMessage.
 * <pre>
 * 0xff, 0xba
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 030821 nsano implements {@link MidiConvertible} <br>
 *          0.02 030920 nsano repackage <br>
 */
public class ChannelConfigurationMessage extends ShortMessage
    implements MidiConvertible, TrackMessage {

    private static final Logger logger = getLogger(ChannelConfigurationMessage.class.getName());

    /** */
    private int channel;
    /** */
    private boolean drum;

    @Override
    public boolean accept(String key) {
        return "255.b.186".equals(key);
    }

    /**
     * for {@link TrackChunk}
     * @param delta delta time
     * @param status unused 0xff fixed
     * @param data1 0xba
     * @param data2 <pre>
     *  .6543 210 LSB
     *   ~~~~ ~~~
     *   |    +- drum
     *   +- channel
     * </pre>
     */
    @Override
    public ChannelConfigurationMessage init(int delta, int status, int data1, int data2) {
        super.init(delta, 0xff, 0xba, data2);

        this.channel = (data2 & 0x78) >> 3;
        this.drum = (data2 & 0x07) == 1;

        return this;
    }

    /** */
    public int getChannel() {
        return channel;
    }

    /** */
    public boolean isDrum() {
        return drum;
    }

    @Override
    public String toString() {
        return "ChannelConfiguration:" +
               " channel=" + channel +
               " drum="    + drum;
    }

    // ----

    /**
     * Sets the context channel configuration.
     * <p>
     * A general midi synthesizer needs nothing more, the channel and the notes go where it
     * expects a percussion. A synthesizer of an mfi sound source needs the byte itself though
     * (its lower 3 bits are the sound source's family mode, not just drum or not), which goes
     * as the {@link MfiValueExclusive#CHANNEL_CONFIGURATION} exclusive to the midi channel
     * the mfi channel ended up at.
     * </p>
     * @return the exclusive, nothing if the channel has no midi channel
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
logger.log(Level.DEBUG, this);
        context.setDrum(getChannel(), isDrum() ? MidiContext.ChannelConfiguration.PERCUSSION :
                                                 MidiContext.ChannelConfiguration.SOUND_SET);

        int midiChannel = context.retrieveChannel(getChannel());
        if (midiChannel < 0) {
            return null;
        }
        int raw = data[3] & 0xff;
        return new MidiEvent[] {
            new MidiEvent(MfiValueExclusive.message(MfiValueExclusive.CHANNEL_CONFIGURATION, midiChannel, raw & 0x7f, raw >> 7), context.getCurrent())
        };
    }
}
