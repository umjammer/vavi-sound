package vavi.sound.adpcm.ccitt;

import java.io.ByteArrayInputStream;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class G723_16InputStreamTest {

    @Test
    void availableMatchesDecodedPcmLength() throws Exception {
        // Four 2-bit code words per input byte, two PCM bytes per code word.
        G723_16InputStream stream = new G723_16InputStream(
                new ByteArrayInputStream(new byte[] { 0 }),
                ByteOrder.LITTLE_ENDIAN);

        assertEquals(8, stream.available());
        assertEquals(8, stream.readAllBytes().length);
        assertEquals(0, stream.available());
    }
}
