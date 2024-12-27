/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vavi.sound.mfi.vavi.VaviMfiFileFormat;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * ChunkTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 241218 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class ChunkTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property
    String mfiex = "src/test/resources/test.mld";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @DisplayName("dump")
    void test1() throws Exception {
        Path path = Paths.get(mfiex);
Debug.println("path: " + path);
        InputStream is = new BufferedInputStream(Files.newInputStream(path));
        VaviMfiFileFormat chunk = VaviMfiFileFormat.readFrom(is);
Debug.println("chunk:\n" + chunk);
    }
}
