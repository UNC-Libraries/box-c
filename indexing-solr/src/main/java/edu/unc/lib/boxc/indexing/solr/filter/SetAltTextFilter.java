package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Filter which populates alt text for the object being indexed
 *
 * @author bbpennel
 */
public class SetAltTextFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetAltTextFilter.class);

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        ContentObject contentObj = dip.getContentObject();
        // object being indexed must be a file object
        if (!(contentObj instanceof FileObject)) {
            return;
        }
        var fileObj = (FileObject) contentObj;

        try {
            var altTextBinary = fileObj.getBinaryObject(DatastreamType.ALT_TEXT.getId());
            var altText = IOUtils.toString(altTextBinary.getBinaryStream(), UTF_8);
            dip.getDocument().setAltText(altText);
        } catch (NotFoundException e) {
            log.debug("No alt text datastream found for {}", dip.getPid());
        } catch (IOException e) {
            throw new IndexingException("Failed to retrieve alt text datastream for {}" + dip.getPid(), e);
        }
    }
}
