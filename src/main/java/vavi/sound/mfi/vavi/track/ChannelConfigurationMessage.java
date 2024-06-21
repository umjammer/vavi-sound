/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;

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
    implements MidiConvertible {

    private static final Logger logger = getLogger(ChannelConfigurationMessage.class.getName());

    /** */
    private final int channel;
    /** */
    private final boolean drum;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
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
    public ChannelConfigurationMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xba, data2);

        this.channel = (data2 & 0x78) >> 3;
        this.drum = (data2 & 0x07) == 1;
    }

    /** */
    public int getChannel() {
        return channel;
    }

    /** */
    public boolean isDrum() {
        return drum;
    }

    /** */
    public String toString() {
        return "ChannelConfiguration:" +
               " channel=" + channel +
               " drum="    + drum;
    }

    //----

    /**
     * Sets the context channel configuration.
     * @return nothing
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) {
logger.log(Level.DEBUG, this);
        context.setDrum(getChannel(), isDrum() ? MidiContext.ChannelConfiguration.PERCUSSION :
                                                 MidiContext.ChannelConfiguration.SOUND_SET);

        return null;    // TODO
    }
}
