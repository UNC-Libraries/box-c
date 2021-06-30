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
package edu.unc.lib.dl.persist.api.ingest;

import java.net.URI;
import java.util.List;

import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Manager for retrieving and querying ingest sources
 *
 * @author bbpennel
 *
 */
public interface IngestSourceManager {

    /**
     * Retrieves a list of ingest sources which contain or match the destination object provided.
     *
     * @param destination
     * @return
     */
    List<IngestSource> listSources(PID destination);

    /**
     * Retrieves a list of candidate file information for ingestable packages from sources which are
     * applicable to the destination provided.
     *
     * @param destination
     * @return
     */
    List<IngestSourceCandidate> listCandidates(PID destination);

    /**
     * Get the ingest source with the provided id
     *
     * @param id
     * @return matching ingest source, or null if not found.
     */
    IngestSource getIngestSourceById(String id);

    /**
     * Return the ingest source that contains the provided URI
     *
     * @param uri
     * @return Ingest source containing the URI
     * @throws UnknownIngestSourceException thrown if no ingest source matches the given URI
     */
    IngestSource getIngestSourceForUri(URI uri);
}