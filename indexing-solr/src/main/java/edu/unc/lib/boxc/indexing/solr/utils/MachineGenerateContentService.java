package edu.unc.lib.boxc.indexing.solr.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service for deserialization and interaction with machine generated content datastreams.
 * @author bbpennel
 */
public class MachineGenerateContentService {
    public static final String MG_CONTENT_TAGS_FIELD = "mgContentTags";
    public static final String MG_DESCRIPTION_FIELD = "full_description";
    public static final String MG_RISK_SCORE_FIELD = "overall_risk_Score";
    public static final String MG_ALT_TEXT = "alt_text";
    public static final String MG_TRANSCRIPT_FIELD = "transcript";
    public static final String MG_SAFETY_ASSESS_FIELD = "safety_assessment";
    public static final String MG_REVIEW_ASSESS_FIELD = "review_assessment";

    public static final String RESULT_FIELD = "result";
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private DerivativeService derivativeService;

    public String loadMachineGeneratedDescription(PID filePid) throws IOException {
        Path mgdPath = derivativeService.getDerivativePath(filePid, DatastreamType.GENERATED_DESCRIPTION);
        return Files.readString(mgdPath);
    }

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

    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }
}
