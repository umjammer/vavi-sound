/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.Sequence;
import vavi.sound.smaf.spi.SmafFileReader;


/**
 * VaviSmafFileReader.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071012 nsano initial version <br>
 */
public class VaviSmafFileReader extends SmafFileReader {

    @Override
    public VaviSmafFileFormat getSmafFileFormat(InputStream stream)
        throws InvalidSmafDataException,
               IOException {

        return VaviSmafFileFormat.readFrom(stream);
    }

    @Override
    public VaviSmafFileFormat getSmafFileFormat(File file)
        throws InvalidSmafDataException,
               IOException {

        InputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()));
        return VaviSmafFileFormat.readFrom(is);
    }

    @Override
    public VaviSmafFileFormat getSmafFileFormat(URL url)
        throws InvalidSmafDataException,
               IOException {

        InputStream is = new BufferedInputStream(url.openStream());
        return VaviSmafFileFormat.readFrom(is);
    }

    @Override
    public Sequence getSequence(InputStream is)
        throws InvalidSmafDataException,
               IOException {

        VaviSmafFileFormat ff = VaviSmafFileFormat.readFrom(is);

        return ff.getSequence();
    }

    @Override
    public Sequence getSequence(File file)
        throws InvalidSmafDataException,
               IOException {

        InputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()));
        return getSequence(is);
    }

    @Override
    public Sequence getSequence(URL url)
        throws InvalidSmafDataException,
               IOException {

        InputStream is = new BufferedInputStream(url.openStream());
        return getSequence(is);
    }
}
