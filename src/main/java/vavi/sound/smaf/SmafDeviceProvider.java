/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import vavi.util.Debug;


/**
 * {@link SmafDeviceProvider} implemented by vavi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071012 nsano initial version <br>
 */
public class SmafDeviceProvider {

    /** */
    public static final String version = "1.0.10";

    /** */
    private SmafDevice.Info[] smafDeviceInfos;

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

            List<SmafDevice.Info> tmp = new ArrayList<>(deviceMap.keySet());

            return tmp.toArray(new SmafDevice.Info[0]);
        }

        /** */
        public static SmafDevice getSmafDevice(SmafDevice.Info smafDeviceInfo) {
            if (deviceMap.containsKey(smafDeviceInfo)) {
                try {
                    return deviceMap.get(smafDeviceInfo).getDeclaredConstructor().newInstance();
                } catch (Exception e) {
Debug.printStackTrace(e);
                }
            }

            throw new IllegalArgumentException(smafDeviceInfo + " is not supported");
        }

        /** */
        private static Map<SmafDevice.Info, Class<SmafDevice>> deviceMap = new HashMap<>();

        /* */
        static {
            try {
                // props
                Properties props = new Properties();
                final String path = "/vavi/sound/smaf/smaf.properties";
                props.load(Factory.class.getResourceAsStream(path));

                // midi
                for (Object o : props.keySet()) {
                    String key = (String) o;
                    if (key.startsWith("smaf.device.")) {
                        @SuppressWarnings("unchecked")
                        Class<SmafDevice> deviceClass = (Class<SmafDevice>) Class.forName(props.getProperty(key));
//Debug.println("smaf device class: " + StringUtil.getClassName(clazz));
                        SmafDevice.Info smafDeviceInfo = deviceClass.getDeclaredConstructor().newInstance().getDeviceInfo();

                        deviceMap.put(smafDeviceInfo, deviceClass);
                    }
                }
            } catch (Exception e) {
Debug.printStackTrace(e);
                throw new IllegalStateException(e);
            }
        }
    }
}
