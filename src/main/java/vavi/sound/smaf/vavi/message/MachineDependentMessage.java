/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SysexMessage;
import vavi.sound.smaf.vavi.message.yamaha.YamahaMessage;
import vavi.util.StringUtil;


/**
 * MachineDependentMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/09 umjammer initial version <br>
 */
public abstract class MachineDependentMessage extends SysexMessage {

    private static final Logger logger = System.getLogger(MachineDependentMessage.class.getName());

    /**
     * {@link vavi.sound.smaf.vavi.chunk.TrackChunk.FormatType#HandyPhoneStandard}
     * <pre>
     *  duration    1 or 2
     *  0xff
     *  0xf0
     *  length      1 byte
     *  data        the maker id ~ 0xf7
     * </pre>
     *
     * @throws IllegalArgumentException the data is longer than the 1 byte length can tell
     */
    @Override
    public byte[] getMessage() {
        int length = data.length - 1; // data[0] is the status
        if (length > 0xff) {
            throw new IllegalArgumentException("too long for HandyPhoneStandard: " + length);
        }
        int[] message = new int[3 + length];
        message[0] = 0xff;
        message[1] = 0xf0;
        message[2] = length;
        for (int i = 0; i < length; i++) {
            message[3 + i] = data[i + 1];
        }
        return HandyPhoneStandard.message(duration, message);
    }

    @Override
    public int getLength() {
        return getMessage().length;
    }

    /** */
    public static class Factory {
        /**
         * @param data 0: maker id ... , 8bit!
         */
        public static SysexMessage getSysexMessage(int duration, int status, byte[] data, int length) throws InvalidSmafDataException {
logger.log(Level.DEBUG, "smaf sysex: %d, ".formatted(data[0] & 0xff) + length + " bytes\n" + StringUtil.getDump(data, 32));
//            assert data[data.length - 1] == (byte) 0xf7;

            SysexMessage sysexMessage = switch (data[0]) {
                case 0x43 -> new YamahaMessage();
                default -> new SysexMessage(); // TODO no one comes here bec smaf is for yamaha only?
            };
            sysexMessage.setDuration(duration);
            sysexMessage.setMessage(status, data, length);
            return sysexMessage;
        }
    }
}
