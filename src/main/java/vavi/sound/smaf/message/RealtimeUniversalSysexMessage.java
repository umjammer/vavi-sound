/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import vavi.sound.smaf.SysexMessage;


/**
 * Realtime Universal System exclusive message.
 * <pre>
 * [MIDI]
 *  FF 7F ... F7
 * </pre>
 * <pre>
 *  0xf0
 *  0xff
 *  SUB-ID#1 | SUB-ID#2 | はたらき
 *  ---------+----------+------------------------------------------
 *   00H     | --       | 未使用
 *   01H     | nn       | MIDI Time Code
 *           | 01H  　  |  Full Message
 *           | 02H  　  |  User Bits
 *   02H     | nn       | MIDI Show Control
 *           | 00H  　  |  MSC Extensions
 *           | 01H〜7FH |  MSC Commands
 *   03H     | nn       | Notation Information
 *           | 01H  　  |  Bar Number
 *           | 02H  　  |  Time Signature(Immediate)
 *           | 42H  　  |  Time Signature(Delayed)
 *   04H     | nn       | Device Control
 *           | 01H  　  |  Master Volume
 *           | 02H  　  |  Master Ballance
 *   05H     | nn       | Real Time MTC Cueing
 *           | 00H  　  |  Special
 *           | 01H  　  |  Punch In Points
 *           | 02H  　  |  Punch Out Points
 *           | 03H  　  |  (Reserved)
 *           | 04H  　  |  (Reserved)
 *           | 05H  　  |  Event Start Points
 *           | 06H  　  |  Event Stop Points
 *           | 07H  　  |  Event Start Points with additional info.
 *           | 08H  　  |  Event Stop Points with additional info.
 *           | 09H  　  |  (Reserved)
 *           | 0AH  　  |  (Reserved)
 *           | 0BH  　  |  Cue Points
 *           | 0CH  　  |  Cue Points with additional info.
 *           | 0DH  　  |  (Reserved)
 *           | 0EH  　  |  Event Name in additional info.
 *   06H     | nn       | MIDI Machine Control Commands
 *           | 00H〜7FH |  MMC Commands
 *   07H     | nn       | MIDI Machine Control Responses
 *           | 00H〜7FH |  MMC Commands
 *   08H     | nn       | MIDI Tuning Standard
 *           | 02H   　 |  Note Change
 * </pre>
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 * @see "ATS-MA5-SMAF_GL_133_HV.pdf"
 */
public class RealtimeUniversalSysexMessage extends SysexMessage {

    /**

[マスターボリューム]
      F0 7F 7F 04 01 ll mm F7
          nn=0〜7F(音量：0〜127) ※ 初期値はnn=64 (音量：100)

[Master Fine Tuning]
      F0h 7F 7F 04 03 vh vl F7
          マスター・ファイン・チューンを設定します。
          A440Hzからのチューニングをセント単位で指定します。

[Master Coarse Tuning]
      F0 7F 7F 04 04 00 vl F7
          マスター・コース・チューンを設定します。
          A440Hzからのチューニングを100[cent]単位で指定します。

[GM SYSTEM ON]
      F0 7E 7F 09 01 F7

[GM2 SYSTEM ON]
      F0 7E 7F 09 03 F7

[GM SYSTEM OFF]
      F0 7E 7F 09 02 F7

     */
}

/* */
