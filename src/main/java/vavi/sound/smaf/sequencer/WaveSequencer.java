/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.sequencer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import vavi.sound.mobile.AudioEngine;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * WaveSequencer. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public interface WaveSequencer {

    /** for {@link WaveSequencer} */
    final int META_FUNCTION_ID_SMAF = 0x03;

    /** */
    void sequence() throws InvalidSmafDataException;

    /** */
    class Factory {

        /** */
        private static ThreadLocal<AudioEngine> audioEngineStore = new ThreadLocal<AudioEngine>();

        /**
         * Second time or later. 
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
//Debug.println("format: " + format);
            String key = "audioEngine.format." + format;
            if (engines.containsKey(key)) {
                AudioEngine engine = engines.get(key);
                audioEngineStore.set(engine);
                return engine;
            } else {
Debug.println(Level.SEVERE, "error format: " + format);
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
                props.load(Factory.class.getResourceAsStream("/vavi/sound/smaf/smaf.properties"));
                
                // 
                Iterator<?> i = props.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String) i.next();
                    if (key.startsWith("audioEngine.format.")) {
//Debug.println("audioEngine key: " + key);
                        @SuppressWarnings("unchecked")
                        Class<AudioEngine> clazz = (Class<AudioEngine>) Class.forName(props.getProperty(key));
Debug.println("audioEngine class: " + StringUtil.getClassName(clazz));
                        AudioEngine engine = clazz.newInstance();
    
                        engines.put(key, engine);
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
