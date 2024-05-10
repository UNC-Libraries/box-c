package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.persist.api.PackagingType.BAGIT;
import static edu.unc.lib.boxc.persist.api.PackagingType.DIRECTORY;
import static edu.unc.lib.boxc.persist.api.storage.StorageType.FILESYSTEM;
import static edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper.addMapping;
import static edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper.createConfigFile;
import static edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper.createFilesystemConfig;
import static edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper.serializeLocationMappings;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.common.util.ZipFileUtil;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceCandidate;
import edu.unc.lib.boxc.persist.impl.sources.FilesystemIngestSource;
import edu.unc.lib.boxc.persist.impl.sources.IngestSourceManagerImpl;
import edu.unc.lib.boxc.persist.impl.sources.IngestSourceManagerImpl.IngestSourceMapping;
import edu.unc.lib.boxc.web.services.rest.modify.IngestSourceController.IngestPackageDetails;
import redis.clients.jedis.JedisPool;

/**
 *
 * @author bbpennel
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/redis-server-context.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/ingest-it-servlet.xml")
})
public class IngestFromSourcesIT extends AbstractAPIIT {
    private static final String DEPOSITOR = "adminuser";
    private static final String DEPOSITOR_EMAIL = "adminuser@example.com";

    @TempDir
    public Path tmpFolder;
    private String sourceFolderPath;

    private List<IngestSourceMapping> mappingList;

    private PID destPid;
    private PID rootPid;
    private PID adminUnitPid;

    @Autowired
    private DepositStatusFactory depositStatusFactory;
    @Autowired
    private IngestSourceManagerImpl sourceManager;
    @Autowired
    private ContentPathFactory contentPathFactory;
    @Autowired
    private JedisPool jedisPool;

    @BeforeEach
    public void setup() throws Exception {
        rootPid = RepositoryPaths.getContentRootPid();
        adminUnitPid = makePid();
        destPid = makePid();

        sourceFolderPath = tmpFolder.resolve("sourceFolder").toString();
        Files.createDirectory(tmpFolder.resolve("sourceFolder"));
        mappingList = new ArrayList<>();

        AccessGroupSet testPrincipals = new AccessGroupSetImpl("admins");

        GroupsThreadStore.storeUsername(DEPOSITOR);
        GroupsThreadStore.storeGroups(testPrincipals);
        GroupsThreadStore.storeEmail(DEPOSITOR_EMAIL);
    }

    @AfterEach
    public void teardownLocal() throws Exception {
        jedisPool.getResource().flushAll();
    }

    // List sources/candidates tests

    @Test
    public void listSourcesInsufficientPermissions() throws Exception {
        Path configPath = createConfigFile(
                createBasicConfig("testsource", sourceFolderPath, destPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(destPid), any(AccessGroupSetImpl.class), eq(Permission.ingest));

        mvc.perform(get("/edit/ingestSources/list/" + destPid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void listSourcesOrphanedTarget() throws Exception {
        Path configPath = createConfigFile(
                createBasicConfig("testsource", sourceFolderPath, destPid));
        initializeManager(configPath);

        mockAncestors(destPid);

        mvc.perform(get("/edit/ingestSources/list/" + destPid.getId()))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    public void listSourcesNoMatchingSources() throws Exception {
        Path configPath = createConfigFile();
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        MvcResult result = mvc.perform(get("/edit/ingestSources/list/" + destPid.getId()))
                .andExpect(status().isOk())
                .andReturn();

        ListSourcesResponse resp = deserializeListSources(result);
        assertTrue(resp.getCandidates().isEmpty());
        assertTrue(resp.getSources().isEmpty());
    }

    @Test
    public void listSourcesWithSourcesAndCandidates() throws Exception {
        String sourceFolderPath2 = tmpFolder.resolve("sourceFolder2").toString();
        Files.createDirectory(tmpFolder.resolve("sourceFolder2"));

        createBagCandidate(sourceFolderPath, "cand1");
        createDirCandidate(sourceFolderPath2, "cand2");

        Path configPath = createConfigFile(
                createBasicConfig("testsource1", sourceFolderPath, destPid),
                createBasicConfig("testsource2", sourceFolderPath2, adminUnitPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        MvcResult result = mvc.perform(get("/edit/ingestSources/list/" + destPid.getId()))
                .andExpect(status().isOk())
                .andReturn();

        ListSourcesResponse resp = deserializeListSources(result);
        List<IngestSourceCandidate> candidates = resp.getCandidates();
        assertEquals(2, candidates.size());

        IngestSourceCandidate cand1 = getCandidateByPath(candidates, "cand1");
        assertNotNull(cand1);
        assertEquals(BAGIT, cand1.getPackagingType());
        assertEquals(3, cand1.getFileCount().intValue());

        IngestSourceCandidate cand2 = getCandidateByPath(candidates, "cand2");
        assertNotNull(cand2);
        assertEquals(DIRECTORY, cand2.getPackagingType());

        List<IngestSource> sources = resp.getSources();
        assertEquals(2, sources.size());

        IngestSource source1 = getSourceByName(sources, "testsource1");
        assertNotNull(source1);

        IngestSource source2 = getSourceByName(sources, "testsource2");
        assertNotNull(source2);
    }

    @Test
    public void listSourcesWithExcludedSource() throws Exception {
        String sourceFolderPath2 = tmpFolder.resolve("sourceFolder2").toString();
        Files.createDirectory(tmpFolder.resolve("sourceFolder2"));

        createBagCandidate(sourceFolderPath, "cand1");
        createBagCandidate(sourceFolderPath2, "cand2");

        Map<String, Object> internalSource = createBasicConfig("testsource2", sourceFolderPath2, destPid);
        internalSource.put("internal", true);
        Path configPath = createConfigFile(
                createBasicConfig("testsource1", sourceFolderPath, destPid),
                internalSource);
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        MvcResult result = mvc.perform(get("/edit/ingestSources/list/" + destPid.getId()))
                .andExpect(status().isOk())
                .andReturn();

        ListSourcesResponse resp = deserializeListSources(result);
        List<IngestSourceCandidate> candidates = resp.getCandidates();
        assertEquals(1, candidates.size());

        IngestSourceCandidate cand1 = getCandidateByPath(candidates, "cand1");
        assertNotNull(cand1);
        assertEquals(BAGIT, cand1.getPackagingType());

        List<IngestSource> sources = resp.getSources();
        assertEquals(1, sources.size());

        IngestSource source1 = getSourceByName(sources, "testsource1");
        assertNotNull(source1);
    }

    // Ingest tests

    @Test
    public void ingestBagInsufficientPermissions() throws Exception {
        Path candPath1 = createBagCandidate(sourceFolderPath, "cand1");

        Path configPath = createConfigFile(
                createBasicConfig("testsource", sourceFolderPath, destPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(destPid), any(AccessGroupSetImpl.class), eq(Permission.ingest));

        List<IngestPackageDetails> details = asList(
                new IngestPackageDetails("testsource", candPath1.getFileName().toString(), BAGIT,
                        null, false));

        mvc.perform(post("/edit/ingestSources/ingest/" + destPid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(details)))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void ingestBagWithExtraFields() throws Exception {
        Path candPath1 = createBagCandidate(sourceFolderPath, "cand1");

        Path configPath = createConfigFile(
                createBasicConfig("testsource1", sourceFolderPath, destPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        List<IngestPackageDetails> details = asList(
                new IngestPackageDetails("testsource1", candPath1.getFileName().toString(), BAGIT,
                        "candidate", false));

        MvcResult result = mvc.perform(post("/edit/ingestSources/ingest/" + destPid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(details)))
                .andExpect(status().isOk())
                .andReturn();

        List<String> depositIds = verifySuccessResponse(result, destPid);

        Map<String, String> candStatus1 = getDepositStatusByPath(depositIds, candPath1);
        assertEquals(destPid.getId(), candStatus1.get(DepositField.containerId.name()));
        assertEquals(BAGIT.getUri(), candStatus1.get(DepositField.packagingType.name()));
        assertEquals("candidate", candStatus1.get(DepositField.depositSlug.name()));
        assertDepositorDetailsStored(candStatus1);
    }

    @Test
    public void ingestNoPackagingType() throws Exception {
        Path candPath1 = createBagCandidate(sourceFolderPath, "cand1");

        Path configPath = createConfigFile(
                createBasicConfig("testsource1", sourceFolderPath, destPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        List<IngestPackageDetails> details = asList(
                new IngestPackageDetails("testsource1", candPath1.getFileName().toString(), null,
                        null, false));

        mvc.perform(post("/edit/ingestSources/ingest/" + destPid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(details)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void ingestNoPackagePath() throws Exception {
        Path configPath = createConfigFile(
                createBasicConfig("testsource1", sourceFolderPath, destPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        List<IngestPackageDetails> details = asList(
                new IngestPackageDetails("testsource1", null, BAGIT,
                        null, false));

        mvc.perform(post("/edit/ingestSources/ingest/" + destPid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(details)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void ingestCandidatePathDoesNotExist() throws Exception {
        // Make a path to a resource that hasn't been created
        Path candPath1 = Paths.get(sourceFolderPath, "noExistCand");

        Path configPath = createConfigFile(
                createBasicConfig("testsource1", sourceFolderPath, destPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        List<IngestPackageDetails> details = asList(
                new IngestPackageDetails("testsource1", candPath1.getFileName().toString(), BAGIT,
                        null, false));

        mvc.perform(post("/edit/ingestSources/ingest/" + destPid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(details)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void ingestCandidateOutsideOfSource() throws Exception {
        // Candidate in path that doesn't match the path of the configured source
        String sourceFolderPath2 = tmpFolder.resolve("sourceFolder2").toString();
        Files.createDirectory(tmpFolder.resolve("sourceFolder2"));
        Path candPath1 = createBagCandidate(sourceFolderPath2, "cand1");

        Path configPath = createConfigFile(
                createBasicConfig("testsource1", sourceFolderPath, destPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        List<IngestPackageDetails> details = asList(
                new IngestPackageDetails("testsource1", candPath1.getFileName().toString(), BAGIT,
                        null, false));

        mvc.perform(post("/edit/ingestSources/ingest/" + destPid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(details)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void ingestInvalidSourceId() throws Exception {
        Path candPath1 = createBagCandidate(sourceFolderPath, "cand1");

        Path configPath = createConfigFile(
                createBasicConfig("testsource1", sourceFolderPath, destPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        List<IngestPackageDetails> details = asList(
                new IngestPackageDetails("sourceWHOA", candPath1.getFileName().toString(), BAGIT,
                        null, false));

        mvc.perform(post("/edit/ingestSources/ingest/" + destPid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(details)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void ingestBagAndDirectory() throws Exception {
        String sourceFolderPath2 = tmpFolder.resolve("sourceFolder2").toString();
        Files.createDirectory(tmpFolder.resolve("sourceFolder2"));

        Path candPath1 = createBagCandidate(sourceFolderPath, "cand1");
        Path candPath2 = createDirCandidate(sourceFolderPath2, "cand2");

        Path configPath = createConfigFile(
                createBasicConfig("testsource1", sourceFolderPath, destPid),
                createBasicConfig("testsource2", sourceFolderPath2, adminUnitPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        List<IngestPackageDetails> details = asList(
                new IngestPackageDetails("testsource1", candPath1.getFileName().toString(), BAGIT,
                        null, true),
                new IngestPackageDetails("testsource2", candPath2.getFileName().toString(), DIRECTORY,
                        null, false));

        MvcResult result = mvc.perform(post("/edit/ingestSources/ingest/" + destPid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(details)))
                .andExpect(status().isOk())
                .andReturn();

        List<String> depositIds = verifySuccessResponse(result, destPid);

        Map<String, String> candStatus1 = getDepositStatusByPath(depositIds, candPath1);
        assertEquals(destPid.getId(), candStatus1.get(DepositField.containerId.name()));
        assertEquals(BAGIT.getUri(), candStatus1.get(DepositField.packagingType.name()));
        assertTrue(Boolean.parseBoolean(candStatus1.get(DepositField.staffOnly.name())));
        assertFalse(Boolean.parseBoolean(candStatus1.get(DepositField.createParentFolder.name())));
        assertDepositorDetailsStored(candStatus1);

        Map<String, String> candStatus2 = getDepositStatusByPath(depositIds, candPath2);
        assertEquals(destPid.getId(), candStatus2.get(DepositField.containerId.name()));
        assertEquals(DIRECTORY.getUri(), candStatus2.get(DepositField.packagingType.name()));
        assertFalse(Boolean.parseBoolean(candStatus2.get(DepositField.staffOnly.name())));
        assertDepositorDetailsStored(candStatus2);
    }

    @Test
    public void ingestBagWithCreateParent() throws Exception {
        Path candPath1 = createBagCandidate(sourceFolderPath, "cand1");

        Path configPath = createConfigFile(
                createBasicConfig("testsource1", sourceFolderPath, destPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        IngestPackageDetails bagDetails = new IngestPackageDetails("testsource1", candPath1.getFileName().toString(),
                BAGIT, null, false);
        bagDetails.setCreateParentFolder(true);
        List<IngestPackageDetails> details = asList(bagDetails);

        MvcResult result = mvc.perform(post("/edit/ingestSources/ingest/" + destPid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(details)))
                .andExpect(status().isOk())
                .andReturn();

        List<String> depositIds = verifySuccessResponse(result, destPid);

        Map<String, String> candStatus1 = getDepositStatusByPath(depositIds, candPath1);
        assertEquals(destPid.getId(), candStatus1.get(DepositField.containerId.name()));
        assertEquals(BAGIT.getUri(), candStatus1.get(DepositField.packagingType.name()));
        assertFalse(Boolean.parseBoolean(candStatus1.get(DepositField.staffOnly.name())));
        assertTrue(Boolean.parseBoolean(candStatus1.get(DepositField.createParentFolder.name())));
        assertDepositorDetailsStored(candStatus1);
    }

    // Test retrieving result from list and submitting to ingest service works
    @Test
    public void roundTripDirectory() throws Exception {
        Path candPath1 = createDirCandidate(sourceFolderPath, "cand1");

        Path configPath = createConfigFile(
                createBasicConfig("testsource1", sourceFolderPath, destPid));
        initializeManager(configPath);

        mockAncestors(destPid, rootPid, adminUnitPid);

        MvcResult result = mvc.perform(get("/edit/ingestSources/list/" + destPid.getId()))
                .andExpect(status().isOk())
                .andReturn();

        ListSourcesResponse resp = deserializeListSources(result);
        IngestSourceCandidate candidate = resp.getCandidates().get(0);

        // Create ingest package details from a candidate in the previous result
        List<IngestPackageDetails> details = asList(
                new IngestPackageDetails(candidate.getSourceId(),
                        candidate.getPatternMatched(),
                        candidate.getPackagingType(),
                        null, false));

        MvcResult ingestResult = mvc.perform(post("/edit/ingestSources/ingest/" + destPid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(details)))
                .andExpect(status().isOk())
                .andReturn();

        List<String> depositIds = verifySuccessResponse(ingestResult, destPid);
        Map<String, String> candStatus1 = getDepositStatusByPath(depositIds, candPath1);
        assertEquals(destPid.getId(), candStatus1.get(DepositField.containerId.name()));
        assertEquals(candidate.getPackagingType().getUri(), candStatus1.get(DepositField.packagingType.name()));
    }

    @SuppressWarnings("unchecked")
    private List<String> verifySuccessResponse(MvcResult result, PID destPid) throws Exception {
        Map<String, Object> resp = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(destPid.getId(), resp.get("destination"));
        assertEquals("ingest", resp.get("action"));
        List<String> depositIds = (List<String>) resp.get("depositIds");
        assertNotNull(depositIds);
        return depositIds;
    }

    private Map<String, String> getDepositStatusByPath(List<String> depositIds, Path candPath) {
        for (String depositId: depositIds) {
            Map<String, String> status = depositStatusFactory.get(depositId);

            if (status.get(DepositField.sourceUri.name()).equals(candPath.toUri().toString())) {
                return status;
            }
        }
        return null;
    }

    private void assertDepositorDetailsStored(Map<String, String> status) {
        assertEquals(DEPOSITOR, status.get(DepositField.depositorName.name()));
        assertEquals(DEPOSITOR_EMAIL, status.get(DepositField.depositorEmail.name()));
        AccessGroupSet depositPrincipals = new AccessGroupSetImpl(status.get(DepositField.permissionGroups.name()));
        assertTrue(depositPrincipals.contains("admins"), "admins principal must be set in deposit");
    }

    private Map<String, Object> createBasicConfig(String id, String path, PID... containers) {
        for (PID container: containers) {
            addMapping(id, container, mappingList);
        }

        return createFilesystemConfig(id, "Source " + id, Paths.get(path), asList("*"));
    }

    private Path createDirCandidate(String sourcePath, String name) throws Exception {
        return Files.createDirectory(Paths.get(sourcePath, name));
    }

    private Path createBagCandidate(String sourcePath, String name) throws Exception {
        File original = new File("src/test/resources/bag_with_files.zip");
        File dest = new File(sourcePath);
        ZipFileUtil.unzipToDir(original, dest);
        Path destPath = Paths.get(sourcePath, "bag_with_files");
        return Files.move(destPath, destPath.resolveSibling(name));
    }

    private IngestSourceCandidate getCandidateByPath(List<IngestSourceCandidate> candidates, String fileName) {
        return candidates.stream()
                .filter(c -> c.getPatternMatched().equals(fileName))
                .findFirst()
                .orElse(null);
    }

    private IngestSource getSourceByName(List<IngestSource> sources, String name) {
        return sources.stream()
                .filter(c -> c.getId().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void mockAncestors(PID target, PID... ancestors) {
        when(contentPathFactory.getAncestorPids(target)).thenReturn(
                asList(ancestors));
    }

    private void initializeManager(Path configPath) throws Exception {
        Path mappingPath = serializeLocationMappings(mappingList);

        sourceManager.setConfigPath(configPath.toString());
        sourceManager.setMappingPath(mappingPath.toString());
        sourceManager.init();
    }

    private ListSourcesResponse deserializeListSources(MvcResult result) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(
                new NamedType(FilesystemIngestSource.class, FILESYSTEM.getId()));
        return mapper.readValue(result.getResponse().getContentAsString(), ListSourcesResponse.class);
    }

    public static class ListSourcesResponse {
        private List<IngestSource> sources;
        private List<IngestSourceCandidate> candidates;

        public List<IngestSource> getSources() {
            return sources;
        }

        public void setSources(List<IngestSource> sources) {
            this.sources = sources;
        }

        public List<IngestSourceCandidate> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<IngestSourceCandidate> candidates) {
            this.candidates = candidates;
        }
    }
}
