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

import static edu.unc.lib.boxc.persist.api.storage.StorageType.FILESYSTEM;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;

import edu.unc.lib.boxc.model.api.exceptions.OrphanedObjectException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.api.exceptions.UnknownIngestSourceException;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceCandidate;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceManager;

/**
 * Loads and manages ingest sources, which are preconfigured locations to find packages for deposit.
 *
 * @author bbpennel
 */
public class IngestSourceManagerImpl implements IngestSourceManager {
    private static final Logger log = LoggerFactory.getLogger(IngestSourceManagerImpl.class);

    private Map<PID, List<IngestSource>> pidToSources;
    private Map<String, IngestSource> idToSource;
    private List<IngestSource> ingestSources;

    private String configPath;
    private String mappingPath;

    private ContentPathFactory contentPathFactory;

    /**
     * Initialize the manager, loading configuration and setting up watcher for config changes.
     *
     * @throws IOException
     */
    public void init() throws IOException {
        deserializeConfig();
        deserializeMapping();
    }

    private void deserializeConfig() throws IOException {
        InputStream configStream = new FileInputStream(new File(configPath));
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(
                new NamedType(FilesystemIngestSource.class, FILESYSTEM.getId()));
        ingestSources = mapper.readValue(configStream,
                new TypeReference<List<IngestSource>>() {});
        idToSource = ingestSources.stream()
                .collect(Collectors.toMap(IngestSource::getId, sl -> sl));
    }

    private void deserializeMapping() throws IOException {
        pidToSources = new HashMap<>();

        InputStream mappingStream = new FileInputStream(new File(mappingPath));
        ObjectMapper mapper = new ObjectMapper();
        List<IngestSourceMapping> mappingList = mapper.readValue(mappingStream,
                new TypeReference<List<IngestSourceMapping>>() {});

        for (IngestSourceMapping mapping: mappingList) {
            PID pid = PIDs.get(mapping.getId());
            List<String> sources = mapping.getSources();

            List<IngestSource> mappedSources = sources.stream()
                    .map(s -> idToSource.get(s))
                    .collect(Collectors.toList());

            if (mappedSources.contains(null)) {
                throw new UnknownIngestSourceException("Mapping for " + pid.getId()
                        + " refers to one or more unknown ingest source: " + sources);
            }

            if (pidToSources.containsKey(pid)) {
                throw new IllegalStateException("Duplicate container key " + pid.getId());
            }

            pidToSources.put(pid, mappedSources);
        }
    }

    /**
     * Retrieves a list of ingest sources which contain or match the destination object provided.
     *
     * @param target
     * @return
     */
    @Override
    public List<IngestSource> listSources(PID target) {
        log.debug("Listing all ingest sources for {}", target.getId());
        List<PID> pathPids = new ArrayList<>(contentPathFactory.getAncestorPids(target));
        if (pathPids.isEmpty()) {
            throw new OrphanedObjectException("Cannot look up sources, no ancestors were found for "
                    + target.getId());
        }
        // Add in the target in case config is mapped to it directly.
        pathPids.add(target);

        // Build a list of all source configs for any objects in the target path
        return pathPids.stream()
                .filter(aPid -> pidToSources.containsKey(aPid))
                .flatMap(aPid -> pidToSources.get(aPid).stream())
                .filter(source -> !source.isInternal())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of candidate file information for ingestable packages from sources which are
     * applicable to the destination provided.
     *
     * @param destination
     * @return
     */
    @Override
    public List<IngestSourceCandidate> listCandidates(PID destination) {
        List<IngestSource> applicableSources = listSources(destination);

        final List<IngestSourceCandidate> candidates = new ArrayList<>();
        for (final IngestSource source : applicableSources) {
            candidates.addAll(source.listCandidates());
        }

        return candidates;
    }

    @Override
    public IngestSource getIngestSourceById(String id) {
        return idToSource.get(id);
    }

    @Override
    public IngestSource getIngestSourceForUri(URI uri) {
        for (IngestSource source: ingestSources) {
            if (source.isValidUri(uri)) {
                return source;
            }
        }
        throw new UnknownIngestSourceException("No ingest sources match URI " + uri);
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public void setMappingPath(String mappingPath) {
        this.mappingPath = mappingPath;
    }

    /**
     * @param contentPathFactory the contentPathFactory to set
     */
    public void setContentPathFactory(ContentPathFactory contentPathFactory) {
        this.contentPathFactory = contentPathFactory;
    }

    public static class IngestSourceMapping {
        private String id;
        private List<String> sources;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<String> getSources() {
            return sources;
        }

        public void setSources(List<String> sources) {
            this.sources = sources;
        }
    }
}