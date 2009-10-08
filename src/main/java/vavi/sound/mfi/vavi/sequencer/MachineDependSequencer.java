/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.MachineDependMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Sub sequencer for machine depend system exclusive message.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020704 nsano initial version <br>
 */
public interface MachineDependSequencer {

    /** for {@link MachineDependSequencer} */
    public final static int META_FUNCTION_ID_MACHINE_DEPEND = 0x01;

    /** */
    void sequence(MachineDependMessage message)
        throws InvalidMfiDataException;

    /** */
    class Factory {

        /** */
        private static final String KEY_HEADER = "sequencer.vendor.";

        /**
         * @param vendor with carrier bit
         * @return same instance for each vendor
         * @throws IllegalStateException when audio engine not found
         */
        public static MachineDependSequencer getSequencer(int vendor) {
            String key = KEY_HEADER + vendor;
            if (sequencers.containsKey(key)) {
                return sequencers.get(key);
            } else {
Debug.println(Level.SEVERE, "error vendor: " + StringUtil.toHex2(vendor));
                throw new IllegalStateException("error vendor: " + StringUtil.toHex2(vendor));
            }
        }

        //---------------------------------------------------------------------

        /**
         * {@link MachineDependSequencer} オブジェクトのインスタンス集。
         * インスタンスを使いまわすのでステートレスでなければならない。
         */
        private static Map<String, MachineDependSequencer> sequencers = new HashMap<String, MachineDependSequencer>();
    
        static {
            try {
                // props
                Properties props = new Properties();
                props.load(Factory.class.getResourceAsStream("/vavi/sound/mfi/vavi/vavi.properties"));
                
                // 
                Iterator<?> i = props.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String) i.next();
                    if (key.startsWith(KEY_HEADER)) {
Debug.println("sequencer class: " + props.getProperty(key));
                        @SuppressWarnings("unchecked")
                        Class<MachineDependSequencer> clazz = (Class<MachineDependSequencer>) Class.forName(props.getProperty(key));
Debug.println("sequencer class: " + StringUtil.getClassName(clazz));
                        MachineDependSequencer sequencer = clazz.newInstance();
    
                        sequencers.put(key, sequencer);
                    }
                }
            } catch (Exception e) {
Debug.printStackTrace(e);
                System.exit(1);
            }
        }
    }
}

/* */
