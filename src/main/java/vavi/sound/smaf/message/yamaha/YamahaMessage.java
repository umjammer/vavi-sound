/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message.yamaha;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.message.MachineDependentMessage;
import vavi.sound.smaf.message.MidiContext;
import vavi.sound.smaf.message.MidiConvertible;
import vavi.sound.smaf.sequencer.MachineDependentSequencer;
import vavi.sound.smaf.sequencer.SmafMessageStore;
import vavi.sound.smaf.sequencer.WaveSequencer;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * YamahaMessage.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050501 nsano initial version <br>
 */
public class YamahaMessage extends MachineDependentMessage
    implements MachineDependentSequencer, MidiConvertible {

    /**
     *
     * <li>[MA-3] ストリームPCM ペア
     * <p>
     * 指定した二つのストリームPCMを同期発音させるよう設定できます。
     * 同期メッセージを受信後、いずれかのノート・オンで二つのサウンドが同時に発音されます。
     * </p>
     * <pre>
     * ex.)F0 xx 43 79 06 7F 08 cl id1 id2 F7
     *  　　cl=00(同期),01(解除)
     *    　id1=00〜20(Wave ID 1)
     *    　id2=00〜20(Wave ID 2)
     * </pre>
     * <li> MA-3/MA-5 ストリームPCM ウェーブ・パンポット
     * <p>
     * 指定したストリームPCMウェーブのステレオ定位位置を設定します。
     * </p>
     * <pre>
     * ex.)F0 xx 43 79 06 7F 0B id pp dd F7
     *  　　id=00〜20(Wave ID)
     *  　　pp=00(指定),01(クリア),02(オフ)
     *  　　dd=00〜7F(定位：Center=40)
     * </pre>
     * ※ 一度これを指定した場合、クリアしない限りチャンネル・パンポット(CC#10)の指定は効果ありません。
     * <pre>
     * ----------------------
     *  MA-3 マスター・ボリューム
     *  MA-3 ストリームPCMペア
     *  MA-3 ストリームPCMウェーブ・パンポット
     *  MA-3 割り込み設定
     *  ----------------------
     * </pre>
<pre>

[???] (puc)
         43 01 80 31 xx F7
                     ~~ テンポデータ？　Mtsu で指定したもの

[???] (my dump)
         43 03 91 18 00 F7
         43 03 91 18 00 F7
         43 03 91 19 10 F7
         43 03 91 1A 32 F7
         43 03 91 1C 76 F7
         43 03 91 1D 98 F7

[???] (puc)
FF F0 05 43 02 80 ** F7
                  ~~ 1 デルタタイムあたりの msec らしい

[音色設定] (puc)
FF F0 13 43 02 01 00 50 72 9B 3F C1 98 4B 3F C0 00 10 21 42 00 F7
                  ~~ ~~  1 バイト目は 00 2 バイト目が音色番号

[FMAll4HPS] (smaftool)
         43 03 00 00 47 50 01 25 1B 92 42 A0 14 72 71 00 A0 F7
               ~~ ~~ 1: no, 2: 00 or 0x80

[MA-3 SetVoiceFM(0x1f,0x2f)/MA-3 SetVoiceWT(0x1e)] (smaftool)
         43 79 06 7F 01 xx tt nn

[MA-5 SetVoiceFM(0x1c,0x2a)/MA-5 SetVoiceWT(0x1b)] (smaftool)
         43 79 07 7F 01

[Reset] (smaftool)
         43 79    7F 7F

[Volume] (smaftool)
         43 79    7F 00

[???] (smaftool)
         43 79    7F 07

[MA-3,5 SetWave] (smaftool)
         43 79    7F 03

[ストリームPCM ウェーブパンポット] (proper)
      F0 43 79 06 7F 0B ii cc dd F7
          ii: WaveID 1〜32 （1H〜20F）
          cc: パンポット指定 0、クリア 1、パンオフ 2
          dd: パンポット値0〜127 (00H〜7FH)

[ユーザーイベント] (proper)
      F0 43 79 06 7F 10 dd F7
          dd: ユーザーイベント種別 0〜15 (0H〜FH)

</pre>
     * <p>
     * デフォルトの MIDI シーケンサを使用するため、メタイベントしかフックできないので
     * メタイベントに変換している。
     * </p>
     * @see "http://www.music.ne.jp/~puc/mmf_format.html"
     * @see "ATS-MA5-SMAF_GL_133_HV.pdf"
     * @see "http://murachue.ddo.jp/web/softlist.cgi?mode=desc&title=mmftool"
     */
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {

//        MidiEvent[] events = new MidiEvent[1];
//        javax.sound.midi.SysexMessage sysexMessage = new javax.sound.midi.SysexMessage();
//Debug.println("(" + StringUtil.toHex2(command) + "): " + channel + "ch, " + StringUtil.toHex2(value));
//        byte[] temp = new byte[data.length + 1];
//        temp[0] = (byte) 0xf0;
//        System.arraycopy(data, 0, temp, 1, data.length);
//        sysexMessage.setMessage(temp, temp.length);
//        events[0] = new MidiEvent(sysexMessage, context.getCurrentTick());
//        return events;

        MetaMessage metaMessage = new MetaMessage();

        int id = SmafMessageStore.put(this);
        byte[] data = {
            VaviMidiDeviceProvider.MANUFACTURER_ID,
            MachineDependentSequencer.META_FUNCTION_ID_MACHINE_DEPEND,
            (byte) ((id / 0x100) & 0xff),
            (byte) ((id % 0x100) & 0xff)
        };
        metaMessage.setMessage(0x7f,    // シーケンサー固有メタイベント
                               data,
                               data.length);

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrentTick())
        };
    }

    /* TODO 今、超適当 */
    public void sequence() throws InvalidSmafDataException {
Debug.println("yamaha: " + data.length + "\n" + StringUtil.getDump(data, 64));
        switch (data[1]) {
        case 0x79:
            switch (data[3]) {
            case 0x7f:
                switch (data[4]) {
                case 0x20: { //
Debug.println("YAMAHA UNKNOWN: ");
                    AudioEngine engine = WaveSequencer.Factory.getAudioEngine();
                    engine.start(2);
                    break;
                }
                case 0x00: { // volume
Debug.println("YAMAHA VOLUME: ");
                    AudioEngine engine = WaveSequencer.Factory.getAudioEngine();
                    engine.start(1);
                    break;
                }
                }
                break;
            }
            break;
        }
    }
}

/* */
