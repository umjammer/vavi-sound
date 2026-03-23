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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import vavi.sound.smaf.chunk.Chunk;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static java.util.function.Predicate.not;


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

    @Property(name = "mmf.dump")
    String mmf = "src/test/resources/test.mmf";

    @Property(name = "mmf.dir")
    String dir = "src/test/resources";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @DisplayName("dump")
    void test1() throws Exception {
        Path path = Paths.get(mmf);
Debug.println("path: " + path);
        InputStream is = new BufferedInputStream(Files.newInputStream(path));
        Chunk chunk = Chunk.readFrom(is, null);
Debug.println("chunk:\n" + chunk);
    }

    @Test
    @DisplayName("dump recursive")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test2() throws Exception {
        Path dir = Paths.get(this.dir);
        AtomicInteger c = new AtomicInteger();
        List<Path> f = new ArrayList<>();
        Files.walk(dir)
                .filter(p -> p.getFileName().toString().endsWith(".mmf"))
                .filter(not(p -> Stream.of("cracker", "test_data").anyMatch(s -> p.toString().contains(s))))
                .forEach(path -> {
Debug.println("---- path: " + path);
            try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
                Chunk chunk = Chunk.readFrom(is, null);
Debug.println("chunk:\n" + chunk);
                c.getAndIncrement();
            } catch (Exception e) {
Debug.println(e.toString());
                f.add(path);
            }
        });
Debug.println("smafs: " + c.get() + ", failure: " + f.size());
f.forEach(System.err::println);
    }
}
