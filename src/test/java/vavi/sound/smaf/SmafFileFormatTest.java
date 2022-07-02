/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;


/**
 * SmafFileFormatTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class SmafFileFormatTest {

    @Test
    public void test() throws Exception, IOException {
        Path path = Paths.get(SmafFileFormatTest.class.getResource("/test.mmf").toURI());
        SmafSystem.getSmafFileFormat(new BufferedInputStream(Files.newInputStream(path)));
    }

    //----

    /**
     * load only
     * @param args 0: input mmf
     */
    public static void main(String[] args) throws Exception {
        SmafSystem.getSmafFileFormat(new BufferedInputStream(Files.newInputStream(Paths.get(args[0]))));
    }
}

/* */
