package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;

import vavi.sound.smaf.chunk.TrackChunk.FormatType;


/**
 * ChannelStatus.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public class ChannelStatus {
    /** */
    private enum Status {
        OFF,
        ON
    }

    /** */
    public enum Type {
        NoCare,
        Melody,
        NoMelody,
        Rhythm
    }

    /** smaf channel */
    private final int channel;
    /** */
    private ChannelStatus.Status keyControlStatus;
    /** */
    private ChannelStatus.Status led;
    /** */
    private final ChannelStatus.Status vibration;
    /** */
    private ChannelStatus.Type type;

    /** internal use */
    private final FormatType formatType;

    /**
     * the value as it is in a file, a nibble for {@link FormatType#HandyPhoneStandard} and a
     * byte otherwise. kept so the reserved bits survive a read/write round trip.
     */
    private final int value;

    /**
     * For HandyPhoneStandard.
     */
    public ChannelStatus(int channel, byte value) {
        this.channel = channel;
        this.value = value & 0x0f;
        setType(value & 0x03);
        setKeyControlStatusForHandyPhoneStandard((value & 0x08) >> 3);
        this.vibration = ((value & 0x04) >> 2) != 0 ? Status.ON : Status.OFF;

        formatType = FormatType.HandyPhoneStandard;
    }

    /**
     * For MobileStandard.
     */
    public ChannelStatus(int channel, int value) {
        this.channel = channel;
        this.value = value & 0xff;
        setType(value & 0x03);
        this.vibration = ((value & 0x20) >> 5) != 0 ? Status.ON : Status.OFF;
        this.led =       ((value & 0x10) >> 4) != 0 ? Status.ON : Status.OFF;
        setKeyControlStatusForMobileStandard((value & 0xc0) >> 6);

        formatType = FormatType.MobileStandard_NoCompress;
    }

    /** */
    private void setKeyControlStatusForHandyPhoneStandard(int value) {
        if (value == 1) {
            keyControlStatus = Status.ON;
        } else {
            keyControlStatus = Status.OFF;
        }
    }

    /** */
    private void setKeyControlStatusForMobileStandard(int value) {
        if (value == 0x02) {
            keyControlStatus = Status.ON;
        } else {
            // 0x00 none
            // 0x03 reserved
            keyControlStatus = Status.OFF;
        }
    }

    /** */
    void setType(int value) {
        type = Type.values()[value];
    }

    /** */
    public ChannelStatus.Type getType() {
        return type;
    }

    /**
     * the value as it is in a file, a nibble for {@link FormatType#HandyPhoneStandard} and a
     * byte otherwise.
     */
    public int getValue() {
        return value;
    }

    /**
     * Writes one channel status. Only a {@link FormatType#HandyPhoneStandard} status is a
     * nibble, so those can not be written one by one, use
     * {@link #writeTo(ChannelStatus[], FormatType, OutputStream)} instead.
     * @throws IllegalStateException when this is a {@link FormatType#HandyPhoneStandard} status
     */
    public void writeTo(OutputStream os) throws IOException {
        if (formatType == FormatType.HandyPhoneStandard) {
            throw new IllegalStateException("a HandyPhoneStandard channel status is a nibble, use #writeTo(ChannelStatus[], FormatType, OutputStream)");
        }
        os.write(value);
    }

    /**
     * Writes the Channel Status field of a Score Track Chunk.
     * <p>
     * {@link FormatType#HandyPhoneStandard} packs 4 statuses into
     * {@link FormatType#size} (2) bytes, the even channel in the high nibble, the others
     * take one byte each and the field is {@link FormatType#size} (16) bytes long.
     * </p>
     * @param channelStatuses nullable, missing channels are written as 0
     */
    public static void writeTo(ChannelStatus[] channelStatuses, FormatType formatType, OutputStream os) throws IOException {
        byte[] buffer = new byte[formatType.size];
        int channels = channelStatuses == null ? 0 : channelStatuses.length;
        if (formatType == FormatType.HandyPhoneStandard) {
            for (int i = 0; i < Math.min(channels, formatType.size * 2); i++) {
                buffer[i / 2] |= (byte) ((channelStatuses[i].getValue() & 0x0f) << (4 * ((i + 1) % 2)));
            }
        } else {
            for (int i = 0; i < Math.min(channels, formatType.size); i++) {
                buffer[i] = (byte) channelStatuses[i].getValue();
            }
        }
        os.write(buffer);
    }

    @Override
    public String toString() {
        return "channel status:[" + channel + "] type=" + type + ", KCS=" + keyControlStatus + ", LED=" + led + ", vibration=" + vibration;
    }
}
