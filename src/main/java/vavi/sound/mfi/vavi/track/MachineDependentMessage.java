/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.SysexMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;
import vavi.sound.mfi.vavi.sequencer.MfiMessageStore;
import vavi.sound.midi.VaviMidiDeviceProvider;

import static java.lang.System.getLogger;


/**
 * Machine dependent System exclusive message.
 * <pre>
 *  0xff, 0xff
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020703 nsano refine <br>
 *          0.01 030711 nsano add constants <br>
 *          0.02 030711 nsano add {@link #getCarrier()} <br>
 *          0.03 030712 nsano read length as unsigned <br>
 *          0.04 030820 nsano implements {@link Serializable} <br>
 *          0.05 030821 nsano implements {@link MidiConvertible} <br>
 */
public class MachineDependentMessage extends SysexMessage
    implements MidiConvertible, Serializable {

    private static final Logger logger = getLogger(MachineDependentMessage.class.getName());

    /** */
    protected MachineDependentMessage(byte[] message) {
        super(message);
    }

    /** */
    public MachineDependentMessage() {
        super(new byte[0]);
    }

    /**
     * Set the message. Specify the data starting from the 6th byte (actual data).
     * @param delta delta time
     * @param message data from 6th byte
     */
    public void setMessage(int delta, byte[] message)
        throws InvalidMfiDataException {

        byte[] tmp = new byte[5 + message.length];
//logger.log(Level.TRACE, "data: " + message.length);
        tmp[0] = (byte) (delta & 0xff);
        tmp[1] = (byte) 0xff;
        tmp[2] = (byte) 0xff;
        tmp[3] = (byte) ((message.length / 0x100) & 0xff);
        tmp[4] = (byte) ((message.length % 0x100) & 0xff);
//logger.log(Level.TRACE, "\n" + StringUtil.getDump(new ByteArrayInputStream(tmp, 0, 5));
        System.arraycopy(message, 0, tmp, 5, message.length);

//logger.log(Level.TRACE, "message: " + tmp.length);
        super.setMessage(tmp, tmp.length);
//logger.log(Level.TRACE, "\n" + StringUtil.getDump(new ByteArrayInputStream(this.data, 0, 10));
    }

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param is actual data (without header, data2 ~)
     */
    public static MachineDependentMessage readFrom(int delta, int status, int data1, InputStream is)
        throws InvalidMfiDataException,
               IOException {

//logger.log(Level.TRACE, "\n" + StringUtil.getDump(is);
        DataInputStream dis = new DataInputStream(is);

        int length = dis.readUnsignedShort();
//logger.log(Level.TRACE, "length: " + length);

        byte[] data = new byte[length + 5];

        data[0] = (byte) (delta & 0xff);
        data[1] = (byte) 0xff;                      // normal 0xff
        data[2] = (byte) 0xff;                      // machine depend 0xff
        data[3] = (byte) ((length / 0x100) & 0xff); // length LSB
        data[4] = (byte) ((length % 0x100) & 0xff); // length MSB

        dis.readFully(data, 5, length);

        // 0 delta
        // 5 vendor | carrier
        // 6
        // 7
logger.log(Level.DEBUG, String.format("MachineDepend: %02x, %02x, %02x %02x %02x %02x %02x", data[0], data[5], data[6], data[7], (data.length > 8 ? data[8] : 0), (data.length > 9 ? data[9] : 0), (data.length > 10 ? data[10] : 0)));
        MachineDependentMessage message = new MachineDependentMessage(data);
        return message;
    }

    /** */
    public int getVendor() {
        return data[5] & 0xf0;
    }

    /** */
    public int getCarrier() {
        return data[5] & 0x0f;
    }

    /** */
    public String toString() {
        return String.format("MachineDepend: vendor: %02x", data[5]);
    }

    // ----

    /**
     * <p>
     * Create {@link MetaMessage} of Meta type 0x7f as a MIDI message
     * corresponding to this instance of {@link MachineDependentMessage}.
     * Store this {@link MachineDependentMessage} instance in {@link MfiMessageStore}
     * as the actual data of {@link MetaMessage}
     * and store the numbered ID in 2 bytes big endian.
     * </p>
     * <p>
     * For playback, listen to Meta type 0x7f with {@link javax.sound.midi.MetaEventListener}
     * and find the message with the corresponding id from {@link MfiMessageStore}.
     * Playback is performed by applying it to {@link vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer}.
     * </p>
     * <p>
     * see also {@code vavi.sound.mfi.vavi.MetaEventAdapter} for playing functionality.
     * </p>
     * <pre>
     * MIDI Meta
     * +--+--+--+--+--+--+--+--+--+--+--+-
     * |ff|7f|LL|ID|DD DD ...
     * +--+--+--+--+--+--+--+--+--+--+--+-
     *  0x7f sequencer specific meta event
     *  LL is really 1 byte?
     *  ID manufacturer ID
     * </pre>
     * <pre>
     * Current Spec.
     * +--+--+--+--+--+--+--+
     * |ff|7f|LL|5f|01|DH DL|
     * +--+--+--+--+--+--+--+
     *  0x5f manufacturer ID added arbitrarily
     *  0x01 indicates {@link MachineDependentMessage} data
     *  DH DL numbered id
     * </pre>
     * <p>
     * Since the default MIDI sequencer is used, only meta-events can be hooked,
     * so they are converted to meta-events.
     * </p>
     * @see vavi.sound.midi.VaviMidiDeviceProvider#MANUFACTURER_ID
     * @see MachineDependentSequencer#META_FUNCTION_ID_MACHINE_DEPEND
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        MetaMessage metaMessage = new MetaMessage();

        int id = MfiMessageStore.put(this);
        byte[] data = {
            VaviMidiDeviceProvider.MANUFACTURER_ID,
            MachineDependentSequencer.META_FUNCTION_ID_MACHINE_DEPEND,
            (byte) ((id / 0x100) & 0xff),
            (byte) ((id % 0x100) & 0xff)
        };
        metaMessage.setMessage(0x7f,    // sequencer specific meta event
                               data,
                               data.length);

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrent())
        };
    }
}
