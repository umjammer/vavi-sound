/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.StringJoiner;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * ExclusiveVoiceChunk.
 * <pre>
 * "EXVO"
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-12-14 nsano initial version <br>
 */
public class ExclusiveVoiceChunk extends Chunk {

    private static final Logger logger = getLogger(ExclusiveVoiceChunk.class.getName());

    enum ExclusiveType {
        Unknown,
        /** VMAVoice */
        VMAVoice,
        /** VM3/VM5Voice */
        VM35Voice;
        static ExclusiveType valueOf(int i) {
            return switch (i) {
                case 1 -> VMAVoice;
                case 2 -> VM35Voice;
                default -> Unknown;
            };
        }
    }

    ExclusiveType exclusiveType;
    /**
     * ff f0 len 43 ..
     * len 10: 43 79 07 7F 01 ...
     * len 10: 43 79 06 7F 01 ...
     * len 3 : 43 05 01
     * len 6 : 43 03 ...
     * @see "https://github.com/but80/smaf825/blob/v1/smaf/subtypes/exclusive.go"
     */
    byte[] exclusive;

    public ExclusiveVoiceChunk(byte[] id, int size) {
        super(id, size);
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        exclusive = new byte[size];
        dis.readFully(exclusive);
        exclusiveType = ExclusiveType.valueOf(exclusive[2]);

logger.log(Level.TRACE, "\n" + StringUtil.getDump(exclusive));
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {

    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getId() + ": {", "}")
                .add("exclusiveType: " + exclusiveType)
                .add("exclusive: " + Arrays.toString(exclusive))
                .toString();
    }
}
