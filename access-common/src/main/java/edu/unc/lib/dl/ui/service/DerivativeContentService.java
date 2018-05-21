/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ui.service;

import static edu.unc.lib.dl.acl.fcrepo4.DatastreamPermissionUtil.getPermissionForDatastream;
import static edu.unc.lib.dl.model.DatastreamType.getByIdentifier;
import static edu.unc.lib.dl.ui.service.FedoraContentService.CONTENT_DISPOSITION;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamType;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;
import edu.unc.lib.dl.util.DerivativeService;
import edu.unc.lib.dl.util.DerivativeService.Derivative;

/**
 * Streams content for derivative files of repository objects.
 *
 * @author bbpennel
 *
 */
public class DerivativeContentService {

    private static final int BUFFER_SIZE = 4096;

    private DerivativeService derivativeService;

    private AccessControlService accessControlService;

    public DerivativeContentService() {
    }

    /**
     * Set content headers and stream the content of the specified derivative from the object identified by pid.
     *
     * @param pid pid of object containing the derivative
     * @param dsName name of derivative being requested.
     * @param principals principals of requesting client
     * @param asAttachment if true, then content-disposition header will specify
     *            as "attachment" instead of "inline"
     * @param response response content and headers will be added to.
     * @throws IOException if unable to stream content to the response.
     * @throws ResourceNotFoundException if an invalid derivative type is requested.
     */
    public void streamData(PID pid, String dsName, AccessGroupSet principals, boolean asAttachment,
            HttpServletResponse response) throws IOException, ResourceNotFoundException {

        DatastreamType derivType = getByIdentifier(dsName);
        if (derivType == null || !derivativeService.listDerivativeTypes().contains(derivType)) {
            throw new IllegalArgumentException(dsName + " is not a valid derivative type.");
        }

        accessControlService.assertHasAccess("Insufficient permissions to access derivative "
                + dsName + " for object " + pid,
                pid, principals, getPermissionForDatastream(derivType));

        Derivative deriv = derivativeService.getDerivative(pid, derivType);
        if (deriv == null) {
            throw new ResourceNotFoundException("Deriviatve " + dsName + " does not exist for object " + pid);
        }

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
