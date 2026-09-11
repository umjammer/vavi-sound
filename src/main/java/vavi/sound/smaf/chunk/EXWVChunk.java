/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.message.WaveDataMessage;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.smaf.chunk.Chunk.DumpContext.getDC;


/**
 * EXWVChunk.
 * <p>
 * "EXclusive WaVe": the wave (ADPCM) body of a wavetable voice that lives in a
 * {@link VoiceChunk} ("VOIC") of a {@link MMMGChunk} ("MMMG") track. The chunk body is a
 * single machine dependent exclusive, but unlike {@link ExclusiveVoiceChunk} ("EXVO") it is
 * <em>not</em> 7 bit safe, hence the {@code 0xf1} status and the 16 bit length.
 * </p>
 * <pre>
 * "EXWV"
 *
 *  +0        0xff            exclusive prefix
 *  +1        0xf1            "long" exclusive (binary, 8 bit data, 16 bit length)
 *  +2 ~ +3   length          little endian, counts +4 ... 0xf7 inclusive
 *  +4        0x43            manufacturer id (YAMAHA)
 *  +5        0x05            SMAF phrase (MA-3/MA-5 class) exclusive
 *  +6        0x00            sub id: wave data
 *  +7        wave id         0 ~ 15, referred to by EXVO "RM|WaveID"
 *  +8 ~      wave data       length - 5 bytes, written verbatim into wave ram
 *  +length+3 0xf7            end of exclusive
 * </pre>
 * <p>
 * The wave data is 4 bit ADPCM (the very codec of {@code vavi.sound.adpcm.ma}); the sampling
 * rate, the loop point and the end point are not here but in the "EXVO" chunk that links to
 * this wave id, see {@link ExclusiveVoiceChunk}. "EXWV" always precedes the "EXVO" that
 * refers to it.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-12-15 nsano initial version <br>
 *          0.01 2026-09-11 nsano parse the exclusive <br>
 */
public class EXWVChunk extends Chunk {

    private static final Logger logger = getLogger(EXWVChunk.class.getName());

    /** exclusive prefix */
    private static final int STATUS = 0xff;

    /** binary (non 7 bit safe) exclusive, 16 bit little endian length follows */
    private static final int LONG_EXCLUSIVE = 0xf1;

    /** YAMAHA */
    private static final int MANUFACTURER = 0x43;

    /** SMAF phrase exclusive */
    private static final int DEVICE = 0x05;

    /** wave data */
    private static final int SUB_ID = 0x00;

    /** end of exclusive */
    private static final int EOX = 0xf7;

    /** bytes of the exclusive which are not wave data (0x43, 0x05, 0x00, wave id, 0xf7) */
    private static final int OVERHEAD = 5;

    /** 0 ~ 15, the "WaveID" an "EXVO" refers to */
    private int waveId;

    /** 4 bit adpcm */
    byte[] data;

    /** comes from the "EXVO" which links to {@link #waveId}, 8000 unless told */
    private int samplingRate = 8000;

    private static final String FOURCC = "EXWV";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key);
    }

    @Override
    public EXWVChunk init(byte[] id, int size) {
        return (EXWVChunk) super.init(id, size);
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        int status = dis.readUnsignedByte();
        int kind = dis.readUnsignedByte();
        if (status != STATUS || kind != LONG_EXCLUSIVE) {
            throw new InvalidSmafDataException("EXWV: not an exclusive: %02x %02x".formatted(status, kind));
        }
        int length = dis.readUnsignedByte() | (dis.readUnsignedByte() << 8); // little endian
        if (length <= OVERHEAD || length + 4 > size) {
            throw new InvalidSmafDataException("EXWV: bad length: " + length + " / " + size);
        }
        int manufacturer = dis.readUnsignedByte();
        int device = dis.readUnsignedByte();
        int subId = dis.readUnsignedByte();
        if (manufacturer != MANUFACTURER || device != DEVICE || subId != SUB_ID) {
            throw new InvalidSmafDataException("EXWV: unknown exclusive: %02x %02x %02x".formatted(manufacturer, device, subId));
        }
        waveId = dis.readUnsignedByte();

        data = new byte[length - OVERHEAD];
        dis.readFully(data);

        int eox = dis.readUnsignedByte();
        if (eox != EOX) {
            throw new InvalidSmafDataException("EXWV: no eox: %02x".formatted(eox));
        }
        dis.skipBytes(size - (length + 4)); // normally none
logger.log(Level.DEBUG, FOURCC + ": " + size + ", waveId: " + waveId + ", adpcm: " + data.length + "\n" + StringUtil.getDump(data, 16));
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        int length = data.length + OVERHEAD;
        dos.writeByte(STATUS);
        dos.writeByte(LONG_EXCLUSIVE);
        dos.writeByte(length & 0xff);
        dos.writeByte((length >> 8) & 0xff);
        dos.writeByte(MANUFACTURER);
        dos.writeByte(DEVICE);
        dos.writeByte(SUB_ID);
        dos.writeByte(waveId);
        dos.write(data);
        dos.writeByte(EOX);
    }

    /** 0 ~ 15, the "WaveID" an "EXVO" refers to */
    public int getWaveId() {
        return waveId;
    }

    /** 4 bit adpcm */
    public byte[] getData() {
        return data;
    }

    /** tells the sampling rate found in the "EXVO" which links to this wave */
    void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
    }

    public SmafMessage getSmafMessage() {
        WaveType waveType = new WaveType(1, 1, samplingRate, 4); // mono, adpcm, 4 bit
        return new WaveDataMessage(
                waveId,
                waveType.getWaveFormat(),
                data,
                waveType.getWaveSamplingFreq(),
                waveType.getWaveBaseBit(),
                waveType.getWaveChannels());
    }

    @Override
    public String toString() {
        return getDC().format(getId() + " waveId: " + waveId + ", " + samplingRate + "Hz, " + data.length + " bytes adpcm");
    }
}
