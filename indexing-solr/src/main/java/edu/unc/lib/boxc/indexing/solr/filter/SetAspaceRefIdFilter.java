package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrAspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets the ArchivesSpace Ref ID tag for WorkObjects
 * @author snluong
 */
public class SetAspaceRefIdFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetAspaceRefIdFilter.class);

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        var contentObject = dip.getContentObject();

        if (!(contentObject instanceof WorkObject)) {
            return;
        }

        log.debug("Performing SetAspaceRefId for object {}", dip.getPid());
        var resource = contentObject.getResource();
        var doc = dip.getDocument();

        var refId = resource.hasProperty(CdrAspace.refId) ?
                resource.getProperty(CdrAspace.refId).getString() : null;

        List<String> identifiers = new ArrayList<>();
        if (!doc.getIdentifier().isEmpty() && doc.getIdentifier() != null) {
            identifiers = doc.getIdentifier();
        }
        identifiers.add("aspaceRefId|" + refId);

        doc.setAspaceRefId(refId);
        doc.setIdentifier(identifiers);
    }
}
