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
     * For HandyPhoneStandard.
     */
    public ChannelStatus(int channel, byte value) {
        this.channel = channel;
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

    /** TODO */
    public void writeTo(OutputStream os) throws IOException {
        switch (formatType) {
        case HandyPhoneStandard:
            break;
        case MobileStandard_Compress:
            break;
        default:
            break;
        }
    }

    /** */
    public String toString() {
        return "channel status:[" + channel + "] type=" + type + ", KCS=" + keyControlStatus + ", LED=" + led + ", vibration=" + vibration;
    }
}
