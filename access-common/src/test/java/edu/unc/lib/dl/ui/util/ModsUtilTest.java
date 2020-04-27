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

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.dl.xml.SecureXMLFactory.createSAXBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;

/**
 * @author lfarrell
 */
public class ModsUtilTest {
    @Test
    public void emptyMods() throws JDOMException, IOException {
        String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\"></mods>";
        Document modsDoc = buildMods(modsString);
        assertEquals(0, modsDoc.getRootElement().getChildren().size());
    }

    @Test
    public void emptyTopLevel() throws JDOMException, IOException {
        String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
                "  <titleInfo>" +
                "  </titleInfo>" +
                "</mods>";

        Document modsDoc = buildMods(modsString);
        assertEquals(0, modsDoc.getRootElement().getChildren().size());
    }

    @Test
    public void emptyNested() throws JDOMException, IOException {
    String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
            "  <titleInfo>" +
            "    <title></title>" +
            "  </titleInfo>" +
            "</mods>";

        Document modsDoc = buildMods(modsString);
        assertEquals(0, modsDoc.getRootElement().getChildren().size());
    }

    @Test
    public void emptyNestedWithAttributes() throws JDOMException, IOException {
        String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
                "  <titleInfo displayLabel=\"test item\">" +
                "    <title></title>" +
                "  </titleInfo>" +
                "</mods>";

        Document modsDoc = buildMods(modsString);
        assertEquals(0, modsDoc.getRootElement().getChildren().size());
    }

    @Test
    public void sameLevelAllEmpty() throws JDOMException, IOException {
        String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
                "  <titleInfo>" +
                "    <title></title>" +
                "  </titleInfo>" +
                "  <genre authority=\"gmgpc\"></genre>" +
                "</mods>";

        Document modsDoc = buildMods(modsString);
        assertEquals(0, modsDoc.getRootElement().getChildren().size());
    }

    @Test
    public void deeplyNestedWithText() throws JDOMException, IOException {
        String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
                "  <titleInfo>" +
                "     <title></title>" +
                "   </titleInfo>" +
                "  <relatedItem>" +
                "    <relatedItem>" +
                "      <relatedItem>" +
                "        <relatedItem>" +
                "          <abstract />" +
                "        </relatedItem>" +
                "        <abstract>Test Pic</abstract>" +
                "      </relatedItem>" +
                "    </relatedItem>" +
                "  </relatedItem>" +
                "</mods>";

        Document modsDoc = buildMods(modsString);
        Element docRoot = modsDoc.getRootElement();
        assertEquals(1, docRoot.getChildren().size());

        Element relatedItems = docRoot.getChild("relatedItem", MODS_V3_NS)
                .getChild("relatedItem", MODS_V3_NS)
                .getChild("relatedItem", MODS_V3_NS);

        assertEquals(1, relatedItems.getChildren().size());
        assertEquals("Test Pic",
                relatedItems.getChildText("abstract", MODS_V3_NS));
    }

    @Test
    public void deeplyNestedWithOutText() throws JDOMException, IOException {
        String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
                "  <titleInfo>" +
                "     <title></title>" +
                "   </titleInfo>" +
                "  <relatedItem>" +
                "    <relatedItem>" +
                "      <relatedItem>" +
                "        <relatedItem>" +
                "          <abstract />" +
                "        </relatedItem>" +
                "        <abstract></abstract>" +
                "      </relatedItem>" +
                "    </relatedItem>" +
                "  </relatedItem>" +
                "</mods>";

        Document modsDoc = buildMods(modsString);
        Element docRoot = modsDoc.getRootElement();
        assertEquals(0, docRoot.getChildren().size());
    }

    @Test
    public void deeplyNestedWithTextVariation() throws JDOMException, IOException {
        String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
                "  <relatedItem>" +
                "    <relatedItem>" +
                "      <relatedItem>" +
                "        <relatedItem>" +
                "          <abstract />" +
                "        </relatedItem>" +
                "        <abstract>Test Pic</abstract>" +
                "      </relatedItem>" +
                "    </relatedItem>" +
                "  </relatedItem>" +
                "</mods>";

        Document modsDoc = buildMods(modsString);
        Element docRoot = modsDoc.getRootElement();
        assertEquals(1, docRoot.getChildren().size());

        Element relatedItems = docRoot.getChild("relatedItem", MODS_V3_NS)
                .getChild("relatedItem", MODS_V3_NS)
                .getChild("relatedItem", MODS_V3_NS);

        assertEquals(1, relatedItems.getChildren().size());
        assertEquals("Test Pic",
                relatedItems.getChildText("abstract", MODS_V3_NS));
    }

    @Test
    public void nestedPartiallyPopulated() throws JDOMException, IOException {
        String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
                "  <titleInfo>" +
                "    <title>Test Image</title>" +
                "  </titleInfo>" +
                "  <typeOfResource>still image</typeOfResource>" +
                "  <genre authority=\"gmgpc\">digital images</genre>" +
                "  <originInfo>" +
                "    <place>" +
                "      <placeTerm type=\"text\" />" +
                "    </place>" +
                "  </originInfo>" +
                "</mods>";

        Document modsDoc = buildMods(modsString);
        Element docRoot = modsDoc.getRootElement();

        assertEquals(3, docRoot.getChildren().size());
        assertEquals("Test Image",
                docRoot.getChild("titleInfo", MODS_V3_NS).getChildText("title", MODS_V3_NS));
        assertEquals("still image",
                docRoot.getChild("typeOfResource", MODS_V3_NS).getTextTrim());
        assertEquals("digital images",
                docRoot.getChild("genre", MODS_V3_NS).getTextTrim());
        assertNull(docRoot.getChild("originInfo", MODS_V3_NS));
    }

    @Test
    public void whiteSpaceOnly() throws JDOMException, IOException {
        String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
                "  <titleInfo>" +
                "    <title>  \n  \n</title>" +
                "  </titleInfo>" +
                "</mods>";

        Document modsDoc = buildMods(modsString);
        assertEquals(0, modsDoc.getRootElement().getChildren().size());
    }

    @Test
    public void emptyDeepNested() throws JDOMException, IOException {
        String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
                "  <originInfo>" +
                "    <place>" +
                "      <placeTerm type=\"text\" />" +
                "    </place>" +
                "  </originInfo>" +
                "</mods>";

        Document modsDoc = buildMods(modsString);
        assertEquals(0, modsDoc.getRootElement().getChildren().size());
    }

    @Test
    public void multipleChildren() throws JDOMException, IOException {
        String modsString = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
                "  <originInfo>" +
                "    <place>" +
                "      <placeTerm type=\"text\" />" +
                "    </place>" +
                "    <publisher>Test publisher</publisher>" +
                "  </originInfo>" +
                "</mods>";

        Document modsDoc = buildMods(modsString);
        Element docRoot = modsDoc.getRootElement();
        assertEquals(1, docRoot.getChildren().size());
        assertEquals(1, docRoot.getChild("originInfo", MODS_V3_NS).getChildren().size());
    }

    private Document buildMods(String modsString) throws JDOMException, IOException {
        InputStream modsStream = new ByteArrayInputStream(modsString.getBytes());
        SAXBuilder builder = createSAXBuilder();
        return ModsUtil.removeEmptyNodes(builder.build(modsStream));
    }
}