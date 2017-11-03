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
package edu.unc.lib.dl.ui.view;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Before;
import org.junit.Test;

public class MODSFullRecordViewTest {
    private static String XSL_PATH = "/recordTransformations/modsToFullRecord.xsl";

    private XSLComponentView view;
    private Map<String,Object> parameters;
    XPathFactory xPathFactory;

    @Before
    public void init() throws Exception {
        parameters = new HashMap<>();

        view = new XSLComponentView(XSL_PATH);
        view.initializeTransformer();

        xPathFactory = XPathFactory.instance();
    }

    @Test
    public void renderSimpleTest() throws Exception {
        Document doc = getDocument("/mods/simple.xml");

        Document resultDoc = getResultDoc(view.renderView(doc.getRootElement(), parameters));

        assertEquals("unc", getFieldValue("Creator", resultDoc));
        assertEquals("Object Title", getFieldValue("Title", resultDoc));
        assertNull(getFieldValue("Abstract", resultDoc));
    }

    @Test
    public void renderRelatedTest() throws Exception {
        Document doc = getDocument("/mods/relatedItems.xml");

        Document resultEl = getResultDoc(view.renderView(doc.getRootElement(), parameters));

        XPathExpression<Element> xp = xPathFactory.compile("/view/table[2]/tr", Filters.element());
        List<Element> relatedRowEls = xp.evaluate(resultEl);

        Element relatedRowEl = relatedRowEls.get(0);

        String titleAttr = relatedRowEl.getChild("th").getAttributeValue("title");
        assertEquals("Information about a predecessor to this resource", titleAttr);

        Element innerRowEl = relatedRowEl.getChild("td").getChild("table").getChild("tr");
        assertEquals("Title", innerRowEl.getChildText("th"));

        assertEquals("Preceding title here", innerRowEl.getChild("td").getChildTextTrim("p"));
    }

    @Test
    public void renderEmptyNestedRelatedTest() throws Exception {
        Document doc = new Document().addContent(
                new Element("mods", MODS_V3_NS)
                    .addContent(new Element("relatedItem", MODS_V3_NS)
                            .addContent(new Element("relatedItem", MODS_V3_NS))));

        Document resultDoc = getResultDoc(view.renderView(doc.getRootElement(), parameters));

        XPathExpression<Element> xp = xPathFactory.compile("/view/table/tr/td", Filters.element());
        List<Element> relatedEls = xp.evaluate(resultDoc);

        assertNull(relatedEls.get(0).getChild("table"));
    }

    @Test
    public void renderRelatedEmptyChildTest() throws Exception {
        Document doc = new Document().addContent(
                new Element("mods", MODS_V3_NS)
                    .addContent(new Element("relatedItem", MODS_V3_NS)
                            .addContent(new Element("relatedItem", MODS_V3_NS)
                                    .addContent(new Element("titleInfo", MODS_V3_NS)))));

        Document resultDoc = getResultDoc(view.renderView(doc.getRootElement(), parameters));

        XPathExpression<Element> xp = xPathFactory.compile("/view/table/tr/td", Filters.element());
        List<Element> relatedEls = xp.evaluate(resultDoc);

        assertNull(relatedEls.get(0).getChild("table"));
    }

    private String getFieldValue(String fieldName, Document doc) {
        XPathExpression<Element> xp = xPathFactory.compile("/view/table/tr/th[text() = '"
                + fieldName + "']/following-sibling::td[1]", Filters.element());
        List<Element> els = xp.evaluate(doc);
        if (els.size() == 0) {
            return null;
        }

        Element tdEl = els.get(0);
        if (tdEl.getChildren().size() > 0) {
            return tdEl.getChildren().get(0).getTextTrim();
        } else {
            return tdEl.getTextTrim();
        }
    }

    private Document getDocument(String path) throws Exception {
        InputStream docStream = this.getClass().getResourceAsStream(path);
        SAXBuilder builder = new SAXBuilder();
        return builder.build(docStream);
    }

    private Document getResultDoc(String result) throws Exception {
        String wrapped = "<view>" + result + "</view>";
        SAXBuilder builder = new SAXBuilder();
        return builder.build(new ByteArrayInputStream(wrapped.getBytes()));
    }
}
