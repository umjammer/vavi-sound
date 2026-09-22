/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smd;

import java.io.IOException;


/**
 * Thrown when the data is not a SMD (J-SKY melody) file.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public class InvalidSmdDataException extends IOException {

    /** */
    public InvalidSmdDataException(String s) {
        super(s);
    }
}
