/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.util.Arrays;
import java.util.Map;

import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.message.yamaha.YamahaMessage;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * System exclusive message.
 * <pre>
 * [MIDI]
 *  F0 id (id id) len (len len) mid ... F7
 * </pre>
 * <pre>
 *
 * F0 ll mm nn
 *    ~~ ~~ ~~
 *    |  |  +---
 *    |  +------ manufacturers id
 *    +--------- length
 *
 * [XF cue point]
 * F0 04 43 7B 02 rr
 *
 * [specify channel status]
 * F0 14 43 02 00 04 dd ... dd
 *
 * [MA-5 AL specify channel]
 * F0 06 43 02 01 01 cc dd
 *
 * [MA-5 V specify voice channel]
 * F0 06 43 02 01 02 cc dd
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 * @see "https://ia800507.us.archive.org/27/items/at-manual-e/AT_Manual_e.pdf"
 */
public class SysexMessage extends SmafMessage {

    private static final Logger logger = getLogger(SysexMessage.class.getName());

    protected byte[] data;

    /** */
    public SysexMessage() {
        duration = 0;
    }

    /**
     * w/o status
     * <p>
     * {@link javax.sound.midi.MetaMessage} compatible.
     * </p>
     */
    public byte[] getData() {
        return Arrays.copyOfRange(data, 1, data.length - 1);
    }

    /**
     * <pre>
     * + HandyPhoneStandard
     *
     *  0xff
     *  0xf0
     *  0x##        maker id, 7d: edu, 7e: non realtime, 7f: realtime
     *  0x##        format id
     *  0x##        message size ???
     *  ... ~ 0xf7  data
     *
     * + MobileStandard
     *
     *  0xf0
     *  ...         size
     *  ... ~ 0xf7  data
     * </pre>
     *
     * @param status 0xf0
     * @param data after 0xf0
     */
    public void setMessage(int status, byte[] data, int length) throws InvalidSmafDataException {
        byte[] tmp = new byte[length + 1];
        tmp[0] = (byte) status;
        System.arraycopy(data, 0, tmp, 1, length);
        this.data = tmp;
    }

    /** @param data 0: must be status byte (0xf0 or 0xf7) */
    public void setMessage(byte[] data, int length) throws InvalidSmafDataException {
        if (data[0] != (byte) 0xf0 && data[0] != (byte) 0xf7) {
            throw new InvalidSmafDataException("status: " + data[0]);
        }
        byte[] tmp = new byte[length];
        System.arraycopy(data, 0, tmp, 0, length);
        this.data = tmp;
    }

    @Override
    public String toString() {
        return "SYSEX:" +
            " duration=" + duration +
            " length=" + data.length + "\n" +
            StringUtil.getDump(data, 32);
    }

    @Override
    public byte[] getMessage() {
        return data;
    }

    @Override
    public int getLength() {
        return data != null ? data.length : -1;
    }

    /*
     * Realtime Universal System exclusive message.
     * <pre>
     * [MIDI]
     *  F0 7F ... F7
     * </pre>
     * <pre>
     *  0xf0
     *  0x7f
     *  SUB-ID#1 | SUB-ID#2  | function
     *  ---------+-----------+------------------------------------------
     *   00H     | --        | unused
     *   01H     | nn        | MIDI Time Code
     *           | 01H       |  Full Message
     *           | 02H       |  User Bits
     *   02H     | nn        | MIDI Show Control
     *           | 00H       |  MSC Extensions
     *           | 01H ~ 7FH |  MSC Commands
     *   03H     | nn        | Notation Information
     *           | 01H       |  Bar Number
     *           | 02H       |  Time Signature(Immediate)
     *           | 42H       |  Time Signature(Delayed)
     *   04H     | nn        | Device Control
     *           | 01H       |  Master Volume
     *           | 02H       |  Master Balance
     *   05H     | nn        | Real Time MTC Cueing
     *           | 00H       |  Special
     *           | 01H       |  Punch In Points
     *           | 02H       |  Punch Out Points
     *           | 03H       |  (Reserved)
     *           | 04H       |  (Reserved)
     *           | 05H       |  Event Start Points
     *           | 06H       |  Event Stop Points
     *           | 07H       |  Event Start Points with additional info.
     *           | 08H       |  Event Stop Points with additional info.
     *           | 09H       |  (Reserved)
     *           | 0AH       |  (Reserved)
     *           | 0BH       |  Cue Points
     *           | 0CH       |  Cue Points with additional info.
     *           | 0DH       |  (Reserved)
     *           | 0EH       |  Event Name in additional info.
     *   06H     | nn        | MIDI Machine Control Commands
     *           | 00H ~ 7FH |  MMC Commands
     *   07H     | nn        | MIDI Machine Control Responses
     *           | 00H ~ 7FH |  MMC Commands
     *   08H     | nn        | MIDI Tuning Standard
     *           | 02H       |  Note Change
     * </pre>
     *
     */

    /*

[master volume]
      F0 7F 7F 04 01 ll mm F7
          nn=0 ~ 7F(volume: 0 ~ 127) * initial value=64 (volume: 100)

[Master Fine Tuning]
      F0h 7F 7F 04 03 vh vl F7
          specify master fine tune
          specifies the tuning from A440Hz in cents.

[Master Coarse Tuning]
      F0 7F 7F 04 04 00 vl F7
          specify coarse fine tune
          specifies the tuning from A440Hz in cents.

[GM SYSTEM ON]
      F0 7E 7F 09 01 F7

[GM2 SYSTEM ON]
      F0 7E 7F 09 03 F7

[GM SYSTEM OFF]
      F0 7E 7F 09 02 F7

     */

    /*
     * Non Realtime Universal System exclusive message.
     * <pre>
     * [MIDI]
     *  F0 7E ... F7
     * </pre>
     * <pre>
     *  0xf0
     *  0x7e
     *  SUB-ID#1 | SUB-ID#2   | function
     *  ---------|------------|-----------------------
     *   00H     | --         | unused
     *   01H     | (not used) | Sample Dump Header
     *   02H     | (not used) | Sample Data Packet
     *   03H     | (not used) | Sample Dump Request
     *   04H     | nn         | MIDI Time Code
     *           | 00H        |  Special
     *           | 01H        |  Punch In Points
     *           | 02H        |  Punch Out Points
     *           | 03H        |  Delete Punch In Point
     *           | 04H        |  Delete Punch Out Point
     *           | 05H        |  Event Start Point
     *           | 06H        |  Event Stop Point
     *           | 07H        |  Event Start Points with additional info.
     *           | 08H        |  Event Stop Points with additional info.
     *           | 09H        |  Delete Event Start Point
     *           | 0AH        |  Delete Event Stop Point
     *           | 0BH        |  Cue Points
     *           | 0CH        |  Cue Points with additional info.
     *           | 0DH        |  Delete Cue Point
     *           | 0EH        |  Event Name in additional info.
     *   05H     | nn         | Sample Dump Extensions
     *           | 01H        |  Multiple Loop Points
     *           | 02H        |  Loop Points Request
     *   06H     | nn         | General Information
     *           | 01H        |  Identity Request
     *           | 02H        |  Identity Reply
     *   07H     | nn         | File Dump
     *           | 01H        |  Header
     *           | 02H        |  Data Packet
     *           | 03H        |  Request
     *   08H     | nn         | MIDI Tuning Standard
     *           | 00H        |  Bulk Dump Request
     *           | 01H        |  Bulk Dump Reply
     *   09H     | nn         | General MIDI
     *           | 01H        |  General MIDI System On
     *           | 02H        |  General MIDI System Off
     *   7BH     | (not used) | End Of File
     *   7CH     | (not used) | Wait
     *   7DH     | (not used) | Cancel
     *   7EH     | (not used) | NAK
     *   7FH     | (not used) | ACK
     * </pre>
     */

    /** TODO use properties file */
    public static class Factory {
        /**
         * @param data 0: maker id ...
         */
        public static SysexMessage getSysexMessage(int duration, int status, byte[] data, int length) throws InvalidSmafDataException {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeByte(status);
                MidiUtil.writeVarInt(dos, length);
                dos.write(data);
                byte[] tmp = baos.toByteArray();

//logger.log(Level.DEBUG, "sysex " + data.length + " bytes\n" + StringUtil.getDump(tmp, 32));
//                assert data[data.length - 1] == (byte) 0xf7;

                SysexMessage sysexMessage = switch (data[0]) {
                    case 0x43 -> new YamahaMessage();
                    default -> new SysexMessage(); // TODO no one comes here bec smaf is for yamaha only?
                };
                sysexMessage.setDuration(duration);
                sysexMessage.setMessage(tmp, tmp.length);
                return sysexMessage;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
