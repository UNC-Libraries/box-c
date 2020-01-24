/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.persist.services.ingest;

import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.addBagToSource;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.addMapping;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.assertBagDetails;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.createConfigFile;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.createFilesystemConfig;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.findCandidateByPath;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.serializeLocationMappings;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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

import edu.unc.lib.dl.exceptions.OrphanedObjectException;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.ingest.IngestSource;
import edu.unc.lib.dl.persist.api.ingest.IngestSourceCandidate;
import edu.unc.lib.dl.persist.api.ingest.UnknownIngestSourceException;
import edu.unc.lib.dl.persist.services.ingest.IngestSourceManagerImpl.IngestSourceMapping;

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