package edu.unc.lib.boxc.persist.impl.sources;

import static edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper.addBagToSource;
import static edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper.addMapping;
import static edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper.assertBagDetails;
import static edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper.createConfigFile;
import static edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper.createFilesystemConfig;
import static edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper.findCandidateByPath;
import static edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper.serializeLocationMappings;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.exceptions.OrphanedObjectException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.persist.api.exceptions.UnknownIngestSourceException;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceCandidate;
import edu.unc.lib.boxc.persist.impl.sources.IngestSourceManagerImpl.IngestSourceMapping;

/**
 * @author bbpennel
 */
public class IngestSourceManagerImplTest {
    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private Path sourceFolderPath;

    private Path configPath;
    private Path mappingPath;

    private IngestSourceManagerImpl sourceMan;

    private List<IngestSourceMapping> mappingList;

    @Mock
    private ContentPathFactory contentPathFactory;

    private PID rootPid;
    private PID adminUnitPid;
    private PID destPid;

    @Before
    public void init() throws Exception {
        initMocks(this);
        tmpFolder.create();
        sourceFolderPath = tmpFolder.newFolder().toPath();

        mappingList = new ArrayList<>();

        rootPid = RepositoryPaths.getContentRootPid();
        adminUnitPid = makePid();
        destPid = makePid();

        mockAncestors(destPid, rootPid, adminUnitPid);
    }

    private void initializeManager() throws Exception {
        mappingPath = serializeLocationMappings(mappingList);

        sourceMan = new IngestSourceManagerImpl();
        sourceMan.setContentPathFactory(contentPathFactory);
        sourceMan.setConfigPath(configPath.toString());
        sourceMan.setMappingPath(mappingPath.toString());
        sourceMan.init();
    }

    @Test(expected = UnknownIngestSourceException.class)
    public void testMappingToNonExistentSource() throws Exception {
        IngestSourceMapping mapping = new IngestSourceMapping();
        mapping.setId(destPid.getId());
        mapping.setSources(asList("wutsource"));
        mappingList.add(mapping);

        configPath = createConfigFile();

        initializeManager();
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicateMappingForContainerId() throws Exception {
        configPath = createConfigFile(createBasicConfig("testsource1", destPid));

        // Add duplicate mapping
        IngestSourceMapping mapping = new IngestSourceMapping();
        mapping.setId(destPid.getId());
        mapping.setSources(asList("testsource1"));
        mappingList.add(mapping);

        initializeManager();
    }

    @Test
    public void testGetIngestSourceByIdExists() throws Exception {
        configPath = createConfigFile(createBasicConfig("testsource", destPid));
        initializeManager();

        IngestSource source = sourceMan.getIngestSourceById("testsource");
        assertEquals("testsource", source.getId());
    }

    @Test
    public void testGetIngestSourceByIdDoesNotExist() throws Exception {
        configPath = createConfigFile(createBasicConfig("testsource", destPid));
        initializeManager();

        IngestSource source = sourceMan.getIngestSourceById("wutsource");
        assertNull(source);
    }

    @Test
    public void testListSourcesDirectMatch() throws Exception {
        configPath = createConfigFile(createBasicConfig("testsource", destPid));
        initializeManager();

        List<IngestSource> sources = sourceMan.listSources(destPid);

        assertEquals("Only one source should match the path", 1, sources.size());
        assertContainsSource(sources, "testsource");
    }

    @Test
    public void testListSourcesDirectMatchExcludeInternal() throws Exception {
        Map<String, Object> sourceConfig = createBasicConfig("testsource", destPid);
        sourceConfig.put("internal", true);
        configPath = createConfigFile(sourceConfig);
        initializeManager();

        List<IngestSource> sources = sourceMan.listSources(destPid);

        assertEquals("Internal source should not be returned", 0, sources.size());
    }

    @Test
    public void testListSourcesMatchAncestor() throws Exception {
        configPath = createConfigFile(createBasicConfig("testsource", adminUnitPid));
        initializeManager();

        List<IngestSource> sources = sourceMan.listSources(destPid);

        assertEquals("Only one source should match the path", 1, sources.size());
        assertContainsSource(sources, "testsource");
    }

    @Test
    public void testListSourcesMatchMultipleAncestors() throws Exception {
        configPath = createConfigFile(
                createBasicConfig("testsource1", destPid),
                createBasicConfig("testsource2", adminUnitPid));
        initializeManager();

        List<IngestSource> sources = sourceMan.listSources(destPid);

        assertEquals("Two sources should match the path", 2, sources.size());
        assertContainsSource(sources, "testsource1");
        assertContainsSource(sources, "testsource2");
    }

    @Test
    public void testListSourcesNoSourcesConfigured() throws Exception {
        configPath = createConfigFile();

        initializeManager();

        List<IngestSource> sources = sourceMan.listSources(destPid);

        assertTrue("No sources expected", sources.isEmpty());
    }

    @Test
    public void testListSourcesNoMatches() throws Exception {
        PID anotherUnitPid = makePid();
        configPath = createConfigFile(createBasicConfig("testsource", anotherUnitPid));
        initializeManager();

        List<IngestSource> sources = sourceMan.listSources(destPid);

        assertTrue("No sources expected", sources.isEmpty());
    }

    @Test(expected = OrphanedObjectException.class)
    public void testListSourcesOrphanedTarget() throws Exception {
        configPath = createConfigFile(createBasicConfig("testsource", adminUnitPid));
        initializeManager();

        mockAncestors(destPid);
        sourceMan.listSources(destPid);
    }

    @Test
    public void testListSourcesMultipleFromSamePid() throws Exception {
        configPath = createConfigFile(
                createBasicConfig("testsource1", adminUnitPid),
                createBasicConfig("testsource2", adminUnitPid));
        initializeManager();

        List<IngestSource> sources = sourceMan.listSources(destPid);

        assertEquals("Two sources should match the path", 2, sources.size());
        assertContainsSource(sources, "testsource1");
        assertContainsSource(sources, "testsource2");
    }

    @Test
    public void testListSourcesBoundToMultipleContainers() throws Exception {
        PID anotherUnitPid = makePid();
        configPath = createConfigFile(createBasicConfig("testsource", adminUnitPid, anotherUnitPid));
        initializeManager();

        List<IngestSource> sources = sourceMan.listSources(destPid);

        assertEquals("Only one source should match the path", 1, sources.size());
        assertContainsSource(sources, "testsource");
    }

    @Test
    public void testListCandidatesBag() throws Exception {
        addBagToSource(sourceFolderPath);

        configPath = createConfigFile(createBasicConfig("testsource", destPid));
        initializeManager();

        List<IngestSourceCandidate> candidates = sourceMan.listCandidates(destPid);

        assertEquals(1, candidates.size());

        IngestSourceCandidate candidate = candidates.get(0);

        assertBagDetails(candidate, "testsource", "bag_with_files");
    }

    @Test
    public void testListCandidatesNoCandidates() throws Exception {
        configPath = createConfigFile(createBasicConfig("testsource", destPid));
        initializeManager();

        List<IngestSourceCandidate> candidates = sourceMan.listCandidates(destPid);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void testListCandidatesFromMultipleSources() throws Exception {
        Path source2FolderPath = tmpFolder.newFolder().toPath();

        addBagToSource(source2FolderPath, "bag_with_files", "second_bag");
        addBagToSource(sourceFolderPath);

        Map<String, Object> source2 = createConfig(
                "testsource2",
                "Source 2",
                source2FolderPath,
                asList("*"),
                destPid);
        configPath = createConfigFile(
                source2,
                createBasicConfig("testsource", destPid));
        initializeManager();

        List<IngestSourceCandidate> candidates = sourceMan.listCandidates(destPid);

        assertEquals(2, candidates.size());

        IngestSourceCandidate candidate1 = findCandidateByPath("bag_with_files", candidates);
        assertBagDetails(candidate1, "testsource", "bag_with_files");

        IngestSourceCandidate candidate2 = findCandidateByPath("second_bag", candidates);
        assertBagDetails(candidate2, "testsource2", "second_bag");
    }

    @Test
    public void testListCandidatesNoMatchingSource() throws Exception {
        // Populate a ingest source that is not in the path of the destination
        addBagToSource(sourceFolderPath);

        PID anotherUnit = makePid();
        configPath = createConfigFile(createBasicConfig("testsource", anotherUnit));
        initializeManager();

        List<IngestSourceCandidate> candidates = sourceMan.listCandidates(destPid);

        assertTrue("No candidates expected", candidates.isEmpty());
    }

    @Test
    public void testGetIngestSourceForUri_NestedUri() throws Exception {
        configPath = createConfigFile(createBasicConfig("testsource", destPid));
        Path source2FolderPath = tmpFolder.newFolder().toPath();

        Map<String, Object> source2 = createConfig(
                "testsource2",
                "Source 2",
                source2FolderPath,
                asList("*"),
                destPid);
        configPath = createConfigFile(source2, createBasicConfig("testsource", destPid));

        Path targetPath = addBagToSource(sourceFolderPath);

        initializeManager();

        IngestSource source = sourceMan.getIngestSourceForUri(targetPath.toUri());
        assertEquals("testsource", source.getId());
    }

    @Test
    public void testGetIngestSourceForUri_ValidPathTargetNotExist() throws Exception {
        configPath = createConfigFile(createBasicConfig("testsource", destPid));

        initializeManager();

        URI targetUri = sourceFolderPath.resolve("my_target").toUri();

        IngestSource source = sourceMan.getIngestSourceForUri(targetUri);
        assertEquals("testsource", source.getId());
    }

    @Test
    public void testGetIngestSourceForUri_NestedPath() throws Exception {
        configPath = createConfigFile(createBasicConfig("testsource", destPid));

        Path bagPath = addBagToSource(sourceFolderPath);
        Path nestedPath = bagPath.resolve("data/test1.txt");

        initializeManager();

        IngestSource source = sourceMan.getIngestSourceForUri(nestedPath.toUri());
        assertEquals("testsource", source.getId());
    }

    @Test
    public void testGetIngestSourceForUri_NestedNotExist() throws Exception {
        configPath = createConfigFile(createBasicConfig("testsource", destPid));

        Path bagPath = addBagToSource(sourceFolderPath);
        Path nestedPath = bagPath.resolve("data/somewhere.txt");

        initializeManager();

        IngestSource source = sourceMan.getIngestSourceForUri(nestedPath.toUri());
        assertEquals("testsource", source.getId());
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private void mockAncestors(PID target, PID... ancestors) {
        when(contentPathFactory.getAncestorPids(target)).thenReturn(
                asList(ancestors));
    }

    private void assertContainsSource(List<IngestSource> sources, String sourceId) {
        assertTrue("Did not contain expected source " + sourceId,
                sources.stream().anyMatch(s -> s.getId().equals(sourceId)));
    }

    private Map<String, Object> createBasicConfig(String id, PID... containers) {
        return createConfig(id, "Source " + id, sourceFolderPath, asList("*"), containers);
    }

    private Map<String, Object> createConfig(String id, String name, Path basePath, List<String> patterns, PID... containers) {
        for (PID container: containers) {
            addMapping(id, container, mappingList);

        }

        return createFilesystemConfig(id, name, basePath, patterns);
    }
}