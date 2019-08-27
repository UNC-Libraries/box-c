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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.util.PackagingType;
import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.reader.BagReader;

/**
 * Loads and manages ingest sources, which are preconfigured locations to find packages for deposit.
 *
 * @author bbpennel
 * @date Oct 22, 2015
 */
public class IngestSourceManager {
    private static final Logger log = LoggerFactory.getLogger(IngestSourceManager.class);

    private List<IngestSourceConfiguration> configs;

    private String configPath;

    private BagReader reader = new BagReader();

    /**
     * Initialize the manager, loading configuration and setting up watcher for config changes.
     *
     * @throws IOException
     */
    public void init() throws IOException {
        IngestSourceConfigWatcher configWatcher = new IngestSourceConfigWatcher(configPath, this);
        configWatcher.loadConfig();

        // Start separate thread for reloading configuration when it changes
        Thread watchThread = new Thread(configWatcher);
        watchThread.start();
    }

    /**
     * Retrieves a list of ingest sources which contain or match the destination object provided.
     *
     * @param destination
     * @return
     */
//    public List<IngestSourceConfiguration> listSources(PID destination) {
//        List<PID> ancestors = tripleService.lookupAllContainersAbove(destination);
//
//        // Determine which sources apply to the selected destination
//        List<IngestSourceConfiguration> applicableSources = new ArrayList<>();
//        for (IngestSourceConfiguration source : configs) {
//            for (String container : source.getContainers()) {
//                PID containerPID = new PID(container);
//                if (containerPID.equals(destination) || ancestors.contains(containerPID)) {
//                    applicableSources.add(source);
//                    continue;
//                }
//            }
//        }
//
//        return applicableSources;
//    }

    /**
     * Retrieves a list of candidate file information for ingestable packages from sources which are
     * applicable to the destination provided.
     *
     * @param destination
     * @return
     */
//    public List<Map<String, Object>> listCandidates(PID destination) {
//
//        List<IngestSourceConfiguration> applicableSources = listSources(destination);
//
//        final List<Map<String, Object>> candidates = new ArrayList<>();
//        for (final IngestSourceConfiguration source : applicableSources) {
//            final String base = source.getBase();
//
//            // Gathering candidates per pattern within a particular base directory
//            for (String pattern : source.getPatterns()) {
//                final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + base + pattern);
//                try {
//                    Files.walkFileTree(Paths.get(base), new SimpleFileVisitor<Path>() {
//                        @Override
//                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
//                                throws IOException {
//                            if (matcher.matches(dir)) {
//                                log.debug("Matched dir {} for source {}", dir, source.getId());
//                                addCandidate(candidates, dir, source, base);
//                                return FileVisitResult.SKIP_SUBTREE;
//                            }
//                            return FileVisitResult.CONTINUE;
//                        }
//
//                        @Override
//                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                            if (matcher.matches(file)) {
//                                log.debug("Matched file {} for source {}", file, source.getId());
//                                addCandidate(candidates, file, source, base);
//                            }
//                            return FileVisitResult.CONTINUE;
//                        }
//                    });
//                } catch (IOException e) {
//                    log.error("Failed to gather candidate files for source {}", source.getId(), e);
//                }
//            }
//        }
//
//        return candidates;
//    }

    /**
     * Adds information about applicable packages to the list of candidates
     *
     * @param candidates
     * @param filePath
     * @param source
     * @param base
     * @throws IOException
     */
    private void addCandidate(List<Map<String, Object>> candidates, Path filePath,
            IngestSourceConfiguration source, String base) throws IOException {

        File file = filePath.toFile();
        if (!file.isDirectory()) {
            return;
        }

        // Only directory bags are candidates currently
        try {
            Bag bagReader = reader.read(filePath);
            String version = bagReader.getVersion().toString();

            Map<String, Object> candidate = new HashMap<>();

            candidate.put("sourceId", source.getId());
            candidate.put("base", base);
            candidate.put("patternMatched", Paths.get(base).relativize(filePath).toString());

            candidate.put("version", version);

            if (version != null) {
                // Add payload stats for bags
                addBagInfo(candidate, filePath);
            } else if (file.isDirectory()) {
                candidate.put("packagingType", PackagingType.DIRECTORY.getUri());
            } else {
                // Add stats for a non-bag zip file
                if (file.getName().endsWith(".zip")) {
                    try (ZipFile zip = new ZipFile(file)) {
                        candidate.put("files", zip.size());
                    }
                }
                candidate.put("size", file.length());
            }

            candidates.add(candidate);
        } catch (UnsupportedAlgorithmException | InvalidBagitFileFormatException |
                UnparsableVersionException | MaliciousPathException e) {
            log.warn("Unable to add bag candidate. {}", e.getMessage());
        }
    }

    private void addBagInfo(Map<String, Object> fileInfo, Path filePath) {
        StandardSupportedAlgorithms algorithm = StandardSupportedAlgorithms.MD5;

        try {
            Bag bagFile = BagCreator.bagInPlace(filePath, Arrays.asList(algorithm), true);

            fileInfo.put("files", bagFile.getItemsToFetch().size());
            long size = 0;
            Iterator<FetchItem> bagIt = bagFile.getItemsToFetch().iterator();
            while (bagIt.hasNext()) {
                Path path = bagIt.next().getPath();
                size += Files.size(path);
            }

            fileInfo.put("size", size);

            fileInfo.put("packagingType", PackagingType.BAGIT.getUri());

        } catch (NoSuchAlgorithmException | IOException e) {
            log.warn("Unable to create bag. {}", e.getMessage());
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
        for (IngestSourceConfiguration source : configs) {
            if (source.getId().equals(id)) {
                return source;
            }
        }
        return null;
    }

    public void setConfigs(List<IngestSourceConfiguration> configs) {
        this.configs = configs;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
}