/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.mfi;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import vavi.util.Debug;

/**
 * MachineDependMfiWithVoiceMakerFactory. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 070428 nsano initial version <br>
 */
class MachineDependMfiWithVoiceMakerFactory {
    /** */
    private static Map<String, MachineDependMfiWithVoiceMaker> mdvms = new HashMap<String, MachineDependMfiWithVoiceMaker>();

    /** */
    static MachineDependMfiWithVoiceMaker getMachineDependMfiWithVoiceMaker(String model) {
        return mdvms.get(model);
    }

    /** */
    static {
        try {
            Properties props = new Properties();
            props.load(DividedMfiWithVoiceMaker.class.getResourceAsStream("MfiWithVoiceMaker.properties"));

            Enumeration<?> e = props.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                if (key.startsWith("class.")) {
                    String className = props.getProperty(key);
                    @SuppressWarnings(value={"unchecked"})
                    Class<MachineDependMfiWithVoiceMaker> clazz = (Class<MachineDependMfiWithVoiceMaker>) Class.forName(className);
                    MachineDependMfiWithVoiceMaker mdvm = clazz.newInstance();
                    mdvms.put(key.substring(key.indexOf('.') + 1), mdvm);
                }
            }
        } catch (Exception e) {
            Debug.printStackTrace(e);
        }
    }
}

/* */
