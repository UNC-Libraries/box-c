package edu.unc.lib.boxc.search.solr.services;

import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_ATROCITIES;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_DEMOGRAPHICS;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_MINORS_PRESENT;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_MISID_RISK;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_NAMED_INDIVS;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_NUDITY;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_PEOPLE_VISIBLE;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_RACIAL_OPPRESSION;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_REVIEW_ASSESS_FIELD;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_REVIEW_BIASED;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_REVIEW_DESC_CONTRADICTIONS;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_REVIEW_EUPH_LANG;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_REVIEW_INCON_DEMOS;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_REVIEW_JUDGMENTS;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_REVIEW_PEOPLE_FIRST;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_REVIEW_SAFETY_ASSESS_INCON;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_REVIEW_STEREOTYPING;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_REVIEW_SUPPORTED_CLAIMS;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_REVIEW_TEXT_CONTRADICTIONS;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_SAFETY_ASSESS_FIELD;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_SEXUAL;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_STEREOTYPING;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_SYMBOLS;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_TEXT_HANDWRITTEN;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_TEXT_PRESENT;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_TEXT_SENSITIVE;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.MG_VIOLENT;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.RESULT_FIELD;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.RESULT_HANDWRITTEN_CURSIVE;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.RESULT_HANDWRITTEN_PRINT;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.RESULT_TEXT_MIXED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;


/**
 * @author bbpennel
 */
public class MachineGeneratedContentServiceTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    public Path tempDir;

    private MachineGeneratedContentService service;
    private DerivativeService derivativeService;
    private PID filePid;

    @BeforeEach
    public void setup() throws Exception {
        derivativeService = new DerivativeService();
        derivativeService.setDerivativeDir(tempDir.toFile().getAbsolutePath());

        service = new MachineGeneratedContentService();
        service.setDerivativeService(derivativeService);

        filePid = PIDs.get(UUID.randomUUID().toString());
    }

    // ─── loadMachineGeneratedDescription ────────────────────────────────────

    @Test
    public void loadMachineGeneratedDescription_returnsFileContents() throws Exception {
        String json = loadDefaultJson();
        writeGeneratedDescriptionFile(filePid, json);

        String result = service.loadMachineGeneratedDescription(filePid);
        assertEquals(json, result);
    }

    @Test
    public void loadMachineGeneratedDescription_missingFile_throwsException() {
        assertThrows(IOException.class,
                () -> service.loadMachineGeneratedDescription(filePid));
    }

    // ─── deserializeMachineGeneratedDescription ──────────────────────────────

    @Test
    public void deserializeMachineGeneratedDescription_validJson_returnsNode() throws Exception {
        String json = loadDefaultJson();
        JsonNode node = service.deserializeMachineGeneratedDescription(json);
        assertNotNull(node);
        assertTrue(node.has("result"));
    }

    @Test
    public void deserializeMachineGeneratedDescription_invalidJson_throwsRepositoryException() {
        assertThrows(RepositoryException.class,
                () -> service.deserializeMachineGeneratedDescription("not valid json {{{"));
    }

    // ─── extractAltText ──────────────────────────────────────────────────────

    @Test
    public void extractAltText_returnsValue() throws Exception {
        JsonNode node = parseDefaultJson();
        assertEquals("Mountain landscape with snow-covered peaks", service.extractAltText(node));
    }

    @Test
    public void extractAltText_nullNode_returnsNull() {
        assertNull(service.extractAltText(null));
    }

    @Test
    public void extractAltText_missingField_returnsNull() throws Exception {
        JsonNode node = buildNodeWithoutField(parseDefaultJson(), "alt_text");
        assertNull(service.extractAltText(node));
    }

    // ─── extractFullDescription ───────────────────────────────────────────────

    @Test
    public void extractFullDescription_returnsValue() throws Exception {
        JsonNode node = parseDefaultJson();
        assertEquals(
                "A scenic mountain landscape with snow-capped peaks rising above a forested valley",
                service.extractFullDescription(node));
    }

    @Test
    public void extractFullDescription_nullNode_returnsNull() {
        assertNull(service.extractFullDescription(null));
    }

    @Test
    public void extractFullDescription_missingField_returnsNull() throws Exception {
        JsonNode node = buildNodeWithoutField(parseDefaultJson(), "full_description");
        assertNull(service.extractFullDescription(node));
    }

    // ─── extractTranscript ───────────────────────────────────────────────────

    @Test
    public void extractTranscript_returnsValue() throws Exception {
        JsonNode node = parseDefaultJson();
        // defaults JSON has an empty transcript
        assertNotNull(service.extractTranscript(node));
    }

    @Test
    public void extractTranscript_nullNode_returnsNull() {
        assertNull(service.extractTranscript(null));
    }

    @Test
    public void extractTranscript_missingField_returnsNull() throws Exception {
        JsonNode node = buildNodeWithoutField(parseDefaultJson(), "transcript");
        assertNull(service.extractTranscript(node));
    }

    // ─── extractRiskScore ────────────────────────────────────────────────────

    @Test
    public void extractRiskScore_returnsValue() throws Exception {
        JsonNode node = parseDefaultJson();
        assertEquals(0, service.extractRiskScore(node));
    }

    @Test
    public void extractRiskScore_nullNode_returnsNull() {
        assertNull(service.extractRiskScore(null));
    }

    @Test
    public void extractRiskScore_missingField_returnsNull() throws Exception {
        JsonNode node = buildNodeWithoutField(parseDefaultJson(), "overall_risk_Score");
        assertNull(service.extractRiskScore(node));
    }

    // ─── extractContentTags – defaults (no tags) ─────────────────────────────

    @Test
    public void extractContentTags_defaults_noTagsReturned() throws Exception {
        JsonNode node = parseDefaultJson();
        List<String> tags = service.extractContentTags(node);
        assertTrue(tags.isEmpty(),
                "Expected no tags for default JSON, but got: " + tags);
    }

    @Test
    public void extractContentTags_nullNode_returnsNull() {
        assertNull(service.extractContentTags(null));
    }

    // ─── extractContentTags – all tags set ───────────────────────────────────

    @Test
    public void extractContentTags_allTagsSet() throws Exception {
        JsonNode node = buildAllTagsJson();
        List<String> tags = service.extractContentTags(node);

        // Safety tags
        assertContainsTag(tags, MG_PEOPLE_VISIBLE);
        assertContainsTag(tags, MG_DEMOGRAPHICS);
        assertContainsTag(tags, MG_MINORS_PRESENT);
        assertContainsTag(tags, MG_NAMED_INDIVS);
        assertContainsTag(tags, MG_STEREOTYPING);
        assertContainsTag(tags, MG_MISID_RISK);
        assertContainsTag(tags, MG_VIOLENT);
        assertContainsTag(tags, MG_RACIAL_OPPRESSION);
        assertContainsTag(tags, MG_NUDITY);
        assertContainsTag(tags, MG_SEXUAL);
        assertContainsTag(tags, MG_ATROCITIES);
        assertContainsTag(tags, MG_SYMBOLS);
        assertContainsTag(tags, MG_TEXT_PRESENT);
        assertContainsTag(tags, MG_TEXT_HANDWRITTEN);
        assertContainsTag(tags, MG_TEXT_SENSITIVE);

        // Review tags
        assertContainsTag(tags, MG_REVIEW_BIASED);
        assertContainsTag(tags, MG_REVIEW_STEREOTYPING);
        assertContainsTag(tags, MG_REVIEW_JUDGMENTS);
        assertContainsTag(tags, MG_REVIEW_DESC_CONTRADICTIONS);
        assertContainsTag(tags, MG_REVIEW_TEXT_CONTRADICTIONS);
        assertContainsTag(tags, MG_REVIEW_INCON_DEMOS);
        assertContainsTag(tags, MG_REVIEW_EUPH_LANG);
        assertContainsTag(tags, MG_REVIEW_PEOPLE_FIRST);
        assertContainsTag(tags, MG_REVIEW_SUPPORTED_CLAIMS);
        assertContainsTag(tags, MG_REVIEW_SAFETY_ASSESS_INCON);
    }

    // ─── extractContentTags – UNKNOWN behavior ────────────────────────────────

    @Test
    public void extractContentTags_unknownYesNoFields_addsUnknownSuffixTags() throws Exception {
        JsonNode node = buildUnknownFieldsJson();
        List<String> tags = service.extractContentTags(node);

        assertContainsTag(tags, MG_PEOPLE_VISIBLE + "_unknown");
        assertContainsTag(tags, MG_DEMOGRAPHICS + "_unknown");
        assertContainsTag(tags, MG_MINORS_PRESENT + "_unknown");
        assertContainsTag(tags, MG_NAMED_INDIVS + "_unknown");
        assertContainsTag(tags, MG_STEREOTYPING + "_unknown");
    }

    // ─── extractContentTags – individual / edge cases ────────────────────────

    @Test
    public void extractContentTags_symbolsTypesContainsOnlyNone_noSymbolsTag() throws Exception {
        JsonNode node = parseDefaultJson();  // defaults already have types: ["NONE"]
        List<String> tags = service.extractContentTags(node);
        assertFalse(tags.contains(MG_SYMBOLS));
    }

    @Test
    public void extractContentTags_symbolsTypesNotNone_addsSymbolsTag() throws Exception {
        JsonNode node = parseDefaultJson();
        setNestedArrayValue((ObjectNode) node.path(RESULT_FIELD)
                .path(MG_SAFETY_ASSESS_FIELD)
                .path("symbols_present"));
        List<String> tags = service.extractContentTags(node);
        assertContainsTag(tags, MG_SYMBOLS);
    }

    @Test
    public void extractContentTags_misidRiskLow_noMisidTag() throws Exception {
        JsonNode node = parseDefaultJson();  // defaults have misidentification_risk_people: "LOW"
        List<String> tags = service.extractContentTags(node);
        assertFalse(tags.contains(MG_MISID_RISK));
    }

    @Test
    public void extractContentTags_misidRiskHigh_addsMisidTag() throws Exception {
        JsonNode node = parseDefaultJson();
        ((ObjectNode) node.path(RESULT_FIELD).path(MG_SAFETY_ASSESS_FIELD))
                .put("misidentification_risk_people", "HIGH");
        List<String> tags = service.extractContentTags(node);
        assertContainsTag(tags, MG_MISID_RISK);
    }

    @Test
    public void extractContentTags_textHandwrittenPrint_addsHandwrittenTag() throws Exception {
        JsonNode node = parseDefaultJson();
        ((ObjectNode) node.path(RESULT_FIELD).path(MG_SAFETY_ASSESS_FIELD)
                .path("text_characteristics"))
                .put("text_type", RESULT_HANDWRITTEN_PRINT);
        List<String> tags = service.extractContentTags(node);
        assertContainsTag(tags, MG_TEXT_HANDWRITTEN);
    }

    @Test
    public void extractContentTags_textHandwrittenCursive_addsHandwrittenTag() throws Exception {
        JsonNode node = parseDefaultJson();
        ((ObjectNode) node.path(RESULT_FIELD).path(MG_SAFETY_ASSESS_FIELD)
                .path("text_characteristics"))
                .put("text_type", RESULT_HANDWRITTEN_CURSIVE);
        List<String> tags = service.extractContentTags(node);
        assertContainsTag(tags, MG_TEXT_HANDWRITTEN);
    }

    @Test
    public void extractContentTags_textTypeMixed_addsHandwrittenTag() throws Exception {
        JsonNode node = parseDefaultJson();
        ((ObjectNode) node.path(RESULT_FIELD).path(MG_SAFETY_ASSESS_FIELD)
                .path("text_characteristics"))
                .put("text_type", RESULT_TEXT_MIXED);
        List<String> tags = service.extractContentTags(node);
        assertContainsTag(tags, MG_TEXT_HANDWRITTEN);
    }

    @Test
    public void extractContentTags_safetyConsistencyInconsistent_addsInconsistentTag() throws Exception {
        JsonNode node = parseDefaultJson();
        ((ObjectNode) node.path(RESULT_FIELD).path(MG_REVIEW_ASSESS_FIELD))
                .put("safety_assessment_consistency", "INCONSISTENT");
        List<String> tags = service.extractContentTags(node);
        assertContainsTag(tags, MG_REVIEW_SAFETY_ASSESS_INCON);
    }

    @Test
    public void extractContentTags_safetyConsistencyConsistent_noInconsistentTag() throws Exception {
        JsonNode node = parseDefaultJson(); // defaults have "CONSISTENT"
        List<String> tags = service.extractContentTags(node);
        assertFalse(tags.contains(MG_REVIEW_SAFETY_ASSESS_INCON));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void assertContainsTag(List<String> tags, String tag) {
        assertTrue(tags.contains(tag), "Expected tag '" + tag + "' in " + tags);
    }

    private String loadDefaultJson() throws Exception {
        return Files.readString(
                Path.of("src/test/resources/datastream/machineGeneratedDescriptionDefaults.json"));
    }

    private JsonNode parseDefaultJson() throws Exception {
        return MAPPER.readTree(loadDefaultJson());
    }

    private JsonNode buildNodeWithoutField(JsonNode baseNode, String fieldName) {
        ((ObjectNode) baseNode.path(RESULT_FIELD)).remove(fieldName);
        return baseNode;
    }

    private void setNestedArrayValue(ObjectNode parent) {
        parent.putArray("types").add("FLAG");
    }

    private void writeGeneratedDescriptionFile(PID pid, String content) throws Exception {
        Path path = derivativeService.getDerivativePath(pid, DatastreamType.GENERATED_DESCRIPTION);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    /**
     * Builds a JSON node where every flag that should produce a tag is set to trigger that tag.
     */
    private JsonNode buildAllTagsJson() throws Exception {
        JsonNode node = parseDefaultJson();
        ObjectNode result = (ObjectNode) node.path(RESULT_FIELD);

        // Safety assessment – all flags set to trigger tags
        ObjectNode safety = (ObjectNode) result.path(MG_SAFETY_ASSESS_FIELD);
        safety.put("people_visible", "YES");
        safety.put("demographics_described", "YES");
        safety.put("minors_present", "YES");
        safety.put("named_individuals_claimed", "YES");
        safety.put("stereotyping_present", "YES");
        safety.put("misidentification_risk_people", "HIGH");
        safety.put("violent_content", "PRESENT");
        safety.put("racial_violence_oppression", "PRESENT");
        safety.put("nudity", "PARTIAL");
        safety.put("sexual_content", "PRESENT");
        safety.put("atrocities_depicted", "YES");

        // symbols_present with non-NONE type
        ObjectNode symbolsNode = (ObjectNode) safety.path("symbols_present");
        setNestedArrayValue(symbolsNode);

        // text_characteristics
        ObjectNode textChars = (ObjectNode) safety.path("text_characteristics");
        textChars.put("text_present", "YES");
        textChars.put("text_type", "HANDWRITTEN_PRINT");
        textChars.put("text_sensitive", "SENSITIVE");

        // Review assessment – all flags set to YES / INCONSISTENT
        ObjectNode review = (ObjectNode) result.path(MG_REVIEW_ASSESS_FIELD);
        review.put("biased_language", "YES");
        review.put("stereotyping", "YES");
        review.put("value_judgments", "YES");
        review.put("contradictions_within_description", "YES");
        review.put("contradictions_between_texts", "YES");
        review.put("inconsistent_demographics", "YES");
        review.put("euphemistic_language", "YES");
        review.put("people_first_language", "YES");
        review.put("unsupported_inferential_claims", "YES");
        review.put("safety_assessment_consistency", "INCONSISTENT");

        return node;
    }

    /**
     * Builds a JSON node where every YES/NO field is set to UNKNOWN.
     */
    private JsonNode buildUnknownFieldsJson() throws Exception {
        JsonNode node = parseDefaultJson();
        ObjectNode result = (ObjectNode) node.path(RESULT_FIELD);

        ObjectNode safety = (ObjectNode) result.path(MG_SAFETY_ASSESS_FIELD);
        safety.put("people_visible", "UNKNOWN");
        safety.put("demographics_described", "UNKNOWN");
        safety.put("minors_present", "UNKNOWN");
        safety.put("named_individuals_claimed", "UNKNOWN");
        safety.put("stereotyping_present", "UNKNOWN");

        ObjectNode review = (ObjectNode) result.path(MG_REVIEW_ASSESS_FIELD);
        review.put("biased_language", "UNKNOWN");
        review.put("stereotyping", "UNKNOWN");
        review.put("value_judgments", "UNKNOWN");
        review.put("contradictions_within_description", "UNKNOWN");
        review.put("inconsistent_demographics", "UNKNOWN");
        review.put("euphemistic_language", "UNKNOWN");
        review.put("people_first_language", "UNKNOWN");
        review.put("unsupported_inferential_claims", "UNKNOWN");

        return node;
    }
}
