package edu.unc.lib.boxc.deposit.normalize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLOutputFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * @author bbpennel
 */
public class WorkFormModsTransformerTest {
    private WorkFormModsTransformer transformer;
    private ObjectMapper mapper;

    @BeforeEach
    public void setup() throws Exception {
        transformer = new WorkFormModsTransformer();
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }

    @Test
    public void testTransformGenericMinimal() throws Exception {
        var formData = deserializeFormJson("src/test/resources/form_submissions/generic_minimal.json");
        var modsDoc = transformer.transform(formData);
        var modsEl = modsDoc.getRootElement();
        var title = modsEl.getChild("titleInfo", MODS_V3_NS).getChild("title", MODS_V3_NS).getValue();
        assertEquals("generic test", title);
        var langTermEl = modsEl.getChild("language", MODS_V3_NS).getChild("languageTerm", MODS_V3_NS);
        assertEquals("iso639-2b", langTermEl.getAttribute("authority").getValue());
        assertEquals("eng", langTermEl.getText());

        // Notes
        var noteEls = modsEl.getChildren("note", MODS_V3_NS);
        assertEquals(1, noteEls.size());
        var description = noteEls.get(0);
        assertEquals("Description", description.getAttributeValue("displayLabel"));
        assertEquals("hello world", description.getText());

        assertEquals(4, modsEl.getChildren().size());
    }

    @Test
    public void testTransformContinuingMinimal() throws Exception {
        var formData = deserializeFormJson("src/test/resources/form_submissions/continuing_minimal.json");
        var modsDoc = transformer.transform(formData);
        var modsEl = modsDoc.getRootElement();
        var title = modsEl.getChild("titleInfo", MODS_V3_NS).getChild("title", MODS_V3_NS).getValue();
        assertEquals("test publication", title);
        var langTermEl = modsEl.getChild("language", MODS_V3_NS).getChild("languageTerm", MODS_V3_NS);
        assertEquals("iso639-2b", langTermEl.getAttribute("authority").getValue());
        assertEquals("eng", langTermEl.getText());
    }

    @Test
    public void testTransformGenericMaximal() throws Exception {
        var formData = deserializeFormJson("src/test/resources/form_submissions/generic_maximal.json");
        var modsDoc = transformer.transform(formData);
        var modsEl = modsDoc.getRootElement();

        // Test title
        var title = modsEl.getChild("titleInfo", MODS_V3_NS).getChild("title", MODS_V3_NS).getValue();
        assertEquals("Generic Maximal", title);

        // Test language
        var langTermEl = modsEl.getChild("language", MODS_V3_NS).getChild("languageTerm", MODS_V3_NS);
        assertEquals("iso639-2b", langTermEl.getAttribute("authority").getValue());
        assertEquals("eng", langTermEl.getText());

        // Test resource type
        var resourceTypeEl = modsEl.getChild("typeOfResource", MODS_V3_NS);
        assertEquals("text", resourceTypeEl.getText());

        // Test date created
        var originInfoEl = modsEl.getChild("originInfo", MODS_V3_NS);
        var dateCreatedEl = originInfoEl.getChild("dateCreated", MODS_V3_NS);
        assertEquals("2020-05-01", dateCreatedEl.getValue());
        assertEquals("w3cdtf", dateCreatedEl.getAttributeValue("encoding"));

        // Test creators (personal)
        var nameEls = modsEl.getChildren("name", MODS_V3_NS);

        // First creator
        var creator1 = nameEls.get(0);
        assertEquals("personal", creator1.getAttributeValue("type"));
        var nameParts1 = creator1.getChildren("namePart", MODS_V3_NS);
        assertEquals("given", nameParts1.get(0).getAttributeValue("type"));
        assertEquals("Ben", nameParts1.get(0).getText());
        assertEquals("family", nameParts1.get(1).getAttributeValue("type"));
        assertEquals("Pen", nameParts1.get(1).getText());
        assertEquals("date", nameParts1.get(2).getAttributeValue("type"));
        assertEquals("1900-2000", nameParts1.get(2).getText());
        assertEquals("termsOfAddress", nameParts1.get(3).getAttributeValue("type"));
        assertEquals("Jr", nameParts1.get(3).getText());

        // Second creator
        var creator2 = nameEls.get(1);
        var nameParts2 = creator2.getChildren("namePart", MODS_V3_NS);
        assertEquals("given", nameParts2.get(0).getAttributeValue("type"));
        assertEquals("Glen", nameParts2.get(0).getText());
        assertEquals("family", nameParts2.get(1).getAttributeValue("type"));
        assertEquals("Hen", nameParts2.get(1).getText());

        var corpCreator = nameEls.get(2);
        assertEquals("Corporate Overlord", corpCreator.getChild("namePart", MODS_V3_NS).getText());

        assertEquals(3, nameEls.size());

        // Test subjects
        var subjectEls = modsEl.getChildren("subject", MODS_V3_NS);
        assertEquals(5, subjectEls.size()); // 1 topical + 1 personal + 1 corporate + 2 geographic

        // Topical subject
        var topicalSubject = subjectEls.get(0);
        assertEquals("Testing subject", topicalSubject.getChild("topic", MODS_V3_NS).getText());

        // Personal name subject
        var personalSubject = subjectEls.get(1);
        assertEquals("Testing Name", personalSubject.getChild("name", MODS_V3_NS)
                .getChild("namePart", MODS_V3_NS).getText());

        // Corporate name subject
        var corpSubject = subjectEls.get(2);
        assertEquals("Testing Corporate", corpSubject.getChild("name", MODS_V3_NS)
                .getChild("namePart", MODS_V3_NS).getText());

        // Geographic subjects
        assertEquals("Testing geographic", subjectEls.get(3).getChild("geographic", MODS_V3_NS).getText());
        assertEquals("North Carolina", subjectEls.get(4).getChild("geographic", MODS_V3_NS).getText());

        // Notes
        var noteEls = modsEl.getChildren("note", MODS_V3_NS);
        assertEquals(2, noteEls.size());
        var description = noteEls.get(0);
        assertEquals("Description", description.getAttributeValue("displayLabel"));
        assertEquals("Test abstract", description.getText());

        var keywordEl = noteEls.get(1);
        assertEquals("Keywords", keywordEl.getAttributeValue("displayLabel"));
        assertEquals("Testing; Working", keywordEl.getText());

        assertEquals(14, modsEl.getChildren().size());
    }

    @Test
    public void testTransformContinuingMaximal() throws Exception {
        var formData = deserializeFormJson("src/test/resources/form_submissions/continuing_maximal.json");
        var modsDoc = transformer.transform(formData);
        var modsEl = modsDoc.getRootElement();

        // Test title
        var titles = modsEl.getChildren("titleInfo", MODS_V3_NS);
        var title = titles.get(0).getChild("title", MODS_V3_NS);
        assertEquals("Maximal Publication", title.getText());
        var title2 = titles.get(1).getChild("title", MODS_V3_NS);
        assertEquals("Lots of fields", title2.getText());
        assertEquals("alternative", titles.get(1).getAttributeValue("type"));

        // Related items
        var relatedItems = modsEl.getChildren("relatedItem", MODS_V3_NS);
        var related1 = relatedItems.get(0);
        assertEquals("preceding", related1.getAttributeValue("type"));
        var related1Text = related1.getChild("titleInfo", MODS_V3_NS).getChildText("title", MODS_V3_NS);
        assertEquals("Preceding pub", related1Text);
        var related2 = relatedItems.get(1);
        assertEquals("succeeding", related2.getAttributeValue("type"));
        var related2Text = related2.getChild("titleInfo", MODS_V3_NS).getChildText("title", MODS_V3_NS);
        assertEquals("Succeeding pub", related2Text);
        var related3 = relatedItems.get(2);
        assertEquals("Related resource", related3.getAttributeValue("displayLabel"));
        var related3Text = related3.getChild("location", MODS_V3_NS).getChildText("url", MODS_V3_NS);
        assertEquals("https://example.com", related3Text);

        assertEquals(3, relatedItems.size());

        // Test language
        var langTermEl = modsEl.getChild("language", MODS_V3_NS).getChild("languageTerm", MODS_V3_NS);
        assertEquals("iso639-2b", langTermEl.getAttribute("authority").getValue());
        assertEquals("eng", langTermEl.getText());

        // Test resource type
        var resourceTypeEl = modsEl.getChild("typeOfResource", MODS_V3_NS);
        assertEquals("text", resourceTypeEl.getText());

        // Test part fields
        var partEl = modsEl.getChild("part", MODS_V3_NS);
        var detailEls = partEl.getChildren("detail", MODS_V3_NS);
        assertEquals("volume", detailEls.get(0).getAttributeValue("type"));
        assertEquals("5", detailEls.get(0).getChildText("number", MODS_V3_NS));
        assertEquals("number", detailEls.get(1).getAttributeValue("type"));
        assertEquals("6", detailEls.get(1).getChildText("number", MODS_V3_NS));
        assertEquals(2, detailEls.size());
        var dateOfIssueEl = partEl.getChild("date", MODS_V3_NS);
        assertEquals("2020", dateOfIssueEl.getValue());
        assertEquals("iso8601", dateOfIssueEl.getAttributeValue("encoding"));

        // Test origin info
        var originInfoEl = modsEl.getChild("originInfo", MODS_V3_NS);
        var placeOfPubEl = originInfoEl.getChild("place", MODS_V3_NS);
        var placeEl = placeOfPubEl.getChild("placeTerm", MODS_V3_NS);
        assertEquals("text", placeEl.getAttributeValue("type"));
        assertEquals("Chapel hill", placeEl.getText());
        assertEquals("Boxc", originInfoEl.getChildText("publisher", MODS_V3_NS));
        assertEquals("multipart monograph", originInfoEl.getChildText("issuance", MODS_V3_NS));
        var frequencyEl = originInfoEl.getChild("frequency", MODS_V3_NS);
        assertEquals("Continuously updated", frequencyEl.getText());
        assertEquals("marcfrequency", frequencyEl.getAttributeValue("authority"));

        // Test genre
        var genreEl = modsEl.getChild("genre", MODS_V3_NS);
        assertEquals("lcgft", genreEl.getAttributeValue("authority"));
        assertEquals("Periodicals", genreEl.getText());

        // Test subjects
        var subjectEls = modsEl.getChildren("subject", MODS_V3_NS);
        assertEquals(4, subjectEls.size());

        // Topical subject
        var topicalSubject = subjectEls.get(0);
        assertEquals("Topic subj", topicalSubject.getChild("topic", MODS_V3_NS).getText());

        // Personal name subject
        var personalSubject = subjectEls.get(1);
        assertEquals("Personal subj", personalSubject.getChild("name", MODS_V3_NS)
                .getChild("namePart", MODS_V3_NS).getText());

        // Corporate name subject
        var corpSubject = subjectEls.get(2);
        assertEquals("Corporate subj", corpSubject.getChild("name", MODS_V3_NS)
                .getChild("namePart", MODS_V3_NS).getText());

        // Geographic subjects
        assertEquals("Geo subj", subjectEls.get(3).getChild("geographic", MODS_V3_NS).getText());

        assertEquals(14, modsEl.getChildren().size());
    }

    @Test
    public void testTransformGenericWithXmlCharacters() throws Exception {
        var formData = deserializeFormJson("src/test/resources/form_submissions/generic_with_xml_chars.json");
        var modsDoc = transformer.transform(formData);
        var modsEl = modsDoc.getRootElement();

        // Test title
        var title = modsEl.getChild("titleInfo", MODS_V3_NS).getChild("title", MODS_V3_NS).getValue();
        assertEquals("generic test & characters", title);

        // Test language
        var langTermEl = modsEl.getChild("language", MODS_V3_NS).getChild("languageTerm", MODS_V3_NS);
        assertEquals("iso639-2b", langTermEl.getAttribute("authority").getValue());
        assertEquals("eng", langTermEl.getText());

        // Test resource type
        var resourceTypeEl = modsEl.getChild("typeOfResource", MODS_V3_NS);
        assertEquals("text", resourceTypeEl.getText());

        // Test creators (personal)
        var nameEls = modsEl.getChildren("name", MODS_V3_NS);

        // First creator
        var creator1 = nameEls.get(0);
        assertEquals("personal", creator1.getAttributeValue("type"));
        var nameParts1 = creator1.getChildren("namePart", MODS_V3_NS);
        assertEquals("given", nameParts1.get(0).getAttributeValue("type"));
        assertEquals("Bo'xc", nameParts1.get(0).getText());
        assertEquals("family", nameParts1.get(1).getAttributeValue("type"));
        assertEquals("Tim>e", nameParts1.get(1).getText());

        assertEquals(1, nameEls.size());

        // Test subjects
        var subjectEls = modsEl.getChildren("subject", MODS_V3_NS);
        assertEquals(1, subjectEls.size()); // 1 topical + 1 personal + 1 corporate + 2 geographic

        // Corporate name subject
        var corpSubject = subjectEls.get(0);
        assertEquals("Test & characters' Inc.", corpSubject.getChild("name", MODS_V3_NS)
                .getChild("namePart", MODS_V3_NS).getText());

        // Keywords note
        var noteEls = modsEl.getChildren("note", MODS_V3_NS);
        assertEquals(2, noteEls.size());
        var description = noteEls.get(0);
        assertEquals("Description", description.getAttributeValue("displayLabel"));
        assertEquals("hello> <world \"this is a test\" with xml chars'", description.getText());

        var keywordEl = noteEls.get(1);
        assertEquals("Keywords", keywordEl.getAttributeValue("displayLabel"));
        assertEquals("<xml>; te$t", keywordEl.getText());

        assertEquals(7, modsEl.getChildren().size());
    }

    private WorkFormData deserializeFormJson(String jsonLocation) throws Exception {
        var jsonPath = Paths.get(jsonLocation);
        try (var formStream = Files.newInputStream(jsonPath)) {
            return mapper.readValue(formStream, WorkFormData.class);
        }
    }
}
