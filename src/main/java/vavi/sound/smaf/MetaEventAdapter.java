/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.util.logging.Level;

import javax.sound.midi.MetaEventListener;

import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.midi.MidiConstants;
import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.message.MidiContext;
import vavi.sound.smaf.message.WaveMessage;
import vavi.sound.smaf.sequencer.SmafMessageStore;
import vavi.sound.smaf.sequencer.WaveSequencer;
import vavi.util.Debug;


/**
 * a MIDI MetaEvent adapter implementation.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
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
    public SmafDevice.Info getDeviceInfo() {
        return info;
    }

    /* */
    public void close() {
    }

    /* */
    public boolean isOpen() {
        return true;
    }

    /* */
    public void open() {
    }

    /**
     * {@link SmafMessageStore} ���g�p�����Đ��@�\���������Ă��܂��B
     * @see WaveMessage#getMidiEvents(MidiContext)
     */
    public void meta(javax.sound.midi.MetaMessage message) {
//Debug.println("type: " + message.getType());
        switch (message.getType()) {
        case MidiConstants.META_MACHINE_DEPEND: // �V�[�P���T�ŗL�̃��^�C�x���g
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
        case MidiConstants.META_TEXT_EVENT:     // �e�L�X�g�E�C�x���g
        case MidiConstants.META_COPYRIGHT:      // ���쌠�\��
        case MidiConstants.META_NAME:           // �V�[�P���X���܂��̓g���b�N��
Debug.println("meta " + message.getType() + ": " + MidiUtil.getDecodedMessage(message.getData()));
            break;
        case MidiConstants.META_END_OF_TRACK:   // �g���b�N�̏I���
        case MidiConstants.META_TEMPO:          // �e���|�ݒ�
Debug.println(Level.FINE, "this handler ignore meta: " + message.getType());
            break;
        default:
Debug.println("no meta sub handler: " + message.getType());
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
Debug.println(String.format("unhandled manufacturer: %02x %02x %02x", data[0], data[1], data[2]));
            break;
        case VaviMidiDeviceProvider.MANUFACTURER_ID: // 0x5f vavi
            processSpecial_Vavi(message);
            break;
        default:
Debug.println(String.format("unhandled manufacturer: %02x", manufacturerId));
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
        case WaveSequencer.META_FUNCTION_ID_SMAF:
            processSpecial_Vavi_Wave(message);
            break;
        default:
Debug.println(String.format("unhandled function: %02x", functionId));
            break;
        }
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

/* */
