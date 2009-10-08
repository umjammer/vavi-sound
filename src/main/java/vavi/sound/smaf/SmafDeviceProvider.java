/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import vavi.util.Debug;


/**
 * {@link SmafDeviceProvider} implemented by vavi.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 071012 nsano initial version <br>
 */
public class SmafDeviceProvider {

    /** */
    public static final String version = "0.21";

    /** */
    private SmafDevice.Info smafDeviceInfos[];

    /** */
    public SmafDeviceProvider() {
        this.smafDeviceInfos = Factory.getSmafDeviceInfos();
    }

    /** */
    public boolean isDeviceSupported(SmafDevice.Info info) {
        for (SmafDevice.Info smafDeviceInfo : smafDeviceInfos) {
            if (smafDeviceInfo.equals(info)) {
                return true;
            }
        }
        return false;
    }

    /** */
    public SmafDevice.Info[] getDeviceInfo() {
        return smafDeviceInfos;
    }

    /** */
    public SmafDevice getDevice(SmafDevice.Info info)
        throws IllegalArgumentException {

        return Factory.getSmafDevice(info);
    }

    /** */
    private static class Factory {

        /** */
        public static SmafDevice.Info[] getSmafDeviceInfos() {
            List<SmafDevice.Info> tmp = new ArrayList<SmafDevice.Info>();

            for (SmafDevice.Info smafDeviceInfo : deviceMap.keySet()) {
                tmp.add(smafDeviceInfo);
            }

            return tmp.toArray(new SmafDevice.Info[tmp.size()]);
        }

        /** */
        public static SmafDevice getSmafDevice(SmafDevice.Info smafDeviceInfo) {
            if (deviceMap.containsKey(smafDeviceInfo)) {
                try {
                    return deviceMap.get(smafDeviceInfo).newInstance();
                } catch (Exception e) {
Debug.printStackTrace(e);
                }
            }

            throw new IllegalArgumentException(smafDeviceInfo + " is not supported");
        }

        /** */
        private static Map<SmafDevice.Info, Class<SmafDevice>> deviceMap = new HashMap<SmafDevice.Info, Class<SmafDevice>>();

        /* */
        static {
            try {
                // props
                Properties props = new Properties();
                final String path = "/vavi/sound/smaf/smaf.properties";
                props.load(Factory.class.getResourceAsStream(path));
    
                // midi
                Iterator<?> i = props.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String) i.next();
                    if (key.startsWith("smaf.device.")) {
                        @SuppressWarnings("unchecked")
                        Class<SmafDevice> deviceClass = (Class<SmafDevice>) Class.forName(props.getProperty(key));
//Debug.println("smaf device class: " + StringUtil.getClassName(clazz));
                        SmafDevice.Info smafDeviceInfo = deviceClass.newInstance().getDeviceInfo();

                        deviceMap.put(smafDeviceInfo, deviceClass);
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
