/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;


/**
 * SmafFileReader.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071012 nsano initial version <br>
 */
class SmafFileReader {

    /** */
    public SmafFileFormat getSmafFileFormat(InputStream stream)
        throws InvalidSmafDataException,
               IOException {

        return SmafFileFormat.readFrom(stream);
    }

    /** */
    public SmafFileFormat getSmafFileFormat(File file)
        throws InvalidSmafDataException,
               IOException {

        InputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()));
        return SmafFileFormat.readFrom(is);
    }

    /** */
    public SmafFileFormat getSmafFileFormat(URL url)
        throws InvalidSmafDataException,
               IOException {

        InputStream is = new BufferedInputStream(url.openStream());
        return SmafFileFormat.readFrom(is);
    }

    /** */
    public Sequence getSequence(InputStream is)
        throws InvalidSmafDataException,
               IOException {

        SmafFileFormat ff = SmafFileFormat.readFrom(is);

        return ff.getSequence();
    }

    /** */
    public Sequence getSequence(File file)
        throws InvalidSmafDataException,
               IOException {

        InputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()));
        return getSequence(is);
    }

    /** */
    public Sequence getSequence(URL url)
        throws InvalidSmafDataException,
               IOException {

        InputStream is = new BufferedInputStream(url.openStream());
        return getSequence(is);
    }
}
