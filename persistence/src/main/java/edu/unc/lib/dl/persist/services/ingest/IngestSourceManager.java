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

import java.io.IOException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.exceptions.OrphanedObjectException;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PackagingType;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.reader.BagReader;

/**
 * Loads and manages ingest sources, which are preconfigured locations to find packages for deposit.
 *
 * @author bbpennel
 */
public class IngestSourceManager {
    private static final Logger log = LoggerFactory.getLogger(IngestSourceManager.class);

    private List<IngestSourceConfiguration> configs;
    private Map<PID, List<IngestSourceConfiguration>> configMappings;

    private String configPath;

    private ContentPathFactory contentPathFactory;

    private BagReader reader = new BagReader();

    private boolean watchForChanges = true;

    /**
     * Initialize the manager, loading configuration and setting up watcher for config changes.
     *
     * @throws IOException
     */
    public void init() throws IOException {
        IngestSourceConfigWatcher configWatcher = new IngestSourceConfigWatcher(configPath, this);
        configWatcher.loadConfig();

        if (watchForChanges) {
            // Start separate thread for reloading configuration when it changes
            Thread watchThread = new Thread(configWatcher);
            watchThread.start();
        }
    }

    /**
     * Retrieves a list of ingest sources which contain or match the destination object provided.
     *
     * @param target
     * @return
     */
    public List<IngestSourceConfiguration> listSources(PID target) {
        List<PID> pathPids = new ArrayList<>(contentPathFactory.getAncestorPids(target));
        if (pathPids.isEmpty()) {
            throw new OrphanedObjectException("Cannot look up sources, no ancestors were found for "
                    + target.getId());
        }
        // Add in the target in case config is mapped to it directly.
        pathPids.add(target);

        // Build a list of all source configs for any objects in the target path
        return pathPids.stream()
                .filter(aPid -> configMappings.containsKey(aPid))
                .flatMap(aPid -> configMappings.get(aPid).stream())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of candidate file information for ingestable packages from sources which are
     * applicable to the destination provided.
     *
     * @param destination
     * @return
     */
    public List<IngestSourceCandidate> listCandidates(PID destination) {
        List<IngestSourceConfiguration> applicableSources = listSources(destination);

        final List<IngestSourceCandidate> candidates = new ArrayList<>();
        for (final IngestSourceConfiguration source : applicableSources) {
            final String base = source.getBase();

            // Gathering candidates per pattern within a particular base directory
            for (String pattern : source.getPatterns()) {
                final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + base + pattern);
                try {
                    Files.walkFileTree(Paths.get(base), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                                throws IOException {
                            if (matcher.matches(dir)) {
                                log.debug("Matched dir {} for source {}", dir, source.getId());
                                addCandidate(candidates, dir, source, base);
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    log.error("Failed to gather candidate files for source {}", source.getId(), e);
                }
            }
        }

        return candidates;
    }

    /**
     * Adds information about applicable packages to the list of candidates
     *
     * @param candidates
     * @param filePath
     * @param source
     * @param base
     * @throws IOException
     */
    private void addCandidate(List<IngestSourceCandidate> candidates, Path filePath,
            IngestSourceConfiguration source, String base) throws IOException {

        // Only directory bags are candidates currently
        try {
            long start = System.nanoTime();
            Bag bagReader = getBagitBag(filePath);
            log.debug("Time spent inspecting candidate as bag: {}ns", (System.nanoTime() - start));

            IngestSourceCandidate candidate = new IngestSourceCandidate();

            candidate.setSourceId(source.getId());
            candidate.setBase(base);
            candidate.setPatternMatched(Paths.get(base).relativize(filePath).toString());

            // If bag reader was able to parse, then process as a bag
            if (bagReader != null) {
                addBagInfo(candidate, bagReader);
            } else {
                candidate.setPackagingType(PackagingType.DIRECTORY);
            }

            candidates.add(candidate);
        } catch (UnsupportedAlgorithmException | InvalidBagitFileFormatException |
                UnparsableVersionException | MaliciousPathException e) {
            log.warn("Unable to add bag candidate. {}", e.getMessage());
        }
    }

    private void addBagInfo(IngestSourceCandidate candidate, Bag bagReader) {
        candidate.setPackagingType(PackagingType.BAGIT);
        candidate.setVersion(bagReader.getVersion().toString());

        // Retrieve the Oxum for the bag, which contains the total payload bytes and number of files
        List<String> oxums = bagReader.getMetadata().get("Payload-Oxum");
        if (oxums != null && !oxums.isEmpty()) {
            String[] oxumParts = oxums.get(0).split("\\.");
            candidate.setFileSize(new Long(oxumParts[0]));
            candidate.setFileCount(new Integer(oxumParts[1]));
        }
    }

    /**
     * Returns true if the given path is from valid for the given source and present.
     *
     * @param pathString
     * @param sourceId
     * @return
     */
    public boolean isPathValid(String pathString, String sourceId) {
        IngestSourceConfiguration source = getSourceConfiguration(sourceId);
        if (source == null) {
            return false;
        }

        Path path = Paths.get(source.getBase(), pathString);
        if (!isPathValidForSource(path, source)) {
            return false;
        }

        return path.toFile().exists();
    }

    /**
     * Returns true if the given path matches any of the patterns specified for the given source
     *
     * @param path
     * @param source
     * @return
     */
    private boolean isPathValidForSource(Path path, IngestSourceConfiguration source) {
        for (String pattern : source.getPatterns()) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + source.getBase() + pattern);
            if (matcher.matches(path)) {
                return true;
            }
        }

        return false;
    }

    public IngestSourceConfiguration getSourceConfiguration(String id) {
        return configs.stream().filter(c -> c.getId().equals(id))
                .findFirst().orElse(null);
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

    public void setConfigs(List<IngestSourceConfiguration> configs) {
        this.configs = configs;
        // Group configurations by container pids they map to
        this.configMappings = new HashMap<>();
        configs.forEach(config -> {
            for (String container: config.getContainers()) {
                PID containerPid = PIDs.get(container);
                List<IngestSourceConfiguration> mapped = configMappings.getOrDefault(containerPid, new ArrayList<>());
                mapped.add(config);
                configMappings.putIfAbsent(containerPid, mapped);
            }
        });
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    /**
     * @param contentPathFactory the contentPathFactory to set
     */
    public void setContentPathFactory(ContentPathFactory contentPathFactory) {
        this.contentPathFactory = contentPathFactory;
    }

    /**
     * @param watchForChanges the watchForChanges to set
     */
    public void setWatchForChanges(boolean watchForChanges) {
        this.watchForChanges = watchForChanges;
    }
}