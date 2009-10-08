/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mitsubishi;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.MachineDependMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Mitsubishi System exclusive message function 0x84 processor.
 * (Wave Packet Data3)
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030828 nsano initial version <br>
 *          0.01 030929 nsano be sub class of Function131
 */
public class Function132 extends Function131 {

    /** ���̃f�[�^�̃w�b�_������ */
    @SuppressWarnings("hiding")
    public static final int HEADER_LENGTH = 14;

    /**
     * 0x84 Wave Packet Data3    MFi2, MFi3
     * <p>
     * TODO playSize �̔��f
     * </p>
     * @param message see below
     * <pre>
     *  0       delta
     *  1       ff
     *  2       ff
     *  3-4     length
     *  5       vendor
     *  6       84
     *  :
     *  10-13   size (big endian)
     * </pre>
     */
    public void process(MachineDependMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.channel   = (data[ 7] & 0xc0) >> 6;    // 0 ~ 3
        this.packetId  =  data[ 7] & 0x3f;          // packet id 0 ~ 15
        this.mode      = (data[ 8] & 0xc0) >> 6;    // 0 ~ 3
        int format1    = (data[ 8] & 0x3c) >> 2;    // 
        int format2    =  data[ 8] & 0x03;          // 
        this.continued = (data[ 9] & 0x01) == 1;    // 
        this.length    = (data[10] << 24) +
                         (data[11] << 16) +
                         (data[12] <<  8) +
                          data[13];

        int adpcmLength = data.length - HEADER_LENGTH;
Debug.println("ADPCM voice: " + channel + "ch, No." + packetId + ", mode=" + mode + ", continued=" + continued + ", playSize=" + length + ", " + adpcmLength);
//Debug.dump(data);

        this.sampleRate = getSamplingRateInternal(format1);
        this.bits = getSamplingBitsInternal(format2);
Debug.println("sampling: " + StringUtil.toHex2(data[8] & 0x3f) + ": rate=" + sampleRate + ", bits=" + bits);

        this.adpcm = new byte[adpcmLength];
        System.arraycopy(data, HEADER_LENGTH, adpcm, 0, adpcmLength);

        play();
    }

    /** */
    protected int length;

    /**
     * @param length ��ԍŏ��̃`�����N�Ɍ㑱�S�Ă𑫂��������A����ȊO�� 0 ���w�肵�Ă��������B
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * ���O�� {@link #setAdpcm(byte[])}, {@link #setSamplingRate(int)},
     * {@link #setSamplingBits(int)} ��p���ăt�B�[���h��ݒ肵�Ă����ĉ������B
     */
    public byte[] getMessage() throws InvalidMfiDataException {

        // [14~]
        byte[] tmp = new byte[adpcm.length + 9];

        // [8]
        int format1 = getSamplingRateInternal();

        // [8]
        int format2 = getSamplingBitsInternal();

        // [5~]
        tmp[0] = (byte) (VENDOR_MITSUBISHI | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x84;
        tmp[2] = (byte) ((channel << 6) | packetId);            // 7: channel, id
        tmp[3] = (byte) ((mode << 6) | format1 | format2);      // 8: sample rate
        tmp[4] = (byte) (continued ? 0x01 : 0x00);              // 9: continued

        tmp[5] = (byte) ((length & 0xff000000) >> 24);          // 10-13: size 
        tmp[6] = (byte) ((length & 0x00ff0000) >> 16);
        tmp[7] = (byte) ((length & 0x0000ff00) >>  8);
        tmp[8] = (byte)  (length & 0x000000ff);

        System.arraycopy(adpcm, 0, tmp, 9, adpcm.length);

        return tmp;
    }
}

/* */
