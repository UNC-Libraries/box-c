package edu.unc.lib.boxc.search.solr.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author bbpennel
 */
public class IndexDocumentBeanTest {
    private IndexDocumentBean bean;

    @BeforeEach
    public void setup() {
        bean = new IndexDocumentBean();
    }

    @Test
    public void testId() {
        assertNull(bean.getId());
        String uuid = UUID.randomUUID().toString();
        bean.setId(uuid);
        assertEquals(uuid, bean.getId());
        assertEquals(uuid, bean.getPid().getId());
    }

    @Test
    public void testAncestorPath() {
        assertNull(bean.getAncestorPath());
        bean.setAncestorPath(List.of("1,uuid:abc", "2,uuid:def"));
        assertEquals(List.of("1,uuid:abc", "2,uuid:def"), bean.getAncestorPath());
    }

    @Test
    public void testAncestorIds() {
        assertNull(bean.getAncestorIds());
        bean.setAncestorIds("/uuid:abc/uuid:def");
        assertEquals("/uuid:abc/uuid:def", bean.getAncestorIds());
    }

    @Test
    public void testParentCollection() {
        assertNull(bean.getParentCollection());
        bean.setParentCollection("uuid:col1");
        assertEquals("uuid:col1", bean.getParentCollection());
    }

    @Test
    public void testParentUnit() {
        assertNull(bean.getParentUnit());
        bean.setParentUnit("uuid:unit1");
        assertEquals("uuid:unit1", bean.getParentUnit());
    }

    @Test
    public void testRollup() {
        assertNull(bean.getRollup());
        bean.setRollup("uuid:rollup1");
        assertEquals("uuid:rollup1", bean.getRollup());
    }

    @Test
    public void testVersion() {
        assertNull(bean.get_version_());
        bean.set_version_(42L);
        assertEquals(42L, bean.get_version_());
    }

    @Test
    public void testDatastream() {
        assertNull(bean.getDatastream());
        bean.setDatastream(List.of("original_file|image/jpeg|file.jpg|jpg|1024|||"));
        assertEquals(List.of("original_file|image/jpeg|file.jpg|jpg|1024|||"), bean.getDatastream());
    }

    @Test
    public void testFilesizeSort() {
        assertNull(bean.getFilesizeSort());
        bean.setFilesizeSort(2048L);
        assertEquals(2048L, bean.getFilesizeSort());
    }

    @Test
    public void testFilesizeTotal() {
        assertNull(bean.getFilesizeTotal());
        bean.setFilesizeTotal(4096L);
        assertEquals(4096L, bean.getFilesizeTotal());
    }

    @Test
    public void testResourceType() {
        assertNull(bean.getResourceType());
        bean.setResourceType("File");
        assertEquals("File", bean.getResourceType());
    }

    @Test
    public void testResourceTypeSort() {
        assertNull(bean.getResourceTypeSort());
        bean.setResourceTypeSort(6);
        assertEquals(6, bean.getResourceTypeSort());
    }

    @Test
    public void testFileFormatCategory() {
        assertNull(bean.getFileFormatCategory());
        bean.setFileFormatCategory(List.of("Image"));
        assertEquals(List.of("Image"), bean.getFileFormatCategory());
    }

    @Test
    public void testFileFormatType() {
        assertNull(bean.getFileFormatType());
        bean.setFileFormatType(List.of("image/jpeg"));
        assertEquals(List.of("image/jpeg"), bean.getFileFormatType());
    }

    @Test
    public void testFileFormatDescription() {
        assertNull(bean.getFileFormatDescription());
        bean.setFileFormatDescription(List.of("JPEG Image"));
        assertEquals(List.of("JPEG Image"), bean.getFileFormatDescription());
    }

    @Test
    public void testTimestamp() {
        assertNull(bean.getTimestamp());
        Date now = new Date();
        bean.setTimestamp(now);
        assertEquals(now, bean.getTimestamp());
    }

    @Test
    public void testLastIndexed() {
        assertNull(bean.getLastIndexed());
        Date now = new Date();
        bean.setLastIndexed(now);
        assertEquals(now, bean.getLastIndexed());
    }

    @Test
    public void testRoleGroup() {
        assertNull(bean.getRoleGroup());
        bean.setRoleGroup(List.of("canViewOriginals|everyone"));
        assertEquals(List.of("canViewOriginals|everyone"), bean.getRoleGroup());
    }

    @Test
    public void testReadGroup() {
        assertNull(bean.getReadGroup());
        bean.setReadGroup(List.of("everyone"));
        assertEquals(List.of("everyone"), bean.getReadGroup());
    }

    @Test
    public void testAdminGroup() {
        assertNull(bean.getAdminGroup());
        bean.setAdminGroup(List.of("adminGroup1"));
        assertEquals(List.of("adminGroup1"), bean.getAdminGroup());
    }

    @Test
    public void testStatus() {
        assertNull(bean.getStatus());
        bean.setStatus(List.of("Public"));
        assertEquals(List.of("Public"), bean.getStatus());
    }

    @Test
    public void testContentStatus() {
        assertNull(bean.getContentStatus());
        bean.setContentStatus(List.of("Described"));
        assertEquals(List.of("Described"), bean.getContentStatus());
    }

    @Test
    public void testIdentifier() {
        assertNull(bean.getIdentifier());
        bean.setIdentifier(List.of("local|abc123"));
        assertEquals(List.of("local|abc123"), bean.getIdentifier());
    }

    @Test
    public void testIdentifierSort() {
        assertNull(bean.getIdentifierSort());
        bean.setIdentifierSort("abc123");
        assertEquals("abc123", bean.getIdentifierSort());
    }

    @Test
    public void testMemberOrderId() {
        assertNull(bean.getMemberOrderId());
        bean.setMemberOrderId(3);
        assertEquals(3, bean.getMemberOrderId());
    }

    @Test
    public void testViewBehavior() {
        assertNull(bean.getViewBehavior());
        bean.setViewBehavior("continuous");
        assertEquals("continuous", bean.getViewBehavior());
    }

    @Test
    public void testTitle() {
        assertNull(bean.getTitle());
        bean.setTitle("Test Title");
        assertEquals("Test Title", bean.getTitle());
    }

    @Test
    public void testOtherTitle() {
        assertNull(bean.getOtherTitle());
        bean.setOtherTitle(List.of("Alt Title"));
        assertEquals(List.of("Alt Title"), bean.getOtherTitle());
    }

    @Test
    public void testAbstractText() {
        assertNull(bean.getAbstractText());
        bean.setAbstractText("An abstract.");
        assertEquals("An abstract.", bean.getAbstractText());
    }

    @Test
    public void testCollectionDisplaySettings() {
        assertNull(bean.getCollectionDisplaySettings());
        bean.setCollectionDisplaySettings("thumbnail");
        assertEquals("thumbnail", bean.getCollectionDisplaySettings());
    }

    @Test
    public void testCollectionId() {
        assertNull(bean.getCollectionId());
        bean.setCollectionId("col001");
        assertEquals("col001", bean.getCollectionId());
    }

    @Test
    public void testKeyword() {
        assertNull(bean.getKeyword());
        bean.setKeyword(List.of("mountains", "landscape"));
        assertEquals(List.of("mountains", "landscape"), bean.getKeyword());
    }

    @Test
    public void testSubject() {
        assertNull(bean.getSubject());
        bean.setSubject(List.of("Nature"));
        assertEquals(List.of("Nature"), bean.getSubject());
    }

    @Test
    public void testOtherSubject() {
        assertNull(bean.getOtherSubject());
        bean.setOtherSubject(List.of("Environment"));
        assertEquals(List.of("Environment"), bean.getOtherSubject());
    }

    @Test
    public void testGenre() {
        assertNull(bean.getGenre());
        bean.setGenre(List.of("Photograph"));
        assertEquals(List.of("Photograph"), bean.getGenre());
    }

    @Test
    public void testDateCreatedYear() {
        assertNull(bean.getDateCreatedYear());
        bean.setDateCreatedYear("1985");
        assertEquals("1985", bean.getDateCreatedYear());
    }

    @Test
    public void testLanguage() {
        assertNull(bean.getLanguage());
        bean.setLanguage(List.of("English"));
        assertEquals(List.of("English"), bean.getLanguage());
    }

    @Test
    public void testLocation() {
        assertNull(bean.getLocation());
        bean.setLocation(List.of("North Carolina"));
        assertEquals(List.of("North Carolina"), bean.getLocation());
    }

    @Test
    public void testCreator() {
        assertNull(bean.getCreator());
        bean.setCreator(List.of("Smith, Jane"));
        assertEquals(List.of("Smith, Jane"), bean.getCreator());
    }

    @Test
    public void testContributor() {
        assertNull(bean.getContributor());
        bean.setContributor(List.of("Doe, John"));
        assertEquals(List.of("Doe, John"), bean.getContributor());
    }

    @Test
    public void testCreatorContributor() {
        assertNull(bean.getCreatorContributor());
        bean.setCreatorContributor(List.of("Smith, Jane", "Doe, John"));
        assertEquals(List.of("Smith, Jane", "Doe, John"), bean.getCreatorContributor());
    }

    @Test
    public void testDateCreated() {
        assertNull(bean.getDateCreated());
        Date date = new Date();
        bean.setDateCreated(date);
        assertEquals(date, bean.getDateCreated());
    }

    @Test
    public void testDateAdded() throws Exception {
        assertNull(bean.getDateAdded());
        Date date = new Date();
        bean.setDateAdded(date);
        assertEquals(date, bean.getDateAdded());
        bean.setDateAdded("2024-01-01T12:00:00Z");
        assertNotNull(bean.getDateAdded());
    }

    @Test
    public void testDateUpdated() throws Exception {
        assertNull(bean.getDateUpdated());
        Date date = new Date();
        bean.setDateUpdated(date);
        assertEquals(date, bean.getDateUpdated());
        bean.setDateUpdated("2024-01-01T12:00:00Z");
        assertNotNull(bean.getDateUpdated());
    }

    @Test
    public void testCitation() {
        assertNull(bean.getCitation());
        bean.setCitation("Some citation");
        assertEquals("Some citation", bean.getCitation());
    }

    @Test
    public void testExhibit() {
        assertNull(bean.getExhibit());
        bean.setExhibit(List.of("exhibit1"));
        assertEquals(List.of("exhibit1"), bean.getExhibit());
    }

    @Test
    public void testPublisher() {
        assertNull(bean.getPublisher());
        bean.setPublisher(List.of("UNC Press"));
        assertEquals(List.of("UNC Press"), bean.getPublisher());
    }

    @Test
    public void testRights() {
        assertNull(bean.getRights());
        bean.setRights(List.of("CC-BY"));
        assertEquals(List.of("CC-BY"), bean.getRights());
    }

    @Test
    public void testRightsOaiPmh() {
        assertNull(bean.getRightsOaiPmh());
        bean.setRightsOaiPmh(List.of("open"));
        assertEquals(List.of("open"), bean.getRightsOaiPmh());
    }

    @Test
    public void testRightsUri() {
        assertNull(bean.getRightsUri());
        bean.setRightsUri(List.of("https://creativecommons.org/licenses/by/4.0/"));
        assertEquals(List.of("https://creativecommons.org/licenses/by/4.0/"), bean.getRightsUri());
    }

    @Test
    public void testFullText() {
        assertNull(bean.getFullText());
        bean.setFullText("full text content");
        assertEquals("full text content", bean.getFullText());
    }

    @Test
    public void testFullDescription() {
        assertNull(bean.getFullDescription());
        bean.setFullDescription("A detailed description.");
        assertEquals("A detailed description.", bean.getFullDescription());
    }

    @Test
    public void testMgContentTags() {
        assertNull(bean.getMgContentTags());
        bean.setMgContentTags(List.of("people_visible", "nudity"));
        assertEquals(List.of("people_visible", "nudity"), bean.getMgContentTags());
    }

    @Test
    public void testMgContentTags_emptyList() {
        bean.setMgContentTags(List.of());
        assertTrue(bean.getMgContentTags().isEmpty());
    }

    @Test
    public void testMgDescription() {
        assertNull(bean.getMgDescription());
        bean.setMgDescription("{\"result\": {}}");
        assertEquals("{\"result\": {}}", bean.getMgDescription());
    }

    @Test
    public void testMgRiskScore() {
        assertNull(bean.getMgRiskScore());
        bean.setMgRiskScore(5);
        assertEquals(5, bean.getMgRiskScore());
    }

    @Test
    public void testTranscript() {
        assertNull(bean.getTranscript());
        bean.setTranscript("Transcribed text here.");
        assertEquals("Transcribed text here.", bean.getTranscript());
    }

    @Test
    public void testStreamingType() {
        assertNull(bean.getStreamingType());
        bean.setStreamingType("video");
        assertEquals("video", bean.getStreamingType());
    }

    @Test
    public void testStreamingUrl() {
        assertNull(bean.getStreamingUrl());
        bean.setStreamingUrl("https://stream.example.com/video.mp4");
        assertEquals("https://stream.example.com/video.mp4", bean.getStreamingUrl());
    }

    @Test
    public void testAspaceRefId() {
        assertNull(bean.getAspaceRefId());
        bean.setAspaceRefId("ref123");
        assertEquals("ref123", bean.getAspaceRefId());
    }

    @Test
    public void testHookId() {
        assertNull(bean.getHookId());
        bean.setHookId("hook456");
        assertEquals("hook456", bean.getHookId());
    }

    @Test
    public void testAltText() {
        assertNull(bean.getAltText());
        bean.setAltText("A snowy mountain landscape.");
        assertEquals("A snowy mountain landscape.", bean.getAltText());
    }

    @Test
    public void testDynamicFields() {
        assertNull(bean.getDynamicFields());
        Map<String, Object> dynamicFields = Map.of("rla_site_code_d", "ABC");
        bean.setDynamicFields(dynamicFields);
        assertEquals(dynamicFields, bean.getDynamicFields());
    }

    @Test
    public void testGetFields_reflectsSetValues() {
        bean.setTitle("My Title");
        bean.setResourceType("Work");
        assertEquals("My Title", bean.getFields().get("title"));
        assertEquals("Work", bean.getFields().get("resourceType"));
    }
}
