package vavi.sound.adpcm.ccitt;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


@PropsEntity(url = "file:local.properties")
class G723InputStreamTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

    File outFile;

    @BeforeEach
    void setup() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
Debug.println(Level.FINE, "outFile: " + outFile);
    }

    @Test
    void availableMatchesDecodedPcmLength() throws Exception {
        // Four 2-bit code words per input byte, two PCM bytes per code word.
        G723InputStream stream = new G723InputStream(
                new ByteArrayInputStream(new byte[] { 0 }),
                ByteOrder.LITTLE_ENDIAN);

        assertEquals(8, stream.available());
        assertEquals(8, stream.readAllBytes().length);
        assertEquals(0, stream.available());
    }


    @ParameterizedTest
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    @ValueSource(ints = {2, 3, 5})
    void test2(int bits) throws Exception {

        int sampleRate = 8000;
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,
                1,
                2,
                sampleRate,
                byteOrder.equals(ByteOrder.BIG_ENDIAN));
Debug.print(format);

        String filename = "in_%d.g723".formatted(bits * 8);
Debug.print("filename: " + filename);
        InputStream is = new G723InputStream(getClass().getResourceAsStream(filename), bits, ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN);
Debug.print("available: " + is.available());

        OutputStream os = new BufferedOutputStream(Files.newOutputStream(outFile.toPath()));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (is.available() > 0) {
            int r = is.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
            os.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
        os.close();

        is.close();
    }
}
