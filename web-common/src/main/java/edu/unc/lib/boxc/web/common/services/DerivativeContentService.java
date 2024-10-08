package edu.unc.lib.boxc.web.common.services;

import static edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil.getPermissionForDatastream;
import static edu.unc.lib.boxc.model.api.DatastreamType.getByIdentifier;
import static edu.unc.lib.boxc.model.fcrepo.services.DerivativeService.listDerivativeTypes;
import static edu.unc.lib.boxc.web.common.services.FedoraContentService.CONTENT_DISPOSITION;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService.Derivative;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Streams content for derivative files of repository objects.
 *
 * @author bbpennel
 *
 */
public class DerivativeContentService {

    private static final int BUFFER_SIZE = 8192;

    private DerivativeService derivativeService;

    private AccessControlService accessControlService;

    public DerivativeContentService() {
    }

    /**
     * Set content headers and stream the content of the specified derivative
     * from the object identified by pid.
     *
     * @param pid pid of object containing the derivative
     * @param dsName name of derivative being requested. Must be a derivative
     *            type, otherwise an IllegalArgumentException will be thrown.
     * @param principals principals of requesting client
     * @param asAttachment if true, then content-disposition header will specify
     *            as "attachment" instead of "inline"
     * @param response response content and headers will be added to.
     * @throws IOException if unable to stream content to the response.
     * @throws ResourceNotFoundException if an invalid derivative type is
     *             requested.
     */
    public void streamData(PID pid, String dsName, AccessGroupSet principals, boolean asAttachment,
            HttpServletResponse response) throws IOException, ResourceNotFoundException {

        DatastreamType derivType = getType(dsName);

        accessControlService.assertHasAccess("Insufficient permissions to access derivative "
                + dsName + " for object " + pid,
                pid, principals, getPermissionForDatastream(derivType));

        Derivative deriv = getDerivative(pid, dsName, derivType);

        File derivFile = deriv.getFile();
        response.setHeader(CONTENT_LENGTH, Long.toString(derivFile.length()));
        response.setHeader(CONTENT_TYPE, derivType.getMimetype());
        String filename = derivFile.getName();
        if (asAttachment) {
            response.setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        } else {
            response.setHeader(CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
        }

        OutputStream outStream = response.getOutputStream();
        IOUtils.copy(new FileInputStream(derivFile), outStream, BUFFER_SIZE);
    }

    private DatastreamType getType(String dsName) {
        DatastreamType derivType = getByIdentifier(dsName);
        if (derivType == null || !listDerivativeTypes().contains(derivType)) {
            throw new IllegalArgumentException(dsName + " is not a valid derivative type.");
        }
        return derivType;
    }

    private Derivative getDerivative (PID pid, String dsName, DatastreamType datastreamType) {
        Derivative derivative = derivativeService.getDerivative(pid, datastreamType);
        if (derivative == null) {
            throw new ResourceNotFoundException("Derivative " + dsName + " does not exist for object " + pid);
        }
        return derivative;
    }

    /**
     * @param derivativeService the derivativeService to set
     */
    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }

    /**
     * @param accessControlService the accessControlService to set
     */
    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }
}
