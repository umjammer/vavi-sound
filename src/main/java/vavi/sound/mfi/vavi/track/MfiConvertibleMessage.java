/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.vavi.MfiContext;
import vavi.sound.mfi.vavi.MfiConvertible;
import vavi.sound.mfi.vavi.MidiContext;


/**
 * mfi convertible utility facade.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071011 nsano initial version <br>
 */
public class MfiConvertibleMessage implements MfiConvertible {

    /** BANK LSB */
    private final int[] bankLSB = new int[MidiContext.MAX_MIDI_CHANNELS];
    /** BANK MSB  */
    private final int[] bankMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** */
    public static final int RPN_PITCH_BEND_SENSITIVITY = 0x0000;
    /** */
    public static final int RPN_FINE_TUNING = 0x0001;
    /** */
    public static final int RPN_COURCE_TUNING = 0x0002;
    /** */
    public static final int RPN_TUNING_PROGRAM_SELECT = 0x0003;
    /** */
    public static final int RPN_TUNING_BANK_SELECT = 0x0004;
    /** */
    public static final int RPN_NULL = 0x7f7f;

    /** RPN LSB */
    private final int[] rpnLSB = new int[MidiContext.MAX_MIDI_CHANNELS];
    /** RPN MSB */
    private final int[] rpnMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** NRPN LSB */
    private final int[] nrpnLSB = new int[MidiContext.MAX_MIDI_CHANNELS];
    /** NRPN MSB */
    private final int[] nrpnMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** bank, rpn, nrpn */
    @Override
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
//      int command = shortMessage.getCommand();
        int data1 = shortMessage.getData1();
        int data2 = shortMessage.getData2();

//      MfiMessage mfiMessage = null;

        switch (data1) {
        case 0:         // bank select MSB
            bankMSB[channel] = data2;
            break;
        case 32:        // bank select LSB
            bankLSB[channel] = data2;
            break;
        case 98:        // NRPN LSB
            nrpnLSB[channel] = data2;
            break;
        case 99:        // NRPN MSB
            nrpnMSB[channel] = data2;
            break;
        case 100:       // RPN LSB
            rpnLSB[channel] = data2;
            break;
        case 101:       // RPN MSB
            rpnMSB[channel] = data2;
            break;
//      case 6:         // data entry MSB
//          int rpn = rpnLSB[channel] & | (rpnMSB[channel] << 8);
//          switch (rpn) {
//          case RPN_PITCH_BEND_SENSITIVITY:
//              MfiConvertible converter = new vavi.sound.mfi.vavi.channel.PitchBendRangeMessage();
//              return converter.getMfiEvents(midiEvent, context);
//logger.log(Level.TRACE, "rpn: MSB:" + rpnMSB[channel] + ", LSB:" + rpnLSB[channel]);
//          default:
//logger.log(Level.TRACE, "data entry: no handler for rpn: " + rpn[channel]);
//              break;
//          }
        default:
//logger.log(Level.TRACE, "not implemented: " + data1);
            break;
        }

        return null;
    }
}
