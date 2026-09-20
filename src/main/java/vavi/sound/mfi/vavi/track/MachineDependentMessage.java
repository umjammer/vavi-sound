/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.SysexMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage;
import vavi.sound.mfi.vavi.TrackMessage.SysexTrackMessage;
import vavi.sound.mfi.vavi.VaviMfiSynthesizer.VaviMfiReceiver;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;

import static java.lang.System.getLogger;
import static vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer.MFi_SYSEX_FUNCTION_ID_MACHINE_DEPENDENT;
import static vavi.sound.midi.VaviMidiDeviceProvider.MANUFACTURER_ID;
import static vavi.sound.mobile.MobileExclusive.packedSysex;


/**
 * Machine dependent System exclusive message.
 * <pre>
 *  0xff, 0xff
 * </pre>
 * system property
 * <li>{@code vavi.sound.mobile.AudioEngine.disabled} ... not to use vavi.sound.mobile.AudioEngine but
 * to convert the message into the exclusives its vendor's functions send, default {@code false}</li>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020703 nsano refine <br>
 *          0.01 030711 nsano add constants <br>
 *          0.02 030711 nsano add {@link #getCarrier()} <br>
 *          0.03 030712 nsano read length as unsigned <br>
 *          0.04 030820 nsano implements {@link Serializable} <br>
 *          0.05 030821 nsano implements {@link MidiConvertible} <br>
 */
public class MachineDependentMessage extends SysexMessage
    implements MidiConvertible, TrackMessage, SysexTrackMessage, Serializable {

    private static final Logger logger = getLogger(MachineDependentMessage.class.getName());

    @Override
    public boolean accept(String key) {
        return "255.e.255".equals(key);
    }

    /** why override? */
    @Override
    public MachineDependentMessage init(byte[] message) {
        return (MachineDependentMessage) super.init(message);
    }

    /** */
    public MachineDependentMessage init() {
        return (MachineDependentMessage) super.init(new byte[0]);
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
     * for {@link TrackChunk}
     * @param dis actual data (without header, data2 ~)
     */
    @Override
    public MachineDependentMessage init(int delta, int status, int data1, DataInputStream dis)
        throws IOException {

//logger.log(Level.TRACE, "\n" + StringUtil.getDump(is);

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
        // ⋮
        // 5 vendor | carrier
        // 6
        // 7
logger.log(Level.DEBUG, "MachineDepend: Δ: %02x, len: %6d, VC: %02x, data: %02x %02x %02x %02x %02x".formatted(data[0], length, data[5], data[6], data[7], (data.length > 8 ? data[8] : 0), (data.length > 9 ? data[9] : 0), (data.length > 10 ? data[10] : 0)));
        super.init(data);
        return this;
    }

    /** */
    public int getVendor() {
        return data[5] & 0xf0;
    }

    /** */
    public int getCarrier() {
        return data[5] & 0x0f;
    }

    @Override
    public String toString() {
        return "MachineDepend: vendor: %02x".formatted(data[5]);
    }

    // ----

    /**
     * <p>
     * Create {@link SysexMessage} of Meta type 0x7f as a MIDI message
     * corresponding to this instance of {@link MachineDependentMessage}.
     * as the actual data of {@link SysexMessage}
     * and store the numbered ID in 2 bytes big endian.
     * </p>
     * <p>
     * For playback, listen to Manufacturer id 0x45 with {@link VaviMfiReceiver}
     * Playback is performed by applying it to {@link vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer}.
     * </p>
     * <p>
     * see also {@link VaviMfiReceiver} for playing functionality.
     * </p>
     * <pre>
     * MIDI Sysex should be
     * +--+--+--+--+--+--+--+--+--+--+--+-
     * |f0|45|OD|DD DD ...
     * +--+--+--+--+--+--+--+--+--+--+--+-
     *  0x45 sequencer specific meta event
     *  ID function id 0x01
     * </pre>
     * @see vavi.sound.midi.VaviMidiDeviceProvider#MANUFACTURER_ID
     * @see MachineDependentSequencer#MFi_SYSEX_FUNCTION_ID_MACHINE_DEPENDENT
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        byte[] exclusive = new byte[data.length + 2];
        exclusive[0] = (byte) MANUFACTURER_ID;
        exclusive[1] = (byte) MFi_SYSEX_FUNCTION_ID_MACHINE_DEPENDENT;
        System.arraycopy(data, 0, exclusive, 2, data.length);
        javax.sound.midi.SysexMessage sysexMessage = packedSysex(exclusive);    // sysex

        return new MidiEvent[] {
            new MidiEvent(sysexMessage, context.getCurrent())
        };
    }
}
