/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * System exclusive message sequencer factory.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030618 nsano initial version <br>
 */
public class MachineDependFunctionFactory {
    /** */
    public static final String KEY_HEADER = "function.";

    /** MachineDependFunction オブジェクトのインスタンス集 */
    private Map<String, MachineDependFunction> functions = new HashMap<String, MachineDependFunction>();

    /** */
    public MachineDependFunctionFactory(String propsName) {
        try {
            // props
            Properties props = new Properties();
            props.load(MachineDependFunctionFactory.class.getResourceAsStream(propsName));
            
            // function
            Iterator<?> i = props.keySet().iterator();
            while (i.hasNext()) {
                String key = (String) i.next();
                if (key.startsWith(KEY_HEADER)) {
Debug.println("function class: " + props.getProperty(key));
                    @SuppressWarnings("unchecked")
                    Class<MachineDependFunction> clazz = (Class<MachineDependFunction>) Class.forName(props.getProperty(key));
Debug.println("function class: " + StringUtil.getClassName(clazz));
                    MachineDependFunction function = clazz.newInstance();

                    functions.put(key, function);
                }
            }
        } catch (Exception e) {
Debug.printStackTrace(e);
            System.exit(1);
        }
    }

    /** */
    public MachineDependFunction getFunction(String key) {
        if (functions.containsKey(key)) {
            return functions.get(key);
        } else {
            return new UndefinedFunction(); // TODO should throw exception
        }
    }
}

/* */
