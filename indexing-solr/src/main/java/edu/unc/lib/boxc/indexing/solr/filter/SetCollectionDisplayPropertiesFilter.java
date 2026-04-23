package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetCollectionDisplayPropertiesFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetCollectionDisplayPropertiesFilter.class);

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing SetCollectionDisplayProperties for object {}", dip.getPid());
        var resource = dip.getContentObject().getResource();
        var doc = dip.getDocument();
        var displaySettings = resource.hasProperty(Cdr.collectionDefaultDisplaySettings) ?
                resource.getProperty(Cdr.collectionDefaultDisplaySettings).getString() : null;

        doc.setCollectionDisplaySettings(displaySettings);
    }
}
