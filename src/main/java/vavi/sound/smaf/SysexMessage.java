/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import vavi.util.StringUtil;


/**
 * System exclusive message.
 * <pre>
 * [MIDI]
 *  FF id (id id) ... F7
 * </pre>
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public abstract class SysexMessage extends SmafMessage {

    /** */
    protected SysexMessage() {
    }

    /** */
    protected byte[] data;

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
     * @param data 0xf0 à»ç~
     */
    public void setMessage(int status, byte[] data, int length) throws InvalidSmafDataException {
        byte[] tmp = new byte[length];
        System.arraycopy(data, 0, tmp, 0, length);
        this.data = tmp;
    }

    /** */
    public String toString() {
        return "SYSEX:" +
            " duration=" + duration +
            " length=" + data.length + "\n" +
            StringUtil.getDump(data, 64);
    }

    /* */
    @Override
    public byte[] getMessage() {
        return null; // TODO
    }

    /* */
    @Override
    public int getLength() {
        return 0;   // TODO
    }

    /** TODO use properties file */
    public static class Factory {
        /**
         * @param data 0: maker id ...
         */
        public static SysexMessage getSysexMessage(int duration, byte[] data) throws InvalidSmafDataException {
            SysexMessage sysexMessage;
            switch (data[0]) {
            case 0x43: // YAMAHA
                sysexMessage = new vavi.sound.smaf.message.yamaha.YamahaMessage();
                break;
            case 0x7e: // non realtime
                sysexMessage = new vavi.sound.smaf.message.NonRealtimeUniversalSysexMessage();
                break;
            case 0x7f: // realtime
                sysexMessage = new vavi.sound.smaf.message.RealtimeUniversalSysexMessage();
                break;
            default:
                throw new IllegalArgumentException("unknown vendor: " + data[0]);
            }
            sysexMessage.setDuration(duration);
            sysexMessage.setMessage(0xf0, data, data.length);
            return sysexMessage;
        }
    }
}

/* */
