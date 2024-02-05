/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.util.logging.Level;

import javax.sound.midi.MetaEventListener;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.midi.MidiUtil;
import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.smaf.message.MidiContext;
import vavi.sound.smaf.message.WaveMessage;
import vavi.sound.smaf.sequencer.MachineDependentSequencer;
import vavi.sound.smaf.sequencer.SmafMessageStore;
import vavi.sound.smaf.sequencer.WaveSequencer;
import vavi.util.Debug;


/**
 * a MIDI MetaEvent adapter implementation.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
class MetaEventAdapter implements MetaEventListener, SmafDevice {

    /** the device information */
    private static final SmafDevice.Info info =
        new SmafDevice.Info("Java SMAF WAVE Sequencer",
                           "Vavisoft",
                           "Software sequencer using wave",
                           "Version " + SmafDeviceProvider.version) {};

    /* */
    @Override
    public SmafDevice.Info getDeviceInfo() {
        return info;
    }

    /* */
    @Override
    public void close() {
    }

    /* */
    @Override
    public boolean isOpen() {
        return true;
    }

    /* */
    @Override
    public void open() {
    }

    /**
     * {@link SmafMessageStore} を使用した再生機構を実装しています。
     * @see WaveMessage#getMidiEvents(MidiContext)
     */
    @Override
    public void meta(javax.sound.midi.MetaMessage message) {
//Debug.println("type: " + message.getType());
        switch (MetaEvent.valueOf(message.getType())) {
        case META_MACHINE_DEPEND: // シーケンサ固有のメタイベント
            try {
                processSpecial(message);
            } catch (InvalidSmafDataException e) {
                Debug.printStackTrace(e.getCause());
            }
catch (RuntimeException e) {
Debug.printStackTrace(e);
} catch (Error e) {
Debug.printStackTrace(e);
throw e;
}
            break;
        case META_TEXT_EVENT:     // テキスト・イベント
        case META_COPYRIGHT:      // 著作権表示
        case META_NAME:           // シーケンス名またはトラック名
Debug.println(Level.FINE, "meta " + message.getType() + ": " + MidiUtil.getDecodedMessage(message.getData()));
            break;
        case META_END_OF_TRACK:   // トラックの終わり
        case META_TEMPO:          // テンポ設定
Debug.println(Level.FINE, "this handler ignore meta: " + message.getType());
            break;
        default:
Debug.println(Level.FINE, "no meta sub handler: " + message.getType());
            break;
        }
    }

    /**
     * meta machine depend: 0x7f
     * <pre>
     * 0x7f manufacturerId
     * </pre>
     */
    private void processSpecial(javax.sound.midi.MetaMessage message)
        throws InvalidSmafDataException {

        byte[] data = message.getData();
        int manufacturerId = data[0];
        switch (manufacturerId) {
        case 0:     // 3 byte manufacturer id
Debug.printf(Level.WARNING, "unhandled manufacturer: %02x %02x %02x\n", data[0], data[1], data[2]);
            break;
        case VaviMidiDeviceProvider.MANUFACTURER_ID: // 0x5f vavi
            processSpecial_Vavi(message);
            break;
        default:
Debug.printf(Level.WARNING, "unhandled manufacturer: %02x\n", manufacturerId);
            break;
        }
    }

    /**
     * manufacturerId 0x5f: vavi
     * <pre>
     * 0x5f functionId
     * </pre>
     */
    private void processSpecial_Vavi(javax.sound.midi.MetaMessage message)
        throws InvalidSmafDataException {

        byte[] data = message.getData();
        int functionId = data[1];
        switch (functionId) {
        case MachineDependentSequencer.META_FUNCTION_ID_MACHINE_DEPEND:
            processSpecial_Vavi_MachineDependent(message);
            break;
        case WaveSequencer.META_FUNCTION_ID_SMAF:
            processSpecial_Vavi_Wave(message);
            break;
        default:
Debug.printf(Level.WARNING, "unhandled function: %02x\n", functionId);
            break;
        }
    }

    /**
     * <pre>
     * 0x5f 0x01
     * </pre>
     */
    private void processSpecial_Vavi_MachineDependent(javax.sound.midi.MetaMessage message)
        throws InvalidSmafDataException {

        byte[] data = message.getData();
        int id = (data[2] & 0xff) * 0xff + (data[3] & 0xff);
//Debug.println("message id: " + id);
        MachineDependentSequencer sequencer = (MachineDependentSequencer) SmafMessageStore.get(id);
        sequencer.sequence();
    }

    /**
     * functionId 0x03: wave
     * <pre>
     * 0x5f 0x03 id(H) id(L)
     * </pre>
     */
    private void processSpecial_Vavi_Wave(javax.sound.midi.MetaMessage message)
        throws InvalidSmafDataException {

        byte[] data = message.getData();
        int id = (data[2] & 0xff) * 0x100 + (data[3] & 0xff);
//Debug.println("message id: " + id);
        WaveSequencer sequencer = (WaveSequencer) SmafMessageStore.get(id);
        sequencer.sequence();
    }
}
