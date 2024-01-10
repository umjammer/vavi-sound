/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiFileFormat;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.spi.MfiFileReader;


/**
 * MfiFileReader implemented by vavi.
 * <li> TODO 特殊命令の実装
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 020627 nsano midi compliant <br>
 *          0.11 030618 nsano add vibrato related <br>
 *          0.12 030817 nsano change unsupported exception <br>
 *          0.13 030819 nsano change sequencer related <br>
 *          0.20 030820 nsano refactoring <br>
 *          0.21 030821 nsano use {@link VaviNoteMessage} <br>
 */
public class VaviMfiFileReader extends MfiFileReader {

    @Override
    public MfiFileFormat getMfiFileFormat(InputStream stream)
        throws InvalidMfiDataException,
               IOException {

        return VaviMfiFileFormat.readFrom(stream);
    }

    @Override
    public MfiFileFormat getMfiFileFormat(File file)
        throws InvalidMfiDataException,
               IOException {

        InputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()));
        return VaviMfiFileFormat.readFrom(is);
    }

    @Override
    public MfiFileFormat getMfiFileFormat(URL url)
        throws InvalidMfiDataException,
               IOException {

        InputStream is = new BufferedInputStream(url.openStream());
        return VaviMfiFileFormat.readFrom(is);
    }

    @Override
    public Sequence getSequence(InputStream is)
        throws InvalidMfiDataException,
               IOException {

        VaviMfiFileFormat mff = VaviMfiFileFormat.readFrom(is);

        return mff.getSequence();
    }

    @Override
    public Sequence getSequence(File file)
        throws InvalidMfiDataException,
               IOException {

        InputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()));
        return getSequence(is);
    }

    @Override
    public Sequence getSequence(URL url)
        throws InvalidMfiDataException,
               IOException {

        InputStream is = new BufferedInputStream(url.openStream());
        return getSequence(is);
    }
}

/* */
