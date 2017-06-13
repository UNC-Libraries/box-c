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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages configuration of the header menu, interpreting the heirarchy of the menu options into a tree of sub-menus.
 *
 * @author bbpennel
 *
 */
public class HeaderMenuSettings {
    private static final Logger LOG = LoggerFactory.getLogger(HeaderMenuSettings.class);

    public static Pattern entryMatcher = Pattern.compile(
            "^menu\\.(\\w+)(\\.(label|url|entry|order|referer|target))?(\\.(\\w+).(label|url|referer))?$");

    // The root node of the menu tree
    private HeaderMenu menuRoot;
    // Map containing placeholder keys to seek in the supplied properties, which will be replaced with the value
    private Map<String,String> replacementValues;
    private Properties properties;
    private String propertiesUrl;

    public HeaderMenuSettings() {
        this.menuRoot = new HeaderMenu("root");
    }

    public void init() {
        this.menuRoot.getSubMenus().clear();
        this.processProperties();
        this.orderMenu(this.menuRoot);
    }

    /**
     * Processes the given properties file into the menu tree
     */
    private void processProperties() {
        Iterator<Entry<Object, Object>> propertiesIt = properties.entrySet().iterator();
        while (propertiesIt.hasNext()) {
            Entry<Object, Object> property = propertiesIt.next();
            String key = (String) property.getKey();

            Matcher keyMatcher = entryMatcher.matcher(key);
            if (keyMatcher.find()) {
                String menuName = keyMatcher.group(1);
                // Catch the ordering of the root element's children
                if ("order".equals(menuName)) {
                    this.menuRoot.setOrder((String)property.getValue());
                    continue;
                }

                String entryValue = this.replaceValues((String)property.getValue());

                HeaderMenu headerMenu = this.menuRoot.subMenus.get(menuName);
                if (headerMenu == null) {
                    headerMenu = new HeaderMenu(menuName);
                    this.menuRoot.subMenus.put(menuName, headerMenu);
                }

                HeaderMenu targetedMenu;
                String menuType = keyMatcher.group(3);
                if ("entry".equals(menuType)) {
                    String subMenuName = keyMatcher.group(5);
                    targetedMenu = headerMenu.getSubMenus().get(subMenuName);

                    if (targetedMenu == null) {
                        targetedMenu = new HeaderMenu(subMenuName);
                        headerMenu.subMenus.put(subMenuName, targetedMenu);
                    }
                    menuType = keyMatcher.group(6);
                } else {
                    // Updating the parent entry.
                    targetedMenu = headerMenu;
                }

                if ("label".equals(menuType)) {
                    targetedMenu.setLabel(entryValue);
                } else if ("url".equals(menuType)) {
                    targetedMenu.setUrl(entryValue);
                } else if ("order".equals(menuType)) {
                    targetedMenu.setOrder(entryValue);
                } else if ("referer".equals(menuType)) {
                    targetedMenu.setIncludeReferer(entryValue);
                } else if ("target".equals(menuType)) {
                    targetedMenu.setTarget(entryValue);
                }
            }
        }
    }

    private void orderMenu(HeaderMenu menuEntry) {
        if (menuEntry.getOrder() == null) {
            return;
        }

        Map<String, HeaderMenu> reorderedEntries = new LinkedHashMap<String, HeaderMenu>();
        String[] orderKeys = menuEntry.getOrder().split(",");

        for (String orderKey: orderKeys) {
            HeaderMenu childEntry = menuEntry.getSubMenus().get(orderKey);
            reorderedEntries.put(orderKey, childEntry);
            this.orderMenu(childEntry);
        }

        menuEntry.setSubMenus(reorderedEntries);
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setReplacementValues(Map<String, String> replacementValues) {
        this.replacementValues = replacementValues;
    }

    /**
     * Replaces all strings matching {key} with the associated value for that key.
     * @param value
     * @return
     */
    private String replaceValues(String value) {
        if (this.replacementValues == null) {
            return value;
        }

        for (Map.Entry<String, String> replacementEntry: this.replacementValues.entrySet()) {
            value = value.replace("{" + replacementEntry.getKey() + "}", replacementEntry.getValue());
        }
        return value;
    }

    public HeaderMenu getMenuRoot() {
        return menuRoot;
    }

    @Override
    public String toString() {
        return "HeaderMenuSettings [menuRoot=" + menuRoot + ", replacementValues=" + replacementValues + ", properties="
                + properties + ", propertiesUrl=" + propertiesUrl + "]";
    }

    public static class HeaderMenu {
        private Map<String, HeaderMenu> subMenus;
        private String key;
        private String label;
        private String url;
        private String order;
        private String target;
        private boolean includeReferer;

        public HeaderMenu(String key) {
            this.key = key;
            this.subMenus = new LinkedHashMap<String, HeaderMenu>();
            this.includeReferer = false;
        }

        public Map<String, HeaderMenu> getSubMenus() {
            return subMenus;
        }

        public void setSubMenus(Map<String, HeaderMenu> subMenus) {
            this.subMenus = subMenus;
        }

        public String getOrder() {
            return order;
        }

        public void setOrder(String order) {
            this.order = order;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getKey() {
            return this.key;
        }

        public boolean isIncludeReferer() {
            return includeReferer;
        }

        public void setIncludeReferer(boolean includeReferer) {
            this.includeReferer = includeReferer;
        }

        public void setIncludeReferer(String referer) {
            this.includeReferer = Boolean.parseBoolean(referer);
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        @Override
        public String toString() {
            return "HeaderMenu [subMenus=" + subMenus + ", key=" + key + ", label=" + label + ", url=" + url +
                    ", order=" + order + "]";
        }
    }
}
