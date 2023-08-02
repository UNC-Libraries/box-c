package edu.unc.lib.boxc.web.access.controllers;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.web.common.view.XSLComponentView;

public class MODSFullRecordViewTest {
    private static String XSL_PATH = "/recordTransformations/modsToFullRecord.xsl";

    private XSLComponentView view;
    private Map<String,Object> parameters;
    XPathFactory xPathFactory;

    @BeforeEach
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

        assertEquals("unc", getFieldValue("Contributor", resultDoc));
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

        assertEquals("Preceding title here", innerRowEl.getChildTextTrim("td"));
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
        return els.get(0).getTextTrim();
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
