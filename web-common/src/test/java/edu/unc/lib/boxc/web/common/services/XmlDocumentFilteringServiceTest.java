package edu.unc.lib.boxc.web.common.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.common.xml.SecureXMLFactory;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author bbpennel
 */
public class XmlDocumentFilteringServiceTest {
    private XmlDocumentFilteringService service;
    private Document doc;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        service = new XmlDocumentFilteringService();
        SAXBuilder saxBuilder = SecureXMLFactory.createSAXBuilder();
        doc = saxBuilder.build(new File("src/test/resources/mods/test_record.xml"));
        tmpFolder.create();
    }

    private void initWithXPath(String... xpaths) throws Exception {
        ObjectWriter writer = new ObjectMapper().writer();
        File configFile = tmpFolder.newFile("config.json");
        writer.writeValue(configFile, Arrays.asList(xpaths));
        service.setConfigPath(configFile.toPath());
        service.init();
    }

    @Test
    public void excludeNoMatchesTest() throws Exception {
        initWithXPath();
        String before = new XMLOutputter().outputString(doc);
        service.filterExclusions(doc);
        String after = new XMLOutputter().outputString(doc);
        assertEquals(before, after);
    }

    @Test
    public void excludeSingleElementTest() throws Exception {
        initWithXPath("mods:mods/mods:titleInfo/mods:title");
        service.filterExclusions(doc);
        assertNull(doc.getRootElement().getChild("titleInfo", MODS_V3_NS));
        assertNotNull(doc.getRootElement().getChild("language", MODS_V3_NS));
        assertNotNull(doc.getRootElement().getChild("abstract", MODS_V3_NS));
        assertEquals(5, doc.getRootElement().getChildren().size());
    }

    @Test
    public void excludeMultipleElementsTest() throws Exception {
        initWithXPath("mods:mods/mods:titleInfo/mods:title",
                "mods:mods/mods:language");
        service.filterExclusions(doc);
        assertNull(doc.getRootElement().getChild("titleInfo", MODS_V3_NS));
        assertNull(doc.getRootElement().getChild("language", MODS_V3_NS));
        assertNotNull(doc.getRootElement().getChild("abstract", MODS_V3_NS));
        assertEquals(4, doc.getRootElement().getChildren().size());
    }

    @Test
    public void excludeMultipleSameFieldTest() throws Exception {
        initWithXPath("mods:mods/mods:abstract");
        service.filterExclusions(doc);
        assertNotNull(doc.getRootElement().getChild("titleInfo", MODS_V3_NS));
        assertNotNull(doc.getRootElement().getChild("language", MODS_V3_NS));
        assertNull(doc.getRootElement().getChild("abstract", MODS_V3_NS));
        assertEquals(4, doc.getRootElement().getChildren().size());
    }

    @Test
    public void excludeDefaultNamespacesTest() throws Exception {
        initWithXPath("mods:mods/mods:extension/*[local-name()='boxc-worktype']");
        service.filterExclusions(doc);
        assertNotNull(doc.getRootElement().getChild("titleInfo", MODS_V3_NS));
        assertNotNull(doc.getRootElement().getChild("language", MODS_V3_NS));
        assertNotNull(doc.getRootElement().getChild("abstract", MODS_V3_NS));
        assertNull(doc.getRootElement().getChild("extension", MODS_V3_NS));
        assertEquals(5, doc.getRootElement().getChildren().size());
    }

    @Test
    public void excludeParentByChildTest() throws Exception {
        initWithXPath("mods:mods/mods:name[mods:affiliation[text() = 'place']]");
        service.filterExclusions(doc);
        assertNotNull(doc.getRootElement().getChild("titleInfo", MODS_V3_NS));
        assertNotNull(doc.getRootElement().getChild("language", MODS_V3_NS));
        assertNotNull(doc.getRootElement().getChild("abstract", MODS_V3_NS));
        assertNull(doc.getRootElement().getChild("name", MODS_V3_NS));
        assertEquals(5, doc.getRootElement().getChildren().size());
    }
}
