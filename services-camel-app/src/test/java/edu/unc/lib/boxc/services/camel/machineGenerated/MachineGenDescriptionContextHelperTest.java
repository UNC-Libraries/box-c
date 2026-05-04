package edu.unc.lib.boxc.services.camel.machineGenerated;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class MachineGenDescriptionContextHelperTest {
    private static final String KEEPSAKE_MODS_PATH = "datastreams/keepsakeMods.xml";
    private static final String SIMPLE_MODS_PATH = "datastreams/simpleMods.xml";
    private static final YAMLMapper YAML_MAPPER = YAMLMapper.builder().build();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private AccessGroupSet accessGroups;
    @Mock
    private FileObject fileObject;
    @Mock
    private WorkObject workObject;

    private AutoCloseable closeable;
    private MachineGenDescriptionContextHelper helper;

    private PID filePid;
    private PID workPid;
    private PID collPid;
    private PID adminUnitPid;
    private PID folder1Pid;
    private PID folder2Pid;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        helper = new MachineGenDescriptionContextHelper();
        helper.setRepositoryObjectLoader(repositoryObjectLoader);
        helper.setSolrSearchService(solrSearchService);
        helper.setAccessGroups(accessGroups);

        filePid = TestHelper.makePid();
        workPid = TestHelper.makePid();
        collPid = TestHelper.makePid();
        adminUnitPid = TestHelper.makePid();
        folder1Pid = TestHelper.makePid();
        folder2Pid = TestHelper.makePid();

        when(fileObject.getPid()).thenReturn(filePid);
        when(workObject.getPid()).thenReturn(workPid);
        when(repositoryObjectLoader.getFileObject(filePid)).thenReturn(fileObject);
        when(repositoryObjectLoader.getWorkObject(workPid)).thenReturn(workObject);
    }

    @AfterEach
    public void teardown() throws Exception {
        closeable.close();
    }

    /**
     * Strip the leading prompt sentence then parse the remaining YAML into a Map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseContext(String result) throws IOException {
        // The prompt is prepended before the YAML block; skip to the first "---"
        int yamlStart = result.indexOf("\n---");
        String yaml = result.substring(yamlStart + 1);
        return YAML_MAPPER.readValue(yaml, MAP_TYPE);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> section(Map<String, Object> ctx, String key) {
        return (Map<String, Object>) ctx.get(key);
    }

    @SuppressWarnings("unchecked")
    private List<String> list(Map<String, Object> ctx, String key) {
        return (List<String>) ctx.get(key);
    }

    /**
     * Build ancestorIds string: adminUnit/coll[/folder1/folder2]/work
     */
    private String ancestorIds(boolean withFolders) {
        if (withFolders) {
            return String.join("/",
                    adminUnitPid.getId(), collPid.getId(),
                    folder1Pid.getId(), folder2Pid.getId(),
                    workPid.getId());
        } else {
            return String.join("/", adminUnitPid.getId(), collPid.getId(), workPid.getId());
        }
    }

    private void mockFileRecord(boolean withFolders) {
        ContentObjectRecord record = mock(ContentObjectRecord.class);
        when(record.getAncestorIds()).thenReturn(ancestorIds(withFolders));
        when(record.getParentCollectionName()).thenReturn("Carolina Keepsakes");
        when(record.getTitle()).thenReturn("Pocket watch image");
        when(record.getLanguage()).thenReturn(List.of("English"));
        when(record.getCreator()).thenReturn(List.of("Green, Paul"));
        when(record.getGenre()).thenReturn(List.of("Photographs"));
        when(solrSearchService.getObjectById(argThat(requestFor(filePid)))).thenReturn(record);
    }

    private void mockWorkRecord() {
        ContentObjectRecord record = mock(ContentObjectRecord.class);
        when(record.getTitle()).thenReturn("Paul Green Keepsakes");
        when(record.getLanguage()).thenReturn(List.of("English"));
        when(record.getCreator()).thenReturn(List.of("Green, Paul"));
        when(record.getGenre()).thenReturn(null);
        when(solrSearchService.getObjectById(argThat(requestFor(workPid)))).thenReturn(record);
    }

    private void mockFolderRecord(PID folderPid, String title) {
        ContentObjectRecord record = mock(ContentObjectRecord.class);
        when(record.getTitle()).thenReturn(title);
        when(solrSearchService.getObjectById(argThat(requestFor(folderPid)))).thenReturn(record);
    }

    private ArgumentMatcher<SimpleIdRequest> requestFor(PID pid) {
        return req -> req != null && pid.getId().equals(req.getId());
    }

    private void mockModsStream(ContentObject obj, String resourcePath) throws Exception {
        BinaryObject descBinary = mock(BinaryObject.class);
        InputStream modsStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        when(descBinary.getBinaryStream()).thenReturn(modsStream);
        when(obj.getDescription()).thenReturn(descBinary);
    }

    @Test
    public void testFullContextWithFolders() throws Exception {
        mockModsStream(fileObject, KEEPSAKE_MODS_PATH);
        mockModsStream(workObject, KEEPSAKE_MODS_PATH);
        mockFileRecord(true);
        mockWorkRecord();
        mockFolderRecord(folder1Pid, "Green Family Papers");
        mockFolderRecord(folder2Pid, "Artifacts");

        String result = helper.generateContext(filePid);
        assertTrue(result.contains("Use the reference information below"), "Should contain prompt");

        Map<String, Object> ctx = parseContext(result);
        Map<String, Object> item = section(ctx, "item");
        Map<String, Object> work = section(ctx, "work");

        assertAll(
                // item — Solr fields
                () -> assertNotNull(item, "Should have item section"),
                () -> assertEquals("Pocket watch image", item.get("title"),
                        "item title should come from Solr"),
                () -> assertTrue(list(item, "creator").contains("Green, Paul"), "item should have creator"),
                () -> assertTrue(list(item, "language").contains("English"), "item should have language"),
                () -> assertTrue(list(item, "genre").contains("Photographs"), "item should have genre"),
                // item — MODS fields
                () -> assertTrue(list(item, "note").stream().anyMatch(n -> n.contains("14-carat yellow gold")),
                        "item should have MODS note"),
                () -> assertTrue(list(item, "subjects").contains("Paul Green"),
                        "item should have MODS topic subject"),
                () -> assertTrue(list(item, "subjects").contains("Green, Paul, 1894-1981"),
                        "item should have MODS name subject"),
                () -> assertTrue(list(item, "subjects").stream().anyMatch(s -> s.contains("Clocks")),
                        "item should have Clocks subject"),
                // work — Solr fields
                () -> assertNotNull(work, "Should have work section"),
                () -> assertEquals("Paul Green Keepsakes", work.get("title"), "work title should come from Solr"),
                // top-level fields
                () -> assertEquals("Carolina Keepsakes", ctx.get("collection_title"), "Should have collection_title"),
                () -> assertTrue(list(ctx, "folder_titles").contains("Green Family Papers"),
                        "Should have folder 1 title"),
                () -> assertTrue(list(ctx, "folder_titles").contains("Artifacts"),
                        "Should have folder 2 title")
        );
    }

    @Test
    public void testContextWithNoFolders() throws Exception {
        mockModsStream(fileObject, KEEPSAKE_MODS_PATH);
        mockModsStream(workObject, KEEPSAKE_MODS_PATH);
        mockFileRecord(false);
        mockWorkRecord();

        Map<String, Object> ctx = parseContext(helper.generateContext(filePid));

        assertAll(
                () -> assertNotNull(section(ctx, "item"), "Should have item section"),
                () -> assertNotNull(section(ctx, "work"), "Should have work section"),
                () -> assertEquals("Carolina Keepsakes", ctx.get("collection_title"), "Should have collection_title"),
                () -> assertNull(ctx.get("folder_titles"), "Should not have folder_titles when no folders")
        );

        verify(solrSearchService, never()).getObjectById(argThat(requestFor(folder1Pid)));
        verify(solrSearchService, never()).getObjectById(argThat(requestFor(folder2Pid)));
    }

    @Test
    public void testContextWithNoFileMods() throws Exception {
        when(fileObject.getDescription()).thenReturn(null);
        mockModsStream(workObject, SIMPLE_MODS_PATH);
        mockFileRecord(false);
        mockWorkRecord();

        Map<String, Object> ctx = parseContext(helper.generateContext(filePid));
        Map<String, Object> item = section(ctx, "item");
        Map<String, Object> work = section(ctx, "work");

        assertAll(
                // Solr fields still present in item
                () -> assertNotNull(item, "Should have item section"),
                () -> assertEquals("Pocket watch image", item.get("title"), "item should still have Solr title"),
                // MODS-only fields absent from item
                () -> assertNull(item.get("note"), "item should have no note when file MODS absent"),
                () -> assertNull(item.get("subjects"), "item should have no subjects when file MODS absent"),
                // work present
                () -> assertNotNull(work, "Should have work section"),
                () -> assertEquals("Paul Green Keepsakes", work.get("title"), "work should have Solr title")
        );
    }

    @Test
    public void testContextWithNoWorkMods() throws Exception {
        mockModsStream(fileObject, KEEPSAKE_MODS_PATH);
        when(workObject.getDescription()).thenReturn(null);
        mockFileRecord(false);
        mockWorkRecord();

        Map<String, Object> ctx = parseContext(helper.generateContext(filePid));
        Map<String, Object> item = section(ctx, "item");
        Map<String, Object> work = section(ctx, "work");

        assertAll(
                // item has MODS fields
                () -> assertNotNull(item, "Should have item section"),
                () -> assertTrue(list(item, "note").stream().anyMatch(n -> n.contains("14-carat yellow gold")),
                        "item should have MODS note"),
                () -> assertNotNull(item.get("subjects"), "item should have MODS subjects"),
                // work has Solr fields but no MODS fields
                () -> assertNotNull(work, "Should have work section"),
                () -> assertEquals("Paul Green Keepsakes", work.get("title"),
                        "work should have Solr title"),
                () -> assertNull(work.get("subjects"), "work should have no subjects when work MODS absent"),
                () -> assertNull(work.get("note"), "work should have no note when work MODS absent")
        );
    }

    @Test
    public void testContextWithSimpleModsForBoth() throws Exception {
        mockModsStream(fileObject, SIMPLE_MODS_PATH);
        mockModsStream(workObject, SIMPLE_MODS_PATH);
        mockFileRecord(false);
        mockWorkRecord();

        Map<String, Object> ctx = parseContext(helper.generateContext(filePid));
        Map<String, Object> item = section(ctx, "item");
        Map<String, Object> work = section(ctx, "work");

        assertAll(
                () -> assertNotNull(item, "Should have item section"),
                () -> assertNotNull(work, "Should have work section"),
                // simpleMods has no note, subjects, dateCreated, or dateIssued
                () -> assertNull(item.get("note"), "item should have no note"),
                () -> assertNull(item.get("subjects"), "item should have no subjects"),
                () -> assertNull(item.get("date_created"), "item should have no date_created"),
                () -> assertNull(item.get("date_issued"), "item should have no date_issued"),
                () -> assertNull(work.get("note"), "work should have no note"),
                () -> assertNull(work.get("subjects"), "work should have no subjects"),
                () -> assertEquals("Carolina Keepsakes", ctx.get("collection_title"), "Should have collection_title")
        );
    }

    @Test
    public void testContextWithModsReadError() throws Exception {
        mockModsStream(fileObject, SIMPLE_MODS_PATH);
        mockModsStream(workObject, SIMPLE_MODS_PATH);
        when(workObject.getDescription().getBinaryStream())
                .thenThrow(new FedoraException("Error reading MODS datastream"));
        mockFileRecord(false);
        mockWorkRecord();

        assertThrows(ServiceException.class, () -> helper.generateContext(filePid));
    }
}
