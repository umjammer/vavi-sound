/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.oki;

import javax.sound.sampled.AudioFormat;

import vavi.sound.adpcm.Codec;


/**
 * OKI MSM6258 ADPCM voice synthesizer codec.
 * <p>
 * TODO 8 bit �Ή�
 * </p>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030816 nsano initial version <br>
 */
class Oki implements Codec {

    /** */
    private AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;

    /** */
    public void setEncoding(AudioFormat.Encoding encoding) {
        this.encoding = encoding;
    }

    /** */
    private Ulaw ulaw = new Ulaw();
    /** */
    private Alaw alaw = new Alaw();

    /** */
    private int mc_amp;
    /** */
    private int mc_estim;

    /** */
    public Oki() {
        ulaw = new Ulaw();
        ulaw.setEncoding(AudioFormat.Encoding.PCM_SIGNED);
        ulaw.setBit(16);
        alaw = new Alaw();
        alaw.setEncoding(AudioFormat.Encoding.PCM_SIGNED);
        alaw.setBit(16);
    }

    /** ����� PCM �l��\�����邽�߂̃e�[�u�� */
    private static final int[] adpcm_estimindex = {
        2,  6,  10,  14,  18,  22,  26,  30,
        -2, -6, -10, -14, -18, -22, -26, -30
    };

    /** �ʎq���� */
    private static final int[] adpcm_estim = {
        16,  17,  19,  21,  23,  25,  28,  31,  34,  37,
        41,  45,  50,  55,  60,  66,  73,  80,  88,  97,
        107, 118, 130, 143, 157, 173, 190, 209, 230, 253,
        279, 307, 337, 371, 408, 449, 494, 544, 598, 658,
        724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552
    };

    /** ������e�[�u��? */
    private static final int[] adpcm_estimstep = {
        -1, -1, -1, -1, 2, 4, 6, 8,
        -1, -1, -1, -1, 2, 4, 6, 8
    };

    /**
     * signed linear 16 �� 1 �T���v���� Oki ADPCM 1 �T���v���ɕϊ����܂��B
     * <p>
     * MSM6258 �����҂��� PCM �� 12bit �����t PCM (slinear12 �ɂ�����) �ł���B
     * �]���Ă��̊֐��ł͓����ɐU���ϊ�(16bit -> 12bit) ���s�Ȃ��Ă���B
     * </p>
     * 
     * @param a 16bit signed linear pcm
     * @return 4bit oki adpcm
     */
    private int encodeInternal(int a) {

        // mc->mc_estim �ɂ́A�O��̍�����\���l�C���f�b�N�X�������Ă���
        int estim = this.mc_estim;
        int b;
        int s;
        
        // df �́A�� PCM �l a �ƑO��̗\�� PCM �l�Ƃ̍����ł���
        int df = a - this.mc_amp;
        // dl �́A������\���l�e�[�u�� adpcm_estim[] ����A�\�����ꂽ
        // ����������o��������
        int dl = adpcm_estim[estim];
        // c �́A������ 12bit �ɕϊ����A(dl/8) �ɑ΂���䗦���o�������́B
        // /16 �� 16bit �� 12bit �Ƃ̍��A���Ȃ킿 2^4 �ł���B
        // *8 �́Adl ���Ȃ킿 adpcm_estim[] �̒l��8�{�l�ŋL�^���Ă��邱��
        // �ɋN������
        int c = (df / 16) * 8 / dl;
        // c �����������ɂ���ď������ς��B
        // ������ c �͏��Z�ɂ�� 0 �ɂȂ��Ă��邩���m�ꂸ�A���̏ꍇ���Ƃ���
        // �����Ă��܂��̂�����邽�ߕ�������ɂ� df ��p���Ă���B
        //
        // ���ۂɃG���R�[�h���� ADPCM �f�[�^�́A�����r�b�g�ƐU���r�b�g��
        // �g�ݍ��킹�ł���B�U���r�b�g�� c �� 2 �Ŋ���������
        if (df < 0) {
            b = -c / 2;
            s = 0x08;
        } else {
            b = c / 2;
            s = 0;
        }
        // �U���� 3bit �Ȃ̂ŁA7 �Ő�������
        if (b > 7) {
            b = 7;
        }
        // �������Ă������ƂŁA�Ȍ� s �͕����� 4bit�B
        // b �͕����Ȃ��̐�Βl�Ƃ��Ďg�������邱�Ƃ��ł���
        s |= b;
        // �����܂ł̕ϊ��ŁA�U���r�b�g b �̎��ۂ̔䗦 c �Ƃ̊֌W�́A
        // b : �䗦�͈� c
        // 0 : 0 <= �䗦 < 2
        // 1 : 2 <= �䗦 < 4
        // 2 : 4 <= �䗦 < 6
        // 3 : 6 <= �䗦 < 8
        // 4 : 8 <= �䗦 < 10
        // 5 : 10 <= �䗦 < 12
        // 6 : 12 <= �䗦 < 14
        // 7 : 14 <= �䗦
        // �̂悤�ɂȂ�B
        //
        // �����PCM�l��\������B�������������肵�Ă��邪�A���ۂɂ�
        //
        //  static int adpcm_estimindex_0[16] = {
        //    1,  3,  5,  7,  9,  11,  13,  15,
        //   -1, -3, -5, -7, -9, -11, -13, -15
        //  };
        //  mc->mc_amp += (short) (adpcm_estimindex_0[(int) s] * 16 / 8 * dl);
        //
        // �Ȃ̂ł���Badpcm_estimindex_0[] �� adpcm_estimindex[] ��
        // 1/2 �l�ł���A���̐���̈Ӗ��͏�L�䗦�͈͂̒����l�ł���B
        // ���̔䗦�ɁA16 = 2 ^ 4 ���|���� 16 bit �����A(dl / 8) ���|���邱�Ƃ�
        // ����ė\�������l�����߂��A������L�^����̂ł���B
        // ���������킯�ŁAadpcm_estimindex[] �����炩����2�{���Ă�����
        // �������肷��̂ł���B
        this.mc_amp += adpcm_estimindex[s] * dl;
        // b �̒l�ɏ]���āA����g�p���鍷�����\���� mc->mc_estim ��
        // �ۑ����Ă����B�S���� 49 �i�K�B
        // �]�k�ł��邪�A������������ adpcm_estimstep[16] �� [8] �ł悢�B
        // [16] �Ȃ̂� adpcm2pcm �Ƃ̊֌W��K�v������ł���B
        estim += adpcm_estimstep[b];
        if (estim < 0) {
            estim = 0;
        } else if (estim > 48) {
            estim = 48;
        }
        
        this.mc_estim = estim;
        return s;
    }

    /**
     * 16 �� 1 �T���v���� Oki ADPCM 1 �T���v���ɕϊ����܂��B
     * 
     * @param pcm pcm
     * @return 4bit oki adpcm
     */
    public int encode(int pcm) {
        if (AudioFormat.Encoding.ALAW.equals(encoding)) {
            return encodeInternal(alaw.decode(pcm));
        } else if (AudioFormat.Encoding.ULAW.equals(encoding)) {
            return encodeInternal(ulaw.decode(pcm));
        } else if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
            return encodeInternal(pcm);
        } else if (AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)) {
            return encodeInternal(pcm ^ 0x8000);	// TODO
        } else {
            throw new IllegalArgumentException(encoding.toString());
        }
    }

    /**
     * Oki ADPCM 1 �T���v���� signed linear 16 �� 1 �T���v���ɕϊ����܂��B
     * <p>
     * MSM6258 ���o�͂��� PCM �� 12bit �����t PCM (slinear12 �ɂ�����) �ł���B
     * �]���Ă��̊֐��ł͓����ɐU���ϊ�(12bit -> 16bit) ���s�Ȃ��Ă���B
     * </p>
     * 
     * @param b 4bit adpcm
     * @return 16bit linear pcm
     */
    private int decodeInternal(int b) {
        // mc->mc_estim �ɂ́A�O��̍�����\���l�C���f�b�N�X�������Ă���
        int estim = this.mc_estim;
        
        // �� PCM �l���v�Z���Ă���B�{���̎���
        //
        //  mc->mc_amp += adpcm_estim[estim] / 8 * adpcm_estimindex_0[b] * 16;
        //
        // �ł���Bpcm2adpcm_step() �ł��q�ׂ��ʂ� adpcm_estim[] ��8�{�l��
        // ����̂� 8 �Ŋ���B����ɔ䗦�ł��� adpcm_estimindex_0[] ���|����B
        // �X�� 12bit -> 16bit �ϊ��̂��߂� 16 (= 2^4) ���|���Ă���B
        // adpcm_estimindex_0[] * 2 �� adpcm_estimindex[] �ł���̂ŁA
        // �㎮�͎��ۂɎg���Ă���ȉ��̎��ƂȂ�
        this.mc_amp += adpcm_estim[estim] * adpcm_estimindex[b];
        // ����̍������\������ mc->mc_estim �ɕۑ����Ă���
        estim += adpcm_estimstep[b];
        
        if (estim < 0) {
            estim = 0;
        } else if (estim > 48) {
            estim = 48;
        }
        
        this.mc_estim = estim;
        
        return this.mc_amp;
    }

    /**
     * Oki ADPCM 1 �T���v���� pcm �� 1 �T���v���ɕϊ����܂��B
     * @param	adpcm	4bit adpcm
     * @return	pcm
     */
    public int decode(int adpcm) {
        if (AudioFormat.Encoding.ALAW.equals(encoding)) {
            return alaw.encode(decodeInternal(adpcm));
        } else if (AudioFormat.Encoding.ULAW.equals(encoding)) {
            return ulaw.decode(decodeInternal(adpcm));
        } else if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
            return decodeInternal(adpcm);
        } else if (AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)) {
            return decodeInternal(adpcm) ^ 0x8000;	// TODO
        } else {
            throw new IllegalArgumentException(encoding.toString());
        }
    }
}

/* */
