package edu.unc.lib.boxc.web.common.services;

import static edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil.getPermissionForDatastream;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WebContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.StoragePolicy;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * Streams binary content from repository objects.
 *
 * @author bbpennel
 */
public class FedoraContentService {
    private static final Logger LOG = LoggerFactory.getLogger(FedoraContentService.class);

    private static final int BUFFER_SIZE = 4096;

    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    private AccessControlService accessControlService;

    private RepositoryObjectLoader repositoryObjectLoader;

    /**
     * Set content headers and stream the binary content of the specified
     * datastream from the object identified by pid.
     *
     * @param pid pid of object containing datastream
     * @param dsName name of datastream being requested. If null, then original
     *            file datastream is assumed.
     * @param principals principals of requesting client
     * @param asAttachment if true, then content-disposition header will specify
     *            as "attachment" instead of "inline"
     * @param response response content and headers will be added to.
     * @param range requested byte range of datastream (optional)
     * @throws IOException if unable to stream content to the response.
     */
    public void streamData(PID pid, String dsName, boolean asAttachment,
            HttpServletResponse response, String range) throws IOException {
        // Default datastream is DATA_FILE
        String datastream = dsName == null ? ORIGINAL_FILE.getId() : dsName;

        DatastreamType dsType = DatastreamType.getByIdentifier(datastream);
        if (dsType == null) {
            throw new NotFoundException("Provided value is not the name of a known datastream type");
        }
        if (dsType.getStoragePolicy().equals(StoragePolicy.EXTERNAL)) {
            throw new IllegalArgumentException("Cannot stream external datastream " + datastream);
        }

        LOG.debug("Streaming datastream {} from object {}", datastream, pid);

        BinaryObject binObj;
        if (ORIGINAL_FILE.getId().equals(datastream)) {
            FileObject fileObj = repositoryObjectLoader.getFileObject(pid);
            binObj = fileObj.getOriginalFile();
        } else {
            String dsPath = URIUtil.join(pid.getQualifiedId(), dsType.getContainer(), dsType.getId());
            PID dsPid = PIDs.get(dsPath);
            binObj = repositoryObjectLoader.getBinaryObject(dsPid);
        }

        // Set binary detail response headers
        response.setHeader(CONTENT_LENGTH, Long.toString(binObj.getFilesize()));
        response.setHeader(CONTENT_TYPE, binObj.getMimetype());
        String binaryName = binObj.getFilename();
        String filename = binaryName == null ? pid.getId() : binaryName;
        if (asAttachment) {
            response.setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        } else {
            response.setHeader(CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
        }

        // Stream binary content to http response
        try (InputStream binStream = binObj.getBinaryStream(range)) {
            OutputStream outStream = response.getOutputStream();
            IOUtils.copy(binStream, outStream, BUFFER_SIZE);
        }
    }

    public void streamEventLog(PID pid, AccessGroupSet principals, boolean asAttachment,
            HttpServletResponse response) throws IOException {

        DatastreamType dsType = DatastreamType.MD_EVENTS;
        String datastream = dsType.getId();

        accessControlService.assertHasAccess("Insufficient permissions to access " + datastream + " for object " + pid,
                pid, principals, getPermissionForDatastream(datastream));

        RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);

        String filename = pid.getId() + "_" + datastream + ".ttl";
        if (asAttachment) {
            response.setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        } else {
            response.setHeader(CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
        }

        // Stream the premis model out as turtle
        response.setHeader(CONTENT_TYPE, WebContent.contentTypeTurtle);
        Model premisModel = repoObj.getPremisLog().getEventsModel();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            RDFDataMgr.write(bos, premisModel, RDFFormat.TURTLE);
            response.setHeader(CONTENT_LENGTH, Integer.toString(bos.size()));
            bos.writeTo(response.getOutputStream());
        }
    }

    /**
     * @param accessControlService the accessControlService to set
     */
    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    /**
     * @param repositoryObjectLoader the repositoryObjectLoader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }
}