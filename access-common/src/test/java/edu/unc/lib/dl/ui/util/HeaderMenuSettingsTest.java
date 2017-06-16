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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.ui.util.HeaderMenuSettings.HeaderMenu;

public class HeaderMenuSettingsTest extends Assert {

    @Test
    public void loadTest() throws Exception {
        Properties properties = new Properties();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("headerMenu.properties");
        properties.load(inputStream);
        
        HeaderMenuSettings menuSettings = new HeaderMenuSettings();
        menuSettings.setProperties(properties);
        menuSettings.init();
        
        assertEquals(4, menuSettings.getMenuRoot().getSubMenus().size());
        
        HeaderMenu contactMenu = menuSettings.getMenuRoot().getSubMenus().get("about");
        assertEquals(3, contactMenu.getSubMenus().size());
        
        HeaderMenu browseMenu = menuSettings.getMenuRoot().getSubMenus().get("browse");
        assertEquals("Browse", browseMenu.getLabel());
        assertEquals("/browse", browseMenu.getUrl());
        assertEquals(2, browseMenu.getSubMenus().size());
        assertEquals("Browse Departments", browseMenu.getSubMenus().get("depts").getLabel());
        assertEquals("Browse Collections", browseMenu.getSubMenus().get("collections").getLabel());
        assertEquals("/collections", browseMenu.getSubMenus().get("collections").getUrl());
        
        //HeaderMenu contactMenu = menuSettings.getMenuRoot().getSubMenus().get("about");
        assertEquals(3, contactMenu.getSubMenus().size());
        
        assertEquals("Browse Departments", browseMenu.getSubMenus().get("depts").getLabel());
    }
    
    @Test
    public void propertyReplacement() throws Exception {
        Properties properties = new Properties();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("headerMenu.properties");
        properties.load(inputStream);
        
        HeaderMenuSettings menuSettings = new HeaderMenuSettings();
        menuSettings.setProperties(properties);
        
        Map<String,String> replacements = new HashMap<String,String>();
        replacements.put("static", "http://localhost/static/");
        menuSettings.setReplacementValues(replacements);
        
        menuSettings.init();
        
        HeaderMenu aboutMenu = menuSettings.getMenuRoot().getSubMenus().get("about");
        assertEquals("http://localhost/static/aboutPages/aboutTheRepository.xml", aboutMenu.getUrl());
    }
    
    @Test
    public void orderTest() throws Exception {
        Properties properties = new Properties();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("headerMenu.properties");
        properties.load(inputStream);
        properties.setProperty("menu.order", "browse,about,contact,home");
        
        HeaderMenuSettings menuSettings = new HeaderMenuSettings();
        menuSettings.setProperties(properties);
        menuSettings.init();
        
        Iterator<String> menuIt = menuSettings.getMenuRoot().getSubMenus().keySet().iterator();
        assertEquals("browse", menuIt.next());
        assertEquals("about", menuIt.next());
        assertEquals("contact", menuIt.next());
        assertEquals("home", menuIt.next());
        
        menuIt = menuSettings.getMenuRoot().getSubMenus().get("browse").getSubMenus().keySet().iterator();
        assertEquals("collections", menuIt.next());
        assertEquals("depts", menuIt.next());
        
        properties.setProperty("menu.browse.order", "depts,collections");
        menuSettings.init();
        
        menuIt = menuSettings.getMenuRoot().getSubMenus().get("browse").getSubMenus().keySet().iterator();
        assertEquals("depts", menuIt.next());
        assertEquals("collections", menuIt.next());
    }
}