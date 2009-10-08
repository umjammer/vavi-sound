/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import vavi.util.Debug;


/**
 * MfiConvertibleFactory. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 070731 nsano initial version <br>
 */
public final class MfiConvertibleFactory {

    /* debug */
    private static Set<String> uc = new HashSet<String>();

    /** */
    public static MfiConvertible getConverter(String key) {
        MfiConvertible converter = converters.get(key);
if (converter == null) {
 if (!uc.contains(key)) {
  Debug.println("no converter for: " + key);
  uc.add(key);
 }
//} else {
//Debug.println("converter: " + StringUtil.getClassName(converter.getClass()));
}
        return converter;
    }

    /** */
    private static Map<String, MfiConvertible> converters = new HashMap<String, MfiConvertible>();

    //----

    static {
        try {
            // props
            Properties props = new Properties();
            final String path = "vavi.properties";
            props.load(MfiConvertibleFactory.class.getResourceAsStream(path));

            // midi
            Iterator<?> i = props.keySet().iterator();
            while (i.hasNext()) {
                String key = (String) i.next();
                if (key.startsWith("midi.")) {
                    @SuppressWarnings("unchecked")
                    Class<MfiConvertible> clazz = (Class<MfiConvertible>) Class.forName(props.getProperty(key));
//Debug.println("midi -> mfi class: " + StringUtil.getClassName(clazz));
                    MfiConvertible converter = clazz.newInstance();

                    converters.put(key, converter);
                }
            }
        } catch (Exception e) {
Debug.printStackTrace(e);
            System.exit(1);
        }
    }
}

/* */
