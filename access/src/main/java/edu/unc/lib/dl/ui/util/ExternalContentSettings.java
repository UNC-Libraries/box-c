/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author count0
 *
 */
public class ExternalContentSettings {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalContentSettings.class);
    public static Properties properties;

    public ExternalContentSettings() {
    }

    public static String getUrl(String key) {
        if (key == null) {
            return null;
        }
        return properties.getProperty("external." + key + ".url");
    }

    public List<String> list(String key) {
        return getList(key);
    }

    public static List<String> getList(String key) {
        if (key == null) {
            return null;
        }
        List<String> matches = new ArrayList<String>();
        Iterator<Entry<Object,Object>> propertiesIt = properties.entrySet().iterator();
        while (propertiesIt.hasNext()) {
            Entry<Object,Object> property = propertiesIt.next();
            if (((String)property.getKey()).indexOf("external." + key) == 0) {
                matches.add((String)property.getValue());
            }
        }
        return matches;
    }

    public static Map<String,String> getMap(String key) {
        if (key == null) {
            return null;
        }
        Map<String,String> matches = new HashMap<String,String>();
        Iterator<Entry<Object,Object>> propertiesIt = properties.entrySet().iterator();
        while (propertiesIt.hasNext()) {
            Entry<Object,Object> property = propertiesIt.next();
            if (((String)property.getKey()).indexOf("external." + key) == 0) {
                matches.put((String)property.getKey(), (String)property.getValue());
            }
        }
        return matches;
    }

    public Map<String,String> map(String key) {
        return getMap(key);
    }

    public static String get(String key) {
        if (key == null) {
            return null;
        }
        return properties.getProperty(key);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        LOG.debug("Settings properties for ExternalContentSettings");
        ExternalContentSettings.properties = properties;
        for (Entry<Object,Object> property: properties.entrySet()) {
            if (((String)property.getKey()).contains(".url")
                    && !((String)property.getValue()).contains("http://")
                    && !((String)property.getValue()).contains("https://")
                    && !((String)property.getValue()).contains("redirect:")) {
                property.setValue(properties.getProperty("external.base.url") + property.getValue());
            }
            LOG.debug(property.getKey() + ": " + property.getValue());
        }
    }
}
