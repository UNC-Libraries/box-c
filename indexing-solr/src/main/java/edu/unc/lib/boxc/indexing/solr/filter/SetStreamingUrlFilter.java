package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetStreamingUrlFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetStreamingUrlFilter.class);

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing SetStreamingUrl for object {}", dip.getPid());
        var resource = dip.getContentObject().getResource();
        var doc = dip.getDocument();
        var url = resource.hasProperty(Cdr.streamingUrl) ?
                resource.getProperty(Cdr.streamingUrl).getString() : null ;

        doc.setStreamingUrl(url);
    }
}
