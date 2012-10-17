/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiFileFormat;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.spi.MfiFileReader;


/**
 * MfiFileReader implemented by vavi.
 * <li> TODO 特殊命令の実装
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.10 020627 nsano midi compliant <br>
 *          0.11 030618 nsano add vibrato related <br>
 *          0.12 030817 nsano change unsupported exception <br>
 *          0.13 030819 nsano change sequencer related <br>
 *          0.20 030820 nsano refactoring <br>
 *          0.21 030821 nsano use {@link VaviNoteMessage} <br>
 */
public class VaviMfiFileReader extends MfiFileReader {

    /** */
    public MfiFileFormat getMfiFileFormat(InputStream stream)
        throws InvalidMfiDataException,
               IOException {

        return VaviMfiFileFormat.readFrom(stream);
    }

    /** */
    public MfiFileFormat getMfiFileFormat(File file)
        throws InvalidMfiDataException,
               IOException {

        InputStream is = new BufferedInputStream(new FileInputStream(file));
        return VaviMfiFileFormat.readFrom(is);
    }

    /** */
    public MfiFileFormat getMfiFileFormat(URL url)
        throws InvalidMfiDataException,
               IOException {

        InputStream is = new BufferedInputStream(url.openStream());
        return VaviMfiFileFormat.readFrom(is);
    }

    /** */
    public Sequence getSequence(InputStream is)
        throws InvalidMfiDataException,
               IOException {

        VaviMfiFileFormat mff = VaviMfiFileFormat.readFrom(is);

        return mff.getSequence();
    }

    /** */
    public Sequence getSequence(File file)
        throws InvalidMfiDataException,
               IOException {

        InputStream is = new BufferedInputStream(new FileInputStream(file));
        return getSequence(is);
    }

    /** */
    public Sequence getSequence(URL url)
        throws InvalidMfiDataException,
               IOException {

        InputStream is = new BufferedInputStream(url.openStream());
        return getSequence(is);
    }
}

/* */
