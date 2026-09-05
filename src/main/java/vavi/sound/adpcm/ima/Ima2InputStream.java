package vavi.sound.adpcm.ima;

import java.io.InputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmInputStream;
import vavi.sound.adpcm.Codec;


/** 2-bit IMA/DVI input stream (four little-endian code words per byte). */
public class Ima2InputStream extends AdpcmInputStream {

    @Override
    protected Codec getCodec() {
        return new Ima2();
    }

    public Ima2InputStream(InputStream in, ByteOrder byteOrder) {
        this(in, byteOrder, ByteOrder.LITTLE_ENDIAN);
    }

    public Ima2InputStream(InputStream in, ByteOrder byteOrder, ByteOrder bitOrder) {
        super(in, byteOrder, 2, bitOrder);
    }
}
