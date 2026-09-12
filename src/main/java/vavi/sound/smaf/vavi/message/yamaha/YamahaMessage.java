/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message.yamaha;

import java.io.ByteArrayOutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.SysexMessage;

import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.smaf.vavi.message.MachineDependentMessage;
import vavi.sound.smaf.vavi.message.MidiContext;
import vavi.sound.smaf.vavi.message.MidiConvertible;
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
        // getLength() is the length of the whole smaf message, the data is what is packed
        byte[] data = getData();
        byte[] encoded = new byte[data.length * 8 / 7 + 1];
        int encodedLength = encode87(data, encoded, 0, data.length);

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
