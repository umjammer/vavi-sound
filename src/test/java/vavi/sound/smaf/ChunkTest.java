/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vavi.sound.smaf.chunk.Chunk;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * ChunkTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050508 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class ChunkTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property
    String mmfex = "src/test/resources/test.mmf";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @DisplayName("dump")
    void test1() throws Exception {
        Path path = Paths.get(mmfex);
Debug.println("path: " + path);
        InputStream is = new BufferedInputStream(Files.newInputStream(path));
        Chunk chunk = Chunk.readFrom(is, null);
Debug.println("chunk:\n" + chunk);
    }
}
