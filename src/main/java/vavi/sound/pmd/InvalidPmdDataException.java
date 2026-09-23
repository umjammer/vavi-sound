/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pmd;

import java.io.IOException;


/**
 * Thrown when the data is not a PMD (CMX) file.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public class InvalidPmdDataException extends IOException {

    /** */
    public InvalidPmdDataException(String s) {
        super(s);
    }
}
