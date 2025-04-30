package edu.unc.lib.boxc.deposit.normalize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

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
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Test
    public void testTransformGenericMinimal() throws Exception {
        var formData = deserializeFormJson("src/test/resources/form_submissions/generic_minimal.json");
        var modsDoc = transformer.transform(formData);
        var modsEl = modsDoc.getRootElement();
        var title = modsEl.getChild("titleInfo", MODS_V3_NS).getChild("title", MODS_V3_NS).getValue();
        assertEquals("test work", title);
        var langTermEl = modsEl.getChild("language", MODS_V3_NS).getChild("languageTerm", MODS_V3_NS);
        assertEquals("iso639-2b", langTermEl.getAttribute("authority").getValue());
        assertEquals("eng", langTermEl.getText());
    }

    private WorkFormData deserializeFormJson(String jsonLocation) throws Exception {
        var jsonPath = Paths.get(jsonLocation);
        try (var formStream = Files.newInputStream(jsonPath)) {
            return mapper.readValue(formStream, WorkFormData.class);
        }
    }
}
