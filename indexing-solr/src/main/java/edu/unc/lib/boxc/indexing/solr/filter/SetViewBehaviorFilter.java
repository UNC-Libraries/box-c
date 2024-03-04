package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.rdf.CdrView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetViewBehaviorFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetViewBehaviorFilter.class);

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing SetViewBehavior for object {}", dip.getPid());
        var resource = dip.getContentObject().getResource();
        var doc = dip.getDocument();
        doc.setViewBehavior(resource.getProperty(CdrView.viewBehavior).getString());
    }
}
