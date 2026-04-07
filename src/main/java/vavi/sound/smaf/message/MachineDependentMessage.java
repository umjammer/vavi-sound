/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SysexMessage;
import vavi.sound.smaf.message.yamaha.YamahaMessage;
import vavi.util.StringUtil;


/**
 * MachineDependentMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/09 umjammer initial version <br>
 */
public abstract class MachineDependentMessage extends SysexMessage {

    private static final Logger logger = System.getLogger(MachineDependentMessage.class.getName());

    /** 7bit packed sysex message for 8bit smaf sysex message */
    public static final int SYSEX_PACKED = 0x7f;

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
