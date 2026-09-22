/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.dxm;

import java.io.IOException;


/**
 * Thrown when the data is not a DXM (MCDF) file.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public class InvalidDxmDataException extends IOException {

    /** */
    public InvalidDxmDataException(String s) {
        super(s);
    }
}
