package edu.unc.lib.boxc.indexing.solr.filter;

import java.text.ParseException;

import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;

/**
 * Indexing filter that extracts Fedora-generated dates about the creation and modification state of an object
 * being indexed.
 *
 * Sets: dateAdded, dateUpdated
 * @author bbpennel, harring
 *
 */
public class SetRecordDatesFilter implements IndexDocumentFilter {
    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        ContentObject obj = dip.getContentObject();
        Resource resc = obj.getResource();
        String dateAdded = resc.getProperty(Fcrepo4Repository.created).getLiteral().getValue().toString();
        String dateUpdated = resc.getProperty(Fcrepo4Repository.lastModified).getLiteral().getValue().toString();
        try {
            dip.getDocument().setDateAdded(dateAdded);
            dip.getDocument().setDateUpdated(dateUpdated);
        } catch (IllegalArgumentException | ParseException e) {
            throw new IndexingException("Failed to parse record dates from " + dip.getPid(), e);
        }
    }
}
