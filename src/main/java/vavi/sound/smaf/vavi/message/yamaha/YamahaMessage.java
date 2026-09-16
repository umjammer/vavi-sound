/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message.yamaha;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.SysexMessage;

import vavi.sound.mobile.MobileExclusive;
import vavi.sound.smaf.vavi.message.MachineDependentMessage;
import vavi.sound.smaf.vavi.message.MidiContext;
import vavi.sound.smaf.vavi.message.MidiConvertible;
import vavi.util.StringUtil;


/**
 * YamahaMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050501 nsano initial version <br>
 */
public class YamahaMessage extends MachineDependentMessage implements MidiConvertible {

    private static final Logger logger = System.getLogger(YamahaMessage.class.getName());

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
        // getLength() is the length of the whole smaf message, the data is what is packed
        byte[] data = getData();

        SysexMessage sysexMessage = MobileExclusive.pack(data);
logger.log(Level.DEBUG, "midi sysex: %02x, ".formatted(sysexMessage.getMessage()[0] & 0xff) + sysexMessage.getLength() + " bytes\n" + StringUtil.getDump(sysexMessage.getData(), 32));

        return new MidiEvent[] {
            new MidiEvent(sysexMessage, context.getCurrentTick())
        };
    }
}
