/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message.yamaha;

import java.io.ByteArrayOutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.SysexMessage;

import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.smaf.message.MachineDependentMessage;
import vavi.sound.smaf.message.MidiContext;
import vavi.sound.smaf.message.MidiConvertible;
import vavi.util.StringUtil;

import static vavi.sound.midi.MidiUtil.encode87;


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
        byte[] encoded = new byte[(getLength() - 1) * 8 / 7 + 1];
        int encodedLength = encode87(getData(), encoded, 0, getLength() - 1);

        // pack 7bit
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(VaviMidiDeviceProvider.MANUFACTURER_ID);
        baos.write(SYSEX_PACKED);
        baos.write(encoded, 0, encodedLength);
        baos.write(getData()[getData().length - 1]);

        SysexMessage sysexMessage = new SysexMessage();

        sysexMessage.setMessage(0xf0, baos.toByteArray(), baos.size());
logger.log(Level.DEBUG, "midi sysex: %02x, ".formatted(sysexMessage.getMessage()[0] & 0xff) + sysexMessage.getLength() + " bytes\n" + StringUtil.getDump(sysexMessage.getData(), 32));

        return new MidiEvent[] {
            new MidiEvent(sysexMessage, context.getCurrentTick())
        };
    }
}
