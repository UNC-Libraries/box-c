package edu.unc.lib.boxc.search.solr.models;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author bbpennel
 */
public class GroupedContentObjectSolrRecordTest {

    private static final String GROUP_ID = "uuid:group1";
    private static final String UUID1 = "b4f697b2-fe14-4333-927d-1f826a7a009c";
    private static final String UUID2 = "2a437964-d781-428c-b4f6-ff586a4b22fd";
    private static final String UUID3 = "da37feb3-0b2c-405c-b02a-cfab3737da85";
    

    @Test
    public void constructor_matchingRollup_selectsRepresentativeByRollup() {
        ContentObjectSolrRecord nonRep = makeRecord(UUID1, "uuid:other");
        ContentObjectSolrRecord rep = makeRecord(UUID2, GROUP_ID);

        GroupedContentObjectSolrRecord grouped =
                new GroupedContentObjectSolrRecord(GROUP_ID, List.of(nonRep, rep), 2L);

        assertSame(rep, grouped.getRepresentative());
    }

    @Test
    public void constructor_noRollupMatch_usesFirstItem() {
        ContentObjectSolrRecord first = makeRecord(UUID1, "uuid:nomatch");
        ContentObjectSolrRecord second = makeRecord(UUID2, "uuid:alsonomatch");

        GroupedContentObjectSolrRecord grouped =
                new GroupedContentObjectSolrRecord(GROUP_ID, List.of(first, second), 2L);

        assertSame(first, grouped.getRepresentative());
    }

    @Test
    public void constructor_nullGroupId_usesFirstItem() {
        ContentObjectSolrRecord first = makeRecord(UUID1, GROUP_ID);

        GroupedContentObjectSolrRecord grouped =
                new GroupedContentObjectSolrRecord(null, List.of(first), 1L);

        assertSame(first, grouped.getRepresentative());
    }

    @Test
    public void constructor_storesItemsAndCounts() {
        ContentObjectSolrRecord item = makeRecord(UUID1, GROUP_ID);

        GroupedContentObjectSolrRecord grouped =
                new GroupedContentObjectSolrRecord(GROUP_ID, List.of(item), 42L);

        assertEquals(1, grouped.getItems().size());
        assertEquals(42L, grouped.getItemCount());
        assertEquals(GROUP_ID, grouped.getGroupId());
    }

    @Test
    public void setRepresentative_overridesConstructorSelection() {
        ContentObjectSolrRecord original = makeRecord(UUID1, GROUP_ID);
        ContentObjectSolrRecord replacement = makeRecord(UUID3, GROUP_ID);

        GroupedContentObjectSolrRecord grouped =
                new GroupedContentObjectSolrRecord(GROUP_ID, List.of(original), 1L);
        grouped.setRepresentative(replacement);

        assertSame(replacement, grouped.getRepresentative());
    }

    @Test
    public void getId_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setId("da37feb3-0b2c-405c-b02a-cfab3737da85");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(UUID3, grouped.getId());
    }

    @Test
    public void getTitle_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setTitle("My Title");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("My Title", grouped.getTitle());
    }

    @Test
    public void getResourceType_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setResourceType("Work");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("Work", grouped.getResourceType());
    }

    @Test
    public void getAncestorPath_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setAncestorPath(List.of("1,uuid:root"));
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(List.of("1,uuid:root"), grouped.getAncestorPath());
    }

    @Test
    public void getAncestorIds_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setAncestorIds("/uuid:root/uuid:child");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("/uuid:root/uuid:child", grouped.getAncestorIds());
    }

    @Test
    public void getRollup_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(GROUP_ID, grouped.getRollup());
    }

    @Test
    public void getParentCollection_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setParentCollection("My Collection|uuid:col");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("My Collection|uuid:col", grouped.getParentCollection());
    }

    @Test
    public void getParentCollectionName_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setParentCollection("My Collection|uuid:col");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("My Collection", grouped.getParentCollectionName());
    }

    @Test
    public void getParentCollectionId_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setParentCollection("My Collection|uuid:col");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("uuid:col", grouped.getParentCollectionId());
    }

    @Test
    public void getCollectionId_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setCollectionId("col001");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("col001", grouped.getCollectionId());
    }

    @Test
    public void getFilesizeSort_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setFilesizeSort(1024L);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(1024L, grouped.getFilesizeSort());
    }

    @Test
    public void getFilesizeTotal_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setFilesizeTotal(2048L);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(2048L, grouped.getFilesizeTotal());
    }

    @Test
    public void getFileFormatCategory_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setFileFormatCategory(List.of("Image"));
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(List.of("Image"), grouped.getFileFormatCategory());
    }

    @Test
    public void getTimestamp_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        Date date = new Date();
        rep.setTimestamp(date);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(date, grouped.getTimestamp());
    }

    @Test
    public void getDateCreated_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        Date date = new Date();
        rep.setDateCreated(date);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(date, grouped.getDateCreated());
    }

    @Test
    public void getDateAdded_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        Date date = new Date();
        rep.setDateAdded(date);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(date, grouped.getDateAdded());
    }

    @Test
    public void getContentStatus_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setContentStatus(List.of("Described"));
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(List.of("Described"), grouped.getContentStatus());
    }

    @Test
    public void getStatus_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setStatus(List.of("Public"));
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(List.of("Public"), grouped.getStatus());
    }

    @Test
    public void getReadGroup_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setReadGroup(List.of("everyone"));
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(List.of("everyone"), grouped.getReadGroup());
    }

    @Test
    public void getAdminGroup_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setAdminGroup(List.of("admins"));
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(List.of("admins"), grouped.getAdminGroup());
    }

    @Test
    public void getCreator_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setCreator(List.of("Smith, Jane"));
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(List.of("Smith, Jane"), grouped.getCreator());
    }

    @Test
    public void getMemberOrderId_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setMemberOrderId(7);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(7, grouped.getMemberOrderId());
    }

    @Test
    public void getStreamingType_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setStreamingType("video");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("video", grouped.getStreamingType());
    }

    @Test
    public void getStreamingUrl_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setStreamingUrl("https://stream.example.com/video.mp4");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("https://stream.example.com/video.mp4", grouped.getStreamingUrl());
    }

    @Test
    public void getViewBehavior_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setViewBehavior("continuous");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("continuous", grouped.getViewBehavior());
    }

    @Test
    public void getAspaceRefId_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setAspaceRefId("ref123");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("ref123", grouped.getAspaceRefId());
    }

    @Test
    public void getHookId_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setHookId("hook456");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("hook456", grouped.getHookId());
    }

    @Test
    public void getFullText_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setFullText("full text here");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("full text here", grouped.getFullText());
    }

    @Test
    public void getAltText_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setAltText("A mountain scene.");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("A mountain scene.", grouped.getAltText());
    }

    @Test
    public void setAltText_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        grouped.setAltText("Updated alt text.");
        assertEquals("Updated alt text.", rep.getAltText());
    }

    @Test
    public void getFullDescription_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setFullDescription("A detailed description.");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("A detailed description.", grouped.getFullDescription());
    }

    @Test
    public void setFullDescription_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        grouped.setFullDescription("Updated description.");
        assertEquals("Updated description.", rep.getFullDescription());
    }

    @Test
    public void getMgContentTags_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setMgContentTags(List.of("people_visible", "nudity"));
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(List.of("people_visible", "nudity"), grouped.getMgContentTags());
    }

    @Test
    public void setMgContentTags_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        grouped.setMgContentTags(List.of("text_present"));
        assertEquals(List.of("text_present"), rep.getMgContentTags());
    }

    @Test
    public void getMgDescription_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setMgDescription("{\"result\":{}}");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("{\"result\":{}}", grouped.getMgDescription());
    }

    @Test
    public void setMgDescription_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        grouped.setMgDescription("{\"result\":{\"alt_text\":\"test\"}}");
        assertEquals("{\"result\":{\"alt_text\":\"test\"}}", rep.getMgDescription());
    }

    @Test
    public void getMgRiskScore_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setMgRiskScore(3);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(3, grouped.getMgRiskScore());
    }

    @Test
    public void setMgRiskScore_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        grouped.setMgRiskScore(7);
        assertEquals(7, rep.getMgRiskScore());
    }

    @Test
    public void getTranscript_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setTranscript("Transcribed text.");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("Transcribed text.", grouped.getTranscript());
    }

    @Test
    public void setTranscript_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        grouped.setTranscript("New transcript.");
        assertEquals("New transcript.", rep.getTranscript());
    }

    @Test
    public void setCountMap_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        Map<String, Long> counts = Map.of("child", 5L);
        grouped.setCountMap(counts);
        assertEquals(counts, rep.getCountMap());
    }

    @Test
    public void getCountMap_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        Map<String, Long> counts = Map.of("child", 5L);
        rep.setCountMap(counts);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals(counts, grouped.getCountMap());
    }

    @Test
    public void setThumbnailId_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        grouped.setThumbnailId("uuid:thumb");
        assertEquals("uuid:thumb", rep.getThumbnailId());
    }

    @Test
    public void getThumbnailId_delegatesToRepresentative() {
        ContentObjectSolrRecord rep = makeRecord(UUID3, GROUP_ID);
        rep.setThumbnailId("uuid:thumb");
        GroupedContentObjectSolrRecord grouped = grouped(rep);
        assertEquals("uuid:thumb", grouped.getThumbnailId());
    }

    private ContentObjectSolrRecord makeRecord(String id, String rollup) {
        ContentObjectSolrRecord record = new ContentObjectSolrRecord();
        record.setId(id);
        record.setRollup(rollup);
        return record;
    }

    /** Wrap a single record in a GroupedContentObjectSolrRecord using the shared GROUP_ID. */
    private GroupedContentObjectSolrRecord grouped(ContentObjectSolrRecord rep) {
        return new GroupedContentObjectSolrRecord(GROUP_ID, List.of(rep), 1L);
    }
}
