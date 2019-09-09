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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.dl.exceptions.OrphanedObjectException;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PackagingType;

/**
 * @author bbpennel
 */
public class IngestSourceManagerTest {
    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private String sourceFolderPath;

    private IngestSourceManager sourceMan;

    @Mock
    private ContentPathFactory contentPathFactory;

    private PID rootPid;
    private PID adminUnitPid;
    private PID destPid;

    @Before
    public void init() throws Exception {
        initMocks(this);
        tmpFolder.create();
        sourceFolderPath = tmpFolder.newFolder().getAbsolutePath();

        rootPid = RepositoryPaths.getContentRootPid();
        adminUnitPid = makePid();
        destPid = makePid();

        mockAncestors(destPid, rootPid, adminUnitPid);
    }

    private void initializeManager(Path configPath) throws Exception {
        sourceMan = new IngestSourceManager();
        sourceMan.setContentPathFactory(contentPathFactory);
        sourceMan.setConfigPath(configPath.toString());
        sourceMan.setWatchForChanges(false);
        sourceMan.init();
    }

    @Test
    public void testListSourcesDirectMatch() throws Exception {
        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

        List<IngestSourceConfiguration> sources = sourceMan.listSources(destPid);

        assertEquals("Only one source should match the path", 1, sources.size());
        assertContainsSource(sources, "testsource");
    }

    @Test
    public void testListSourcesMatchAncestor() throws Exception {
        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", adminUnitPid)));
        initializeManager(configPath);

        List<IngestSourceConfiguration> sources = sourceMan.listSources(destPid);

        assertEquals("Only one source should match the path", 1, sources.size());
        assertContainsSource(sources, "testsource");
    }

    @Test
    public void testListSourcesMatchMultipleAncestors() throws Exception {
        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource1", destPid),
                createBasicConfig("testsource2", adminUnitPid)));

        initializeManager(configPath);

        List<IngestSourceConfiguration> sources = sourceMan.listSources(destPid);

        assertEquals("Two sources should match the path", 2, sources.size());
        assertContainsSource(sources, "testsource1");
        assertContainsSource(sources, "testsource2");
    }

    @Test
    public void testListSourcesNoSourcesConfigured() throws Exception {
        Path configPath = createConfigFile(asList());
        initializeManager(configPath);

        List<IngestSourceConfiguration> sources = sourceMan.listSources(destPid);

        assertTrue("No sources expected", sources.isEmpty());
    }

    @Test
    public void testListSourcesNoMatches() throws Exception {
        PID anotherUnitPid = makePid();
        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", anotherUnitPid)));
        initializeManager(configPath);

        List<IngestSourceConfiguration> sources = sourceMan.listSources(destPid);

        assertTrue("No sources expected", sources.isEmpty());
    }

    @Test(expected = OrphanedObjectException.class)
    public void testListSourcesOrphanedTarget() throws Exception {
        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", adminUnitPid)));
        initializeManager(configPath);

        mockAncestors(destPid);
        sourceMan.listSources(destPid);
    }

    @Test
    public void testListSourcesMultipleFromSamePid() throws Exception {
        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource1", adminUnitPid),
                createBasicConfig("testsource2", adminUnitPid)));
        initializeManager(configPath);

        List<IngestSourceConfiguration> sources = sourceMan.listSources(destPid);

        assertEquals("Two sources should match the path", 2, sources.size());
        assertContainsSource(sources, "testsource1");
        assertContainsSource(sources, "testsource2");
    }

    @Test
    public void testListSourcesBoundToMultipleContainers() throws Exception {
        PID anotherUnitPid = makePid();
        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", adminUnitPid, anotherUnitPid)));
        initializeManager(configPath);

        List<IngestSourceConfiguration> sources = sourceMan.listSources(destPid);

        assertEquals("Only one source should match the path", 1, sources.size());
        assertContainsSource(sources, "testsource");
    }

    @Test
    public void testListCandidatesBag() throws Exception {
        addBagToSource(sourceFolderPath);

        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

        List<IngestSourceCandidate> candidates = sourceMan.listCandidates(destPid);

        assertEquals(1, candidates.size());

        IngestSourceCandidate candidate = candidates.get(0);

        assertBagDetails(candidate, "testsource", "bag_with_files");
    }

    @Test
    public void testListCandidatesWithIntermediateDirectories() throws Exception {
        // Putting the bag for ingest into a subfolder of the source path
        Path subPath = Files.createDirectory(Paths.get(sourceFolderPath, "subPath"));
        addBagToSource(subPath.toString());

        Path configPath = createConfigFile(asList(
                new IngestSourceConfiguration("testsource",
                        "Source nested",
                        sourceFolderPath,
                        asList("*/*"),
                        asList(adminUnitPid))));
        initializeManager(configPath);

        List<IngestSourceCandidate> candidates = sourceMan.listCandidates(destPid);

        assertEquals(1, candidates.size());

        IngestSourceCandidate candidate = candidates.get(0);

        assertBagDetails(candidate, "testsource", "subPath/bag_with_files");
    }

    @Test
    public void testListCandidatesDirectory() throws Exception {
        Path candFolder = Files.createDirectory(Paths.get(sourceFolderPath, "ingestme"));
        File candFile = new File(candFolder.toString(), "content.txt");
        FileUtils.writeStringToFile(candFile, "data", "UTF-8");

        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

        List<IngestSourceCandidate> candidates = sourceMan.listCandidates(destPid);

        assertEquals(1, candidates.size());
        assertDirectoryDetails(candidates.get(0), "testsource", "ingestme");
    }

    @Test
    public void testListCandidatesDirectoryWithNestedDirs() throws Exception {
        Path candFolder = Files.createDirectory(Paths.get(sourceFolderPath, "ingestme"));
        Path candFolder2 = Files.createDirectory(Paths.get(candFolder.toString(), "f2"));
        Path candFolder3 = Files.createDirectory(Paths.get(candFolder2.toString(), "f3"));
        File candFile = new File(candFolder3.toString(), "content.txt");
        FileUtils.writeStringToFile(candFile, "data", "UTF-8");

        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

        List<IngestSourceCandidate> candidates = sourceMan.listCandidates(destPid);

        assertEquals(1, candidates.size());
        assertDirectoryDetails(candidates.get(0), "testsource", "ingestme");
    }

    @Test
    public void testListCandidatesNoCandidates() throws Exception {
        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

        List<IngestSourceCandidate> candidates = sourceMan.listCandidates(destPid);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void testListCandidatesFromMultipleSources() throws Exception {
        String source2FolderPath = tmpFolder.newFolder().getAbsolutePath();

        addBagToSource(source2FolderPath, "bag_with_files", "second_bag");
        addBagToSource(sourceFolderPath);

        IngestSourceConfiguration source2 = new IngestSourceConfiguration("testsource2",
                "Source 2",
                source2FolderPath,
                asList("*"),
                asList(destPid));
        Path configPath = createConfigFile(asList(
                source2,
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

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
        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", anotherUnit)));
        initializeManager(configPath);

        List<IngestSourceCandidate> candidates = sourceMan.listCandidates(destPid);

        assertTrue("No candidates expected", candidates.isEmpty());
    }

    @Test
    public void testListCandidatesMultipleFromSameSource() throws Exception {
        // Populate a directory candidate
        Path candFolder = Files.createDirectory(Paths.get(sourceFolderPath, "ingestme"));
        File candFile = new File(candFolder.toString(), "content.txt");
        FileUtils.writeStringToFile(candFile, "data", "UTF-8");

        // Populate a bag candidate in same ingest source
        addBagToSource(sourceFolderPath);

        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

        List<IngestSourceCandidate> candidates = sourceMan.listCandidates(destPid);

        assertEquals(2, candidates.size());

        IngestSourceCandidate candidate1 = findCandidateByPath("ingestme", candidates);
        assertDirectoryDetails(candidate1, "testsource", "ingestme");

        IngestSourceCandidate candidate2 = findCandidateByPath("bag_with_files", candidates);
        assertBagDetails(candidate2, "testsource", "bag_with_files");
    }

    @Test
    public void testIsPathValidNoSource() throws Exception {
        addBagToSource(sourceFolderPath);

        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

        assertFalse(sourceMan.isPathValid("bag_with_files", "wrongsourceid"));
    }

    @Test
    public void testIsPathValidCorrectPath() throws Exception {
        addBagToSource(sourceFolderPath);

        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

        assertTrue(sourceMan.isPathValid("bag_with_files", "testsource"));
    }

    @Test
    public void testIsPathValidNonexistentPath() throws Exception {
        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

        assertFalse(sourceMan.isPathValid("bag_with_files", "testsource"));
    }

    @Test
    public void testIsPathValidPathDoesNotMatchPattern() throws Exception {
        // Source only allows immediate children paths, but bag is in subfolder
        Path subPath = Files.createDirectory(Paths.get(sourceFolderPath, "subPath"));
        addBagToSource(subPath.toString());

        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

        assertFalse(sourceMan.isPathValid("subPath/bag_with_files", "testsource"));
    }

    @Test
    public void testIsPathValidRelativePath() throws Exception {
        addBagToSource(sourceFolderPath);

        Path configPath = createConfigFile(asList(
                createBasicConfig("testsource", destPid)));
        initializeManager(configPath);

        assertFalse(sourceMan.isPathValid("bag_with_files/..", "testsource"));
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private Path createConfigFile(List<IngestSourceConfiguration> configs) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Path configPath = Files.createTempFile("ingestSources", ".json");
        mapper.writeValue(configPath.toFile(), configs);
        return configPath;
    }

    private void mockAncestors(PID target, PID... ancestors) {
        when(contentPathFactory.getAncestorPids(target)).thenReturn(
                asList(ancestors));
    }

    private void addBagToSource(String ingestSourcePath) throws Exception {
        String bagName = "bag_with_files";
        addBagToSource(ingestSourcePath, bagName, bagName);
    }

    private void addBagToSource(String ingestSourcePath, String bagName, String destName) throws Exception {
        File original = new File("src/test/resources/ingestSources/" + bagName);
        FileUtils.copyDirectory(original, Paths.get(ingestSourcePath, destName).toFile());
    }

    private void assertContainsSource(List<IngestSourceConfiguration> sources, String sourceId) {
        assertTrue("Did not contain expected source " + sourceId,
                sources.stream().anyMatch(s -> s.getId().equals(sourceId)));
    }

    private IngestSourceConfiguration createBasicConfig(String id, PID... containers) {
        return new IngestSourceConfiguration(id,
                "Source " + id,
                sourceFolderPath,
                asList("*"),
                asList(containers));
    }

    private IngestSourceCandidate findCandidateByPath(String patternMatched, List<IngestSourceCandidate> candidates) {
        return candidates.stream().filter(c -> c.getPatternMatched().equals(patternMatched)).findFirst().get();
    }

    private void assertBagDetails(IngestSourceCandidate candidate, String sourceId, String patternMatched) {
        assertEquals(sourceId, candidate.getSourceId());
        assertEquals(PackagingType.BAGIT, candidate.getPackagingType());
        assertEquals("0.96", candidate.getVersion());
        assertEquals(15, candidate.getFileSize().longValue());
        assertEquals(3, candidate.getFileCount().intValue());
        assertEquals(patternMatched, candidate.getPatternMatched());
    }

    private void assertDirectoryDetails(IngestSourceCandidate candidate, String sourceId, String patternMatched) {
        assertEquals(sourceId, candidate.getSourceId());
        assertEquals(PackagingType.DIRECTORY, candidate.getPackagingType());
        assertEquals(patternMatched, candidate.getPatternMatched());
    }
}