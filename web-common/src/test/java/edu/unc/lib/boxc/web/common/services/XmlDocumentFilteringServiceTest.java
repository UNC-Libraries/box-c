package edu.unc.lib.boxc.web.common.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.common.xml.SecureXMLFactory;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author bbpennel
 */
public class XmlDocumentFilteringServiceTest {
    private XmlDocumentFilteringService service;
    private Document doc;
    private Document exclusionDoc;

    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void setup() throws Exception {
        service = new XmlDocumentFilteringService();
        SAXBuilder saxBuilder = SecureXMLFactory.createSAXBuilder();
        doc = saxBuilder.build(new File("src/test/resources/mods/test_record.xml"));
        exclusionDoc = saxBuilder.build(new File("src/test/resources/mods/exclusion_record.xml"));
        tmpFolder.resolve("testFolder");
        Files.createDirectory(tmpFolder.resolve("testFolder"));
    }

    private void initWithXPath(String... xpaths) throws Exception {
        ObjectWriter writer = new ObjectMapper().writer();
        File configFile = tmpFolder.resolve("config.json").toFile();
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
    public void excludeAllElementsTest() throws Exception {
        initWithXPath("//mods:originInfo[@displayLabel=\"Digital Scan Date Raw Scan\"]/mods:dateCaptured",
                "//mods:originInfo[@displayLabel=\"Digital Scan Date filename\"]/mods:dateCaptured",
                "//mods:identifier[@type=\"filename\"]",
                "//mods:physicalDescription/mods:note[@type=\"technical\"]",
                "//mods:originInfo/mods:place/mods:placeTerm[@type=\"code\" and @authority=\"marccountry\"]",
                "//mods:originInfo/mods:dateIssued[@encoding=\"marc\"]",
                "//mods:identifier[@displayLabel=\"HookID\" and @type=\"local\"]",
                "//mods:physicalDescription/mods:note[@displayLabel=\"Container type\"]",
                "//mods:identifier[@displayLabel=\"CONTENTdm number\" and @type=\"local\"]",
                "//mods:accessCondition[@type=\"use and reproduction\" and @displayLabel=\"CONTENTdm Usage Rights\"]");

        service.filterExclusions(exclusionDoc);
        assertEquals(0, exclusionDoc.getRootElement().getChildren().size());
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
