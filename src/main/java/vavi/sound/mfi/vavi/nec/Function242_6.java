/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.lang.System.Logger.Level;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.StringUtil;

import static vavi.sound.mfi.vavi.nec.NecSequencer.VENDOR_NEC;


/**
 * NEC System exclusive message function 0xf2, 0x06 processor.
 * <p>
 * TODO more investigation. Only 3 messages in the ~4400 file
 * {@code ~/Public/np2/mfi} corpus write it, all of them in MFi 2.0 files
 * (`That's Why`, `think of U`, `ジングルベル`), so what it drives is unknown.
 * </p>
 * <p>
 * What the data says: it is one of the device control messages next to
 * {@link Function242_4} (vibrator) and {@link Function242_5} (LED) - same
 * 0xf2 group, same two byte payload, same value space ({@code 00 00} twice and
 * {@code 40 00} once, where vibrator and LED use {@code 20 00} / {@code 00 00} /
 * {@code 40 00}), channel always 0, and all three sit at time 0 of track 0 in
 * the setup block between an {@code f0.01} (FM voice change) and an
 * {@code f0.02} (ADPCM data) message.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
public class Function242_6 implements MachineDependentFunction {

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "242_6";
    }

    /**
     * 0xf2, 0x06 (unknown), length 5
     *
     * @param message  see below
     *                 <pre>
     *                 0    delta
     *                 1    ff
     *                 2    ff
     *                 3-4  length
     *                 5    vendor
     *                 6    f1          f2
     *                 7    76....0110
     *                      ~~    ~~~~
     *                      |     +----- 0x6
     *                      +----------- channel
     *                 8    ?           00 or 40
     *                 9    ?           00
     *                 </pre>
     * @param receiver
     */
    @Override
    public void process(MachineDependentMessage message, Receiver receiver)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        if (data.length < 10) {
            throw new InvalidMfiDataException("too short: " + (data.length - 5));
        }

        this.channel = (data[7] & 0xc0) >> 6;    // 0 ~ 3
        this.data1   =  data[8] & 0xff;          // TODO
        this.data2   =  data[9] & 0xff;          // TODO

logger.log(Level.DEBUG, "f2.06 (TODO more investigation): " + channel + "ch, %02x %02x".formatted(data1, data2));
logger.log(Level.TRACE, StringUtil.getDump(data));
    }

    /** channel 0 ~ 3 */
    private int channel;
    /** TODO 0x00 or 0x40 so far */
    private int data1;
    /** TODO 0x00 so far */
    private int data2;

    /** channel 0 ~ 3 */
    public int getChannel() {
        return channel;
    }

    /** TODO 0x00 or 0x40 so far */
    public int getData1() {
        return data1;
    }

    /** TODO 0x00 so far */
    public int getData2() {
        return data2;
    }

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public void setData1(int data1) {
        this.data1 = data1 & 0xff;
    }

    /** */
    public void setData2(int data2) {
        this.data2 = data2 & 0xff;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[5];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0xf2;
        tmp[2] = (byte) ((channel << 6) | 0x06);
        tmp[3] = (byte) data1;
        tmp[4] = (byte) data2;
        return tmp;
    }
}
