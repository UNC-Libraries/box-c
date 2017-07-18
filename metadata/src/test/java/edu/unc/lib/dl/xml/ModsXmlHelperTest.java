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
package edu.unc.lib.dl.xml;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ModsXmlHelperTest extends Assert {
    private static final Log log = LogFactory.getLog(ModsXmlHelper.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetFormattedLabelText() {
    Document mods = null;
    SAXBuilder builder = new SAXBuilder();
    try {
        mods = builder.build("src/test/resources/samples/mods.xml");
    } catch (JDOMException e) {
        throw new Error(e);
    } catch (IOException e) {
        throw new Error(e);
    }
    String result = ModsXmlHelper.getFormattedLabelText(mods.getRootElement());
    assertTrue("Must match expected output", "The SCHOLARLY COMMUNICATIONS CONVOCATION: Final Report"
            .equals(result));
    log.info(result);
    }

    @Test
    public void testTransform() {
    Document mods = null;
    Document result = null;
    SAXBuilder builder = new SAXBuilder();
    try {
        mods = builder.build("src/test/resources/samples/mods.xml");
        result = ModsXmlHelper.transform(mods.getRootElement());
    } catch (JDOMException e) {
        throw new Error(e);
    } catch (IOException e) {
        throw new Error(e);
    } catch (TransformerException e) {
        throw new Error(e);
    }
    if (log.isInfoEnabled()) {
        XMLOutputter out = new XMLOutputter();
        String str = out.outputString(result);
        log.info(str);
    }
    assertTrue("Must have proper number of DC elements", 5 == result.getRootElement().getChildren().size());
    }

}
