/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;


/**
 * NEC System exclusive message function 0x02, 0xf1, 0x0c processor.
 * (level 0x02 only, unidentified)
 * <p>
 * TODO more investigation. The MA-7 converter can emit it (internal event 214,
 * one data byte, {@code CnvMA7MFi_N.dll} at {@code 0x10020466}) but nothing in
 * either corpus does, and it has no level 0x01 counterpart, so what it controls
 * is unknown.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see ChannelValueFunction
 */
public class Function2_241_12 extends ChannelValueFunction {

    @Override
    int getLevel() {
        return 0x02;
    }

    @Override
    int getFunction() {
        return 0x0c;
    }

    @Override
    String getName() {
        return "Unknown";
    }
}
