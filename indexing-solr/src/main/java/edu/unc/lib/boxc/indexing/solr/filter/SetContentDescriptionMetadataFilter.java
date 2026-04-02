package edu.unc.lib.boxc.indexing.solr.filter;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.utils.MachineGenerateContentService;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author bbpennel
 */
public class SetContentDescriptionMetadataFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetContentDescriptionMetadataFilter.class);
    private RepositoryObjectLoader repositoryObjectLoader;
    private MachineGenerateContentService mgContentService;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        ContentObject contentObj = dip.getContentObject();
        // object being indexed must be a file object
        if (!(contentObj instanceof FileObject)) {
            return;
        }
        PID filePid = contentObj.getPid();
        IndexDocumentBean idb = dip.getDocument();


        String mgdString = getMachineGeneratedDescriptionJson(filePid);
        JsonNode mgdNode = mgContentService.deserializeMachineGeneratedDescription(mgdString);

        // Store the raw string in the index for reviewing
        idb.setMgDescription(mgdString);
        idb.setAltText(getAltText(filePid, mgdNode));
        idb.setFullDescription(getFullDescription(filePid, mgdNode));
        idb.setTranscript(getTranscript(filePid, mgdNode));
        idb.setMgRiskScore(mgContentService.extractRiskScore(mgdNode));
    }

    private String getMachineGeneratedDescriptionJson(PID filePid) throws IndexingException {
        try {
            return mgContentService.loadMachineGeneratedDescription(filePid);
        } catch (FileNotFoundException e) {
            log.debug("No machine generated description datastream found for {}", filePid);
            return null;
        } catch (IOException e) {
            throw new IndexingException("Failed to read machine generated description for " + filePid, e);
        }
    }

    private String getAltText(PID filePid, JsonNode mgdNode) throws IndexingException {
        // Preferentially use the alt text stored in fedora if it exists
        try {
            var altTextPid = DatastreamPids.getAltTextPid(filePid);
            var altTextBinary = repositoryObjectLoader.getBinaryObject(altTextPid);
            return IOUtils.toString(altTextBinary.getBinaryStream(), UTF_8);
        } catch (NotFoundException e) {
            log.debug("No alt text datastream found for {}", filePid);
        } catch (IOException e) {
            throw new IndexingException("Failed to retrieve alt text datastream for {}" + filePid, e);
        }
        // Fall back to using the machine generated alt text if it exists
        return mgContentService.extractAltText(mgdNode);
    }

    private String getFullDescription(PID filePid, JsonNode mgdNode) {
        // TODO implement retrieval from fedora when that functionality is in place
        return mgContentService.extractFullDescription(mgdNode);
    }

    private String getTranscript(PID filePid, JsonNode mgdNode) {
        // TODO implement retrieval from fedora when that functionality is in place
        return mgContentService.extractTranscript(mgdNode);
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setMgContentService(MachineGenerateContentService mgContentService) {
        this.mgContentService = mgContentService;
    }
}
