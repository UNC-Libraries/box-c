package edu.unc.lib.boxc.indexing.solr.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for deserialization and interaction with machine generated content datastreams.
 * @author bbpennel
 */
public class MachineGeneratedContentService {
    private static final Logger log = LoggerFactory.getLogger(MachineGeneratedContentService.class);

    public static final String MG_DESCRIPTION_FIELD = "full_description";
    public static final String MG_RISK_SCORE_FIELD = "overall_risk_Score";
    public static final String MG_ALT_TEXT = "alt_text";
    public static final String MG_TRANSCRIPT_FIELD = "transcript";
    public static final String MG_SAFETY_ASSESS_FIELD = "safety_assessment";
    public static final String MG_REVIEW_ASSESS_FIELD = "review_assessment";
    public static final String MG_PEOPLE_VISIBLE = "people_visible";
    public static final String MG_DEMOGRAPHICS = "demographics";
    public static final String MG_MISID_RISK = "misidentification_risk_people";
    public static final String MG_MINORS_PRESENT = "minors_present";
    public static final String MG_NAMED_INDIVS = "named_individuals";
    public static final String MG_VIOLENT = "violent_content";
    public static final String MG_RACIAL_OPPRESSION = "racial_oppression";
    public static final String MG_NUDITY = "nudity";
    public static final String MG_SEXUAL = "sexual_content";
    public static final String MG_SYMBOLS = "symbols_present";
    public static final String MG_STEREOTYPING = "stereotyping";
    public static final String MG_ATROCITIES = "atrocities";
    public static final String MG_TEXT_PRESENT = "text_present";
    public static final String MG_TEXT_HANDWRITTEN = "text_handwritten";
    public static final String MG_TEXT_SENSITIVE = "text_sensitive";

    public static final String MG_REVIEW_BIASED = "model_biased_language";
    public static final String MG_REVIEW_STEREOTYPING = "model_stereotyping";
    public static final String MG_REVIEW_JUDGMENTS = "model_value_judgments";
    public static final String MG_REVIEW_DESC_CONTRADICTIONS = "contradictions_within_description";
    public static final String MG_REVIEW_TEXT_CONTRADICTIONS = "contradictions_between_texts";
    public static final String MG_REVIEW_INCON_DEMOS = "inconsistent_demographics";
    public static final String MG_REVIEW_EUPH_LANG = "model_euphemistic_language";
    public static final String MG_REVIEW_PEOPLE_FIRST = "model_people_first_language";
    public static final String MG_REVIEW_SUPPORTED_CLAIMS = "unsupported_claims";
    public static final String MG_REVIEW_SAFETY_ASSESS_INCON = "safety_assessment_inconsistent";

    public static final String RESULT_FIELD = "result";
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private DerivativeService derivativeService;

    /**
     * Loads the machine generated description JSON string for the given file PID, if it exists.
     * @param filePid file
     * @return
     * @throws IOException
     */
    public String loadMachineGeneratedDescription(PID filePid) throws IOException {
        Path mgdPath = derivativeService.getDerivativePath(filePid, DatastreamType.GENERATED_DESCRIPTION);
        log.debug("Loading MGD content for {} at path {}", filePid, mgdPath);
        return Files.readString(mgdPath);
    }

    /**
     * Deserializes the machine generated description JSON string into a JsonNode for easier interaction.
     * @param mgdJson
     * @return
     */
    public JsonNode deserializeMachineGeneratedDescription(String mgdJson) {
        try {
            return MAPPER.readTree(mgdJson);
        } catch (JsonProcessingException e) {
            throw new IndexingException("Unable to deserialize machine generated JSON", e);
        }
    }

    /**
     * Extracts the alt text from the machine generated description JSON, if it exists.
     * @param mgdNode the machine generated description JSON root node
     * @return alt text if it exists, otherwise null
     */
    public String extractAltText(JsonNode mgdNode) {
        return extractTextField(mgdNode, MG_ALT_TEXT);
    }

    /**
     * Extracts the full description from the machine generated description JSON, if it exists.
     * @param mgdNode the machine generated description JSON root node
     * @return full description if it exists, otherwise null
     */
    public String extractFullDescription(JsonNode mgdNode) {
        return extractTextField(mgdNode, MG_DESCRIPTION_FIELD);
    }

    /**
     * Extracts the transcript from the machine generated description JSON, if it exists.
     * @param mgdNode the machine generated description JSON root node
     * @return transcript if it exists, otherwise null
     */
    public String extractTranscript(JsonNode mgdNode) {
        return extractTextField(mgdNode, MG_TRANSCRIPT_FIELD);
    }

    private String extractTextField(JsonNode mgdNode, String fieldName) {
        if (mgdNode == null) {
            return null;
        }
        JsonNode textNode = mgdNode.path(RESULT_FIELD).path(fieldName);
        return textNode.isMissingNode() ? null : textNode.asText();
    }

    /**
     * Extracts the risk score from the machine generated description JSON, if it exists.
     * @param mgdNode the machine generated description JSON root node
     * @return risk score if it exists, otherwise null
     */
    public Integer extractRiskScore(JsonNode mgdNode) {
        if (mgdNode == null) {
            return null;
        }
        JsonNode scoreNode = mgdNode.path(RESULT_FIELD).path(MG_RISK_SCORE_FIELD);
        return scoreNode.isMissingNode() ? null : scoreNode.asInt();
    }

    /**
     * Extracts content tags based on the machine generated description JSON. Tags are generated based review
     * and safety assessments in the JSON. Examples include: "people_visible", "minors_present_unknown",
     * "model_biased_language", etc.
     * @param mgdNode root node of the machine generated description JSON
     * @return list of content tags from the JSON, or empty list if there were none, or null if input was null
     */
    public List<String> extractContentTags(JsonNode mgdNode) {
        if (mgdNode == null) {
            return null;
        }
        List<String> tags = new ArrayList<>();
        JsonNode safetyNode = mgdNode.path(RESULT_FIELD).path(MG_SAFETY_ASSESS_FIELD);
        if (!safetyNode.isMissingNode()) {
            // YES/NO/UNKNOWN fields
            addYesNoTag(tags, safetyNode, "people_visible", MG_PEOPLE_VISIBLE);
            addYesNoTag(tags, safetyNode, "demographics_described", MG_DEMOGRAPHICS);
            addYesNoTag(tags, safetyNode, "minors_present", MG_MINORS_PRESENT);
            addYesNoTag(tags, safetyNode, "named_individuals_claimed", MG_NAMED_INDIVS);
            addYesNoTag(tags, safetyNode, "stereotyping_present", MG_STEREOTYPING);
            addYesNoTag(tags, safetyNode, "atrocities_depicted", MG_ATROCITIES);

            // misidentification_risk_people: add tag if value is not LOW
            JsonNode misidNode = safetyNode.path("misidentification_risk_people");
            if (!misidNode.isMissingNode()) {
                String misidVal = misidNode.asText();
                if (!"LOW".equalsIgnoreCase(misidVal)) {
                    tags.add(MG_MISID_RISK);
                }
            }

            // NONE-based fields: add tag if value is not NONE
            addNoneBasedTag(tags, safetyNode, "violent_content", MG_VIOLENT);
            addNoneBasedTag(tags, safetyNode, "racial_violence_oppression", MG_RACIAL_OPPRESSION);
            addNoneBasedTag(tags, safetyNode, "nudity", MG_NUDITY);
            addNoneBasedTag(tags, safetyNode, "sexual_content", MG_SEXUAL);

            // symbols_present: add tag unless types contains "NONE"
            JsonNode typesNode = safetyNode.path("symbols_present").path("types");
            if (!typesNode.isMissingNode()) {
                boolean allNone = true;
                if (typesNode.isArray()) {
                    for (JsonNode typeVal : typesNode) {
                        if (!"NONE".equalsIgnoreCase(typeVal.asText())) {
                            allNone = false;
                            break;
                        }
                    }
                }
                if (!allNone) {
                    tags.add(MG_SYMBOLS);
                }
            }

            // text_characteristics
            JsonNode textCharsNode = safetyNode.path("text_characteristics");
            if (!textCharsNode.isMissingNode()) {
                // text_present: YES/NO/UNKNOWN field
                addYesNoTag(tags, textCharsNode, "text_present", MG_TEXT_PRESENT);

                // text_sensitive: add if text_type indicates sensitive
                JsonNode textSensitiveNode = textCharsNode.path("text_sensitive");
                if (!textSensitiveNode.isMissingNode() && "SENSITIVE".equalsIgnoreCase(textSensitiveNode.asText())) {
                    tags.add(MG_TEXT_SENSITIVE);
                }

                // text_handwritten: add if text_type is HANDWRITTEN_PRINT, HANDWRITTEN_CURSIVE, or MIXED
                JsonNode textTypeNode = textCharsNode.path("text_type");
                if (!textTypeNode.isMissingNode()) {
                    String textType = textTypeNode.asText();
                    if ("HANDWRITTEN_PRINT".equalsIgnoreCase(textType)
                            || "HANDWRITTEN_CURSIVE".equalsIgnoreCase(textType)
                            || "MIXED".equalsIgnoreCase(textType)) {
                        tags.add(MG_TEXT_HANDWRITTEN);
                    }
                }
            }
        }
        JsonNode reviewNode = mgdNode.path(RESULT_FIELD).path(MG_REVIEW_ASSESS_FIELD);
        if (!reviewNode.isMissingNode()) {
            addYesNoTag(tags, reviewNode, "biased_language", MG_REVIEW_BIASED);
            addYesNoTag(tags, reviewNode, "stereotyping", MG_REVIEW_STEREOTYPING);
            addYesNoTag(tags, reviewNode, "value_judgments", MG_REVIEW_JUDGMENTS);
            addYesNoTag(tags, reviewNode, "contradictions_within_description", MG_REVIEW_DESC_CONTRADICTIONS);
            addYesNoTag(tags, reviewNode, "contradictions_between_texts", MG_REVIEW_TEXT_CONTRADICTIONS);
            addYesNoTag(tags, reviewNode, "inconsistent_demographics", MG_REVIEW_INCON_DEMOS);
            addYesNoTag(tags, reviewNode, "euphemistic_language", MG_REVIEW_EUPH_LANG);
            addYesNoTag(tags, reviewNode, "people_first_language", MG_REVIEW_PEOPLE_FIRST);
            addYesNoTag(tags, reviewNode, "unsupported_inferential_claims", MG_REVIEW_SUPPORTED_CLAIMS);

            JsonNode safetyConsistencyNode = reviewNode.path("safety_assessment_consistency");
            if (!safetyConsistencyNode.isMissingNode()
                    && "INCONSISTENT".equalsIgnoreCase(safetyConsistencyNode.asText())) {
                tags.add(MG_REVIEW_SAFETY_ASSESS_INCON);
            }
        }
        log.debug("Generated content tags: {}", tags);
        return tags;
    }

    /**
     * Adds a tag for a YES/NO/UNKNOWN field. If the value is "YES", adds the tag name as-is.
     * If the value is "UNKNOWN", adds the tag name with "_unknown" suffix.
     * If the value is "NO", no tag is added.
     */
    private void addYesNoTag(List<String> tags, JsonNode node, String jsonField, String tagName) {
        JsonNode fieldNode = node.path(jsonField);
        if (!fieldNode.isMissingNode()) {
            String val = fieldNode.asText();
            if ("YES".equalsIgnoreCase(val)) {
                tags.add(tagName);
            } else if ("UNKNOWN".equalsIgnoreCase(val)) {
                tags.add(tagName + "_unknown");
            }
        }
    }

    /**
     * Adds a tag for a NONE-based field. If the value is not "NONE", adds the tag.
     */
    private void addNoneBasedTag(List<String> tags, JsonNode node, String jsonField, String tagName) {
        JsonNode fieldNode = node.path(jsonField);
        if (!fieldNode.isMissingNode() && !"NONE".equalsIgnoreCase(fieldNode.asText())) {
            tags.add(tagName);
        }
    }

    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }
}
