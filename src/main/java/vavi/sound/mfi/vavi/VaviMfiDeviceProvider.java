/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import vavi.sound.mfi.MfiDevice;
import vavi.sound.mfi.spi.MfiDeviceProvider;
import vavi.util.Debug;


/**
 * {@link MfiDeviceProvider} implemented by vavi.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020629 nsano initial version <br>
 *          0.10 020703 nsano complete <br>
 *          0.11 030819 nsano add {@link vavi.sound.mfi.MidiConverter} <br>
 */
public class VaviMfiDeviceProvider extends MfiDeviceProvider {

    /** */
    public static final String version = "3.02";

    /** */
    private MfiDevice.Info mfiDeviceInfos[];

    /** */
    public VaviMfiDeviceProvider() {
        this.mfiDeviceInfos = Factory.getMfiDeviceInfos();
    }

    /** */
    public boolean isDeviceSupported(MfiDevice.Info info) {
        for (MfiDevice.Info mfiDeviceInfo : mfiDeviceInfos) {
            if (mfiDeviceInfo.equals(info)) {
                return true;
            }
        }
        return false;
    }

    /** */
    public MfiDevice.Info[] getDeviceInfo() {
        return mfiDeviceInfos;
    }

    /** */
    public MfiDevice getDevice(MfiDevice.Info info)
        throws IllegalArgumentException {

        return Factory.getMfiDevice(info);
    }

    /** */
    private static class Factory {

        /** */
        public static MfiDevice.Info[] getMfiDeviceInfos() {
            List<MfiDevice.Info> tmp = new ArrayList<MfiDevice.Info>();

            for (MfiDevice.Info mfiDeviceInfo : deviceMap.keySet()) {
                tmp.add(mfiDeviceInfo);
            }

            return tmp.toArray(new MfiDevice.Info[tmp.size()]);
        }

        /** */
        public static MfiDevice getMfiDevice(MfiDevice.Info mfiDeviceInfo) {
            if (deviceMap.containsKey(mfiDeviceInfo)) {
                try {
                    return deviceMap.get(mfiDeviceInfo).newInstance();
                } catch (Exception e) {
Debug.printStackTrace(e);
                }
            }

            throw new IllegalArgumentException(mfiDeviceInfo + " is not supported");
        }

        /** */
        private static Map<MfiDevice.Info, Class<MfiDevice>> deviceMap = new HashMap<MfiDevice.Info, Class<MfiDevice>>();

        /* */
        static {
            try {
                // props
                Properties props = new Properties();
                final String path = "vavi.properties";
                props.load(Factory.class.getResourceAsStream(path));
    
                // midi
                Iterator<?> i = props.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String) i.next();
                    if (key.startsWith("mfi.device.")) {
                        @SuppressWarnings("unchecked")
                        Class<MfiDevice> deviceClass = (Class<MfiDevice>) Class.forName(props.getProperty(key));
//Debug.println("mfi device class: " + StringUtil.getClassName(deviceClass));
                        MfiDevice.Info mfiDeviceInfo = deviceClass.newInstance().getDeviceInfo();

                        deviceMap.put(mfiDeviceInfo, deviceClass);
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
