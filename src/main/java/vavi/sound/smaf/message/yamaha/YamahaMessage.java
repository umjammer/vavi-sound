/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message.yamaha;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * YamahaMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050501 nsano initial version <br>
 */
public class YamahaMessage extends MachineDependentMessage
    implements MachineDependentSequencer, MidiConvertible {

    private static final Logger logger = getLogger(YamahaMessage.class.getName());

    /**
     *
     * <li>[MA-3] stream PCM pair
     * <p>
     * You can set two specified stream PCMs to sound synchronously.
     * After receiving the sync message, any note-on will cause the two sounds to be played simultaneously.
     * </p>
     * <pre>
     * ex.)F0 xx 43 79 06 7F 08 cl id1 id2 F7
     *  　　cl=00(synchronize),01(cancel)
     *    　id1=00 ~ 20(Wave ID 1)
     *    　id2=00 ~ 20(Wave ID 2)
     * </pre>
     * <li> MA-3/MA-5 stream PCM wave pan pot
     * <p>
     * Sets the stereo location position of the specified stream PCM wave.
     * </p>
     * <pre>
     * ex.)F0 xx 43 79 06 7F 0B id pp dd F7
     *  　　id=00 ~ 20(Wave ID)
     *  　　pp=00(specify),01(clear),02(off)
     *  　　dd=00 ~ 7F(localization: Center=40)
     * </pre>
     * * Once this is specified, the channel panpot (CC#10) specification will have no effect unless cleared.
     * <pre>
     * ----------------------
     *  MA-3 master volume
     *  MA-3 stream PCM pair
     *  MA-3 stream PCM wave, pan pot
     *  MA-3 interruption setting
     *  ----------------------
     * </pre>
<pre>

[???] (puc)
         43 01 80 31 xx F7
                     ~~ tempo data?　set by Mtsu

[???] (my dump)
         43 03 91 18 00 F7
         43 03 91 18 00 F7
         43 03 91 19 10 F7
         43 03 91 1A 32 F7
         43 03 91 1C 76 F7
         43 03 91 1D 98 F7

[???] (puc)
FF F0 05 43 02 80 ** F7
                  ~~ msec seems per 1 delta time

[voice setting] (puc)
FF F0 13 43 02 01 00 50 72 9B 3F C1 98 4B 3F C0 00 10 21 42 00 F7
                  ~~ ~~  1st byte is 00, 2nd byte is voice number

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

[stream PCM wave pan-pot] (proper)
      F0 43 79 06 7F 0B ii cc dd F7
          ii: WaveID 1 ~ 32 （1H ~ 20F）
          cc: specify pan-pot 0,clear 1, pan off 2
          dd: pan-pot value 0 ~ 127 (00H ~ 7FH)

[user event] (proper)
      F0 43 79 06 7F 10 dd F7
          dd: user event type 0 ~ 15 (0H ~ FH)

</pre>
     * <p>
     * Since the default MIDI sequencer is used, only meta-events can be hooked,
     * so they are converted to meta-events.
     * </p>
     * @see "http://www.music.ne.jp/~puc/mmf_format.html"
     * @see "ATS-MA5-SMAF_GL_133_HV.pdf"
     * @see "http://murachue.ddo.jp/web/softlist.cgi?mode=desc&title=mmftool"
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {

//        MidiEvent[] events = new MidiEvent[1];
//        javax.sound.midi.SysexMessage sysexMessage = new javax.sound.midi.SysexMessage();
//logger.log(Level.DEBUG, "(" + StringUtil.toHex2(command) + "): " + channel + "ch, " + StringUtil.toHex2(value));
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
        metaMessage.setMessage(0x7f,    // sequencer specific meta event
                               data,
                               data.length);

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrentTick())
        };
    }

    /* TODO super appropriate right now */
    @Override
    public void sequence() throws InvalidSmafDataException {
logger.log(Level.INFO, "yamaha: " + data.length + "\n" + StringUtil.getDump(data, 64));
        switch (data[1]) {
        case 0x79:
            switch (data[3]) {
            case 0x7f:
                switch (data[4]) {
                case 0x20: { //
logger.log(Level.DEBUG, "YAMAHA UNKNOWN: ");
                    AudioEngine engine = WaveSequencer.Factory.getAudioEngine();
                    engine.start(2);
                    break;
                }
                case 0x00: { // volume
logger.log(Level.DEBUG, "YAMAHA VOLUME: ");
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
