/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import vavi.sound.smaf.SysexMessage;


/**
 * Non Realtime Universal System exclusive message.
 * <pre>
 * [MIDI]
 *  FF 7E ... F7
 * </pre>
 * <pre>
 *  0xf0
 *  0xfe
 *  SUB-ID#1 | SUB-ID#2   | はたらき 
 *  ---------|------------|-----------------------
 *   00H     | --         | 未使用
 *   01H     | (not used) | Sample Dump Header
 *   02H     | (not used) | Sample Data Packet
 *   03H     | (not used) | Sample Dump Request
 *   04H     | nn         | MIDI Time Code
 *           | 00H  　    |  Special
 *           | 01H        |  Punch In Points
 *           | 02H        |  Punch Out Points
 *           | 03H        |  Delete Punch In Point
 *           | 04H  　    |  Delete Punch Out Point
 *           | 05H  　    |  Event Start Point
 *           | 06H  　    |  Event Stop Point
 *           | 07H  　    |  Event Start Points with additional info.
 *           | 08H  　    |  Event Stop Points with additional info.
 *           | 09H  　    |  Delete Event Start Point
 *           | 0AH  　    |  Delete Event Stop Point
 *           | 0BH  　    |  Cue Points
 *           | 0CH        |  Cue Points with additional info.
 *           | 0DH        |  Delete Cue Point
 *           | 0EH  　    |  Event Name in additional info.
 *   05H     | nn         | Sample Dump Extensions
 *           | 01H  　    |  Multiple Loop Points
 *           | 02H  　    |  Loop Points Request
 *   06H     | nn         | General Information
 *           | 01H  　    |  Identity Request
 *           | 02H  　    |  Identity Reply
 *   07H     | nn         | File Dump
 *           | 01H  　    |  Header
 *           | 02H  　    |  Data Packet
 *           | 03H  　    |  Request
 *   08H     | nn         | MIDI Tuning Standard 
 *           | 00H      　|  Bulk Dump Request
 *           | 01H  　    |  Bulk Dump Reply
 *   09H     | nn         | General MIDI
 *           | 01H        |  General MIDI System On
 *           | 02H  　    |  General MIDI System Off
 *   7BH     | (not used) | End Of File
 *   7CH     | (not used) | Wait
 *   7DH     | (not used) | Cancel 
 *   7EH     | (not used) | NAK 
 *   7FH     | (not used) | ACK 
 * </pre>
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class NonRealtimeUniversalSysexMessage extends SysexMessage {

}

/* */
