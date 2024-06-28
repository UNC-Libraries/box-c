package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter which sets a file's streaming URL. Updates the streamingUrl field.
 *
 * @author snluong
 */
public class SetStreamingPropertiesFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetStreamingPropertiesFilter.class);

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing SetStreamingProperties for object {}", dip.getPid());
        var resource = dip.getContentObject().getResource();
        var doc = dip.getDocument();
        var url = resource.hasProperty(Cdr.streamingUrl) ?
                resource.getProperty(Cdr.streamingUrl).getString() : null;

        String streamingType = null;
        if (url != null) {
            streamingType = resource.hasProperty(Cdr.streamingType) ?
                    resource.getProperty(Cdr.streamingType).getString() : null;
        }

        doc.setStreamingUrl(url);
        doc.setStreamingType(streamingType);
    }
}
