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
package edu.unc.lib.boxc.persist.impl.sources;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.persist.api.PackagingType;
import edu.unc.lib.boxc.persist.api.exceptions.InvalidIngestSourceCandidateException;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceCandidate;
import edu.unc.lib.boxc.persist.api.storage.StorageType;
import edu.unc.lib.dl.fedora.ServiceException;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.reader.BagReader;

/**
 * A filesystem based staging location from which objects may be ingested into the repository.
 *
 * @author bbpennel
 *
 */
public class FilesystemIngestSource implements IngestSource {
    private static final Logger log = LoggerFactory.getLogger(FilesystemIngestSource.class);

    private String id;
    private String name;
    private Path basePath;
    private List<String> patterns;
    private boolean readOnly;
    private boolean internal;

    private BagReader reader = new BagReader();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public boolean isInternal() {
        return internal;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.FILESYSTEM;
    }

    @Override
    public String getType() {
        return StorageType.FILESYSTEM.getId();
    }

    public void setBase(String base) {
        URI baseUri = URI.create(base);
        if (baseUri.getScheme() == null || !baseUri.getScheme().equals("file")) {
            throw new IllegalArgumentException("Must specify a file URI for ingest source, but encountered: " + base);
        }
        this.basePath = Paths.get(baseUri);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * @param internal set whether this source is for internal usage
     */
    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    /**
     * A URI is valid if it is a file uri, it matches the base and pattern for the location,
     * and references a filepath that exists
     */
    @Override
    public boolean isValidUri(URI uri) {
        if (!uri.getScheme().equals("file")) {
            return false;
        }

        // normalize to evaluate any modifiers
        Path path = Paths.get(uri).normalize();
        return isValidPath(path);
    }

    private boolean isValidPath(Path path) {
        // Check that the incoming path is contained by this base for this source
        if (!path.startsWith(basePath)) {
            return false;
        }

        String baseString = basePath.toAbsolutePath().toString();
        for (String pattern : patterns) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + baseString + "/" + pattern + "*");
            if (matcher.matches(path)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean exists(URI uri) {
        Path path = Paths.get(uri).normalize();
        if (!path.startsWith(basePath)) {
            return false;
        }

        return Files.exists(path);
    }

    @Override
    public List<IngestSourceCandidate> listCandidates() {
        List<IngestSourceCandidate> candidates = new ArrayList<>();

        String baseString = basePath.toAbsolutePath().toString();
        // Gathering candidates per pattern within a particular base directory
        for (String pattern : patterns) {
            final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + baseString + "/" + pattern);
            try {
                Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        if (matcher.matches(dir)) {
                            log.debug("Matched dir {} for source {}", dir, id);
                            IngestSourceCandidate candidate = createCandidate(dir);
                            if (candidate != null) {
                                candidates.add(candidate);
                            }
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new ServiceException("Failed to gather candidate files for source " + id, e);
            }
        }
        return candidates;
    }

    /**
     * Builds a candidate object from the supplied path
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    private IngestSourceCandidate createCandidate(Path filePath) throws IOException {
     // Only directory bags are candidates currently
        try {
            long start = System.nanoTime();
            Bag bagReader = getBagitBag(filePath);
            log.debug("Time spent inspecting candidate as bag: {}ns", (System.nanoTime() - start));

            IngestSourceCandidate candidate = new IngestSourceCandidate();

            candidate.setSourceId(id);
            candidate.setPatternMatched(basePath.relativize(filePath).toString());

            // If bag reader was able to parse, then process as a bag
            if (bagReader != null) {
                addBagInfo(candidate, bagReader);
            } else {
                candidate.setPackagingType(PackagingType.DIRECTORY);
            }

            return candidate;
        } catch (UnsupportedAlgorithmException | InvalidBagitFileFormatException |
                UnparsableVersionException | MaliciousPathException e) {
            log.warn("Unable to add bag candidate. {}", e.getMessage());
        }

        return null;
    }

    private void addBagInfo(IngestSourceCandidate candidate, Bag bagReader) {
        candidate.setPackagingType(PackagingType.BAGIT);
        candidate.setVersion(bagReader.getVersion().toString());

        // Retrieve the Oxum for the bag, which contains the total payload bytes and number of files
        List<String> oxums = bagReader.getMetadata().get("Payload-Oxum");
        if (oxums != null && !oxums.isEmpty()) {
            String[] oxumParts = oxums.get(0).split("\\.");
            candidate.setFileSize(Long.valueOf(oxumParts[0]));
            candidate.setFileCount(Integer.valueOf(oxumParts[1]));
        }
    }

    private Bag getBagitBag(Path filePath) throws IOException, UnparsableVersionException,
            MaliciousPathException, UnsupportedAlgorithmException, InvalidBagitFileFormatException {
        try {
            return reader.read(filePath);
        } catch (FileSystemException e) {
            log.debug("Candidate {} was not a bag", filePath);
        }
        return null;
    }

    @Override
    public URI resolveRelativePath(String relative) {
        Path candidatePath = basePath.resolve(relative).normalize();

        if (!isValidPath(candidatePath) || Files.notExists(candidatePath)) {
            throw new InvalidIngestSourceCandidateException("Invalid ingest source path " + relative);
        }

        return candidatePath.toUri();
    }
}
