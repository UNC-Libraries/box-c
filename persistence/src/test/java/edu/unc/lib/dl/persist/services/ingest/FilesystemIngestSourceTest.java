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
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.assertBagDetails;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.assertDirectoryDetails;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.findCandidateByPath;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.dl.persist.services.storage.StorageType;

/**
 * @author bbpennel
 *
 */
public class FilesystemIngestSourceTest {

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private FilesystemIngestSource ingestSource;

    private Path sourceFolderPath;

    @Before
    public void setup() throws Exception {
        tmpFolder.create();
        sourceFolderPath = tmpFolder.newFolder().toPath();

        ingestSource = new FilesystemIngestSource();
    }

    @Test
    public void getStorageType() {
        assertEquals(StorageType.FILESYSTEM, ingestSource.getStorageType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void baseFieldWithoutScheme() {
        ingestSource.setBase(sourceFolderPath.toAbsolutePath().toString());
    }

    @Test
    public void baseFieldFromUri() {
        ingestSource.setBase(sourceFolderPath.toUri().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void baseFieldFromNonFileUri() {
        ingestSource.setBase("http://example.com/stuff");
    }

    @Test
    public void idField() {
        ingestSource.setId("my_id");
        assertEquals("my_id", ingestSource.getId());
    }

    @Test
    public void nameField() {
        ingestSource.setName("a name");
        assertEquals("a name", ingestSource.getName());
    }

    @Test
    public void readOnlyField() {
        ingestSource.setReadOnly(true);
        assertTrue(ingestSource.isReadOnly());
    }

    @Test
    public void isValidUriNotWithinBase() throws Exception {
        ingestSource.setBase(sourceFolderPath.toUri().toString());

        Path otherSourcePath = tmpFolder.newFolder().toPath();

        assertFalse(ingestSource.isValidUri(otherSourcePath.toUri()));
    }

    @Test
    public void isValidUriDoesNotExist() throws Exception {
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*"));

        URI testUri = Paths.get(sourceFolderPath.toString(), "someSubPath").toUri();

        assertFalse(ingestSource.isValidUri(testUri));
    }

    @Test
    public void isValidUriDoesNotMatchPattern() throws Exception {
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*/*"));

        Path path = Paths.get(sourceFolderPath.toString(), "someSubPath");
        Files.createDirectory(path);

        assertFalse(ingestSource.isValidUri(path.toUri()));
    }

    @Test
    public void isValidUriMatchesImmediateChildPattern() throws Exception {
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*"));

        Path path = Paths.get(sourceFolderPath.toString(), "someSubPath");
        Files.createDirectory(path);

        assertTrue(ingestSource.isValidUri(path.toUri()));
    }

    @Test
    public void isValidUriMatchesNestedPattern() throws Exception {
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*/*"));

        Path path = Paths.get(sourceFolderPath.toString(), "someSubPath/nestedPath");
        Files.createDirectories(path);

        assertTrue(ingestSource.isValidUri(path.toUri()));
    }

    @Test
    public void isValidUriDoesNotMatchRelative() throws Exception {
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*/*"));

        Path realPath = Paths.get(sourceFolderPath.toString(), "someSubPath");
        Files.createDirectory(realPath);
        Path path = Paths.get(sourceFolderPath.toString(), "someSubPath/..");

        assertFalse(ingestSource.isValidUri(path.toUri()));
    }

    @Test
    public void isValidUriNonFileUri() throws Exception {
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*"));

        URI uri = URI.create("http://example.com" + sourceFolderPath);

        assertFalse(ingestSource.isValidUri(uri));
    }

    @Test
    public void listCandidatesBag() throws Exception {
        ingestSource.setId("testsource");
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*"));

        addBagToSource(sourceFolderPath);

        List<IngestSourceCandidate> candidates = ingestSource.listCandidates();
        assertEquals(1, candidates.size());

        IngestSourceCandidate candidate = candidates.get(0);
        assertBagDetails(candidate, "testsource", "bag_with_files");
    }

    @Test
    public void listCandidatesBagWithIntermediateDirectories() throws Exception {
        ingestSource.setId("testsource");
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*/*"));

        Path subPath = Files.createDirectory(sourceFolderPath.resolve("subPath"));
        addBagToSource(subPath);

        List<IngestSourceCandidate> candidates = ingestSource.listCandidates();
        assertEquals(1, candidates.size());

        IngestSourceCandidate candidate = candidates.get(0);
        assertBagDetails(candidate, "testsource", "subPath/bag_with_files");
    }

    @Test
    public void listCandidatesDirectory() throws Exception {
        ingestSource.setId("testsource");
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*"));

        Path candFolder = Files.createDirectory(sourceFolderPath.resolve("ingestme"));
        File candFile = new File(candFolder.toString(), "content.txt");
        FileUtils.writeStringToFile(candFile, "data", "UTF-8");

        List<IngestSourceCandidate> candidates = ingestSource.listCandidates();
        assertEquals(1, candidates.size());
        assertDirectoryDetails(candidates.get(0), "testsource", "ingestme");
    }

    @Test
    public void listCandidatesDirectoryWithChildDirs() throws Exception {
        ingestSource.setId("testsource");
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*"));

        Path candFolder = Files.createDirectory(sourceFolderPath.resolve("ingestme"));
        Path candFolder2 = Files.createDirectory(candFolder.resolve("f2"));
        Path candFolder3 = Files.createDirectory(candFolder2.resolve("f3"));
        File candFile = new File(candFolder3.toString(), "content.txt");
        FileUtils.writeStringToFile(candFile, "data", "UTF-8");

        List<IngestSourceCandidate> candidates = ingestSource.listCandidates();
        assertEquals(1, candidates.size());
        assertDirectoryDetails(candidates.get(0), "testsource", "ingestme");
    }

    @Test
    public void listCandidatesEmptySource() throws Exception {
        ingestSource.setId("testsource");
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*"));

        List<IngestSourceCandidate> candidates = ingestSource.listCandidates();
        assertEquals(0, candidates.size());
    }

    @Test
    public void listCandidatesNoMatches() throws Exception {
        ingestSource.setId("testsource");
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*/*"));

        // directory will not match the pattern
        Path candFolder = Files.createDirectory(sourceFolderPath.resolve("ingestme"));
        File candFile = new File(candFolder.toString(), "content.txt");
        FileUtils.writeStringToFile(candFile, "data", "UTF-8");

        List<IngestSourceCandidate> candidates = ingestSource.listCandidates();
        assertEquals(0, candidates.size());
    }

    @Test
    public void listCandidatesMultipleCandidates() throws Exception {
        ingestSource.setId("testsource");
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*"));

        addBagToSource(sourceFolderPath);

        // directory will not match the pattern
        Path candFolder = Files.createDirectory(sourceFolderPath.resolve("ingestme"));
        File candFile = new File(candFolder.toString(), "content.txt");
        FileUtils.writeStringToFile(candFile, "data", "UTF-8");

        List<IngestSourceCandidate> candidates = ingestSource.listCandidates();
        assertEquals(2, candidates.size());

        IngestSourceCandidate candidate1 = findCandidateByPath("ingestme", candidates);
        assertDirectoryDetails(candidate1, "testsource", "ingestme");

        IngestSourceCandidate candidate2 = findCandidateByPath("bag_with_files", candidates);
        assertBagDetails(candidate2, "testsource", "bag_with_files");
    }

    @Test
    public void resolveRelativePathBag() throws Exception {
        ingestSource.setId("testsource");
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*"));

        addBagToSource(sourceFolderPath);

        URI candUri = ingestSource.resolveRelativePath("bag_with_files");
        assertEquals(sourceFolderPath.resolve("bag_with_files").toUri(), candUri);
    }

    @Test(expected = InvalidIngestSourceCandidateException.class)
    public void resolveRelativePathOutsideOfSource() throws Exception {
        ingestSource.setId("testsource");
        ingestSource.setBase(sourceFolderPath.toUri().toString());
        ingestSource.setPatterns(asList("*"));

        addBagToSource(sourceFolderPath);

        ingestSource.resolveRelativePath("../../bag_with_files");
    }
}
