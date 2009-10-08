/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
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
import vavi.sound.mobile.AudioEngine;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * AudioData message sequencer.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 070119 nsano initial version <br>
 * @since MFi 4.0
 */
public interface AudioDataSequencer {

    /** for {@link AudioDataSequencer} */
    final int META_FUNCTION_ID_MFi4 = 0x02;

    /** */
    void sequence() throws InvalidMfiDataException;

    /** */
    class Factory {

        /** */
        private static ThreadLocal<AudioEngine> audioEngineStore = new ThreadLocal<AudioEngine>();

        /**
         * Second time or lator. 
         */
        public static AudioEngine getAudioEngine() {
            return audioEngineStore.get();
        }

        /**
         * First time.
         * @return same instance for each format
         * @throws IllegalStateException when audio engine not found
         */
        public static AudioEngine getAudioEngine(int format) {
            String key = "audioEngine.format." + format;
            if (engines.containsKey(key)) {
                AudioEngine engine = engines.get(key);
                audioEngineStore.set(engine);
                return engine;
            } else {
Debug.println(Level.SEVERE, "error format: " + StringUtil.toHex2(format));
                throw new IllegalStateException("error format: " + StringUtil.toHex2(format));
            }
        }

        //---------------------------------------------------------------------

        /**
         * {@link AudioEngine} オブジェクトのインスタンス集。
         * インスタンスを使いまわすのでステートレスでなければならない。
         */
        private static Map<String, AudioEngine> engines = new HashMap<String, AudioEngine>();
    
        static {
            try {
                // props
                Properties props = new Properties();
                props.load(Factory.class.getResourceAsStream("/vavi/sound/mfi/vavi/vavi.properties"));
                
                // 
                Iterator<?> i = props.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String) i.next();
                    if (key.startsWith("audioEngine.format.")) {
                        @SuppressWarnings("unchecked")
                        Class<AudioEngine> clazz = (Class<AudioEngine>) Class.forName(props.getProperty(key));
Debug.println("audioEngine class: " + StringUtil.getClassName(clazz));
                        AudioEngine engine = clazz.newInstance();
    
                        engines.put(key, engine);
                    }
                }
            } catch (Exception e) {
Debug.printStackTrace(e);
                throw (RuntimeException) new IllegalStateException().initCause(e);
            }
        }
    }
}

/* */
