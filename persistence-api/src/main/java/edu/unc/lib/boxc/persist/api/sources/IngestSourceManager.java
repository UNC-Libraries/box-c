package edu.unc.lib.boxc.persist.api.sources;

import java.net.URI;
import java.util.List;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.exceptions.UnknownIngestSourceException;

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