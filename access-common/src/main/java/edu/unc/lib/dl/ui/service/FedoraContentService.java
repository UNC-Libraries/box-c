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
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.ORIGINAL_FILE;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;

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
     * @throws IOException if unable to stream content to the response.
     */
    public void streamData(PID pid, String dsName, AccessGroupSet principals, boolean asAttachment,
            HttpServletResponse response) throws IOException {
        // Default datastream is DATA_FILE
        String datastream = dsName == null ? ORIGINAL_FILE : dsName;

        accessControlService.assertHasAccess("Insufficient permissions to access " + datastream + " for object " + pid,
                pid, principals, getPermissionForDatastream(datastream));

        LOG.debug("Streaming datastream {} from object {}", datastream, pid);

        FileObject fileObj = repositoryObjectLoader.getFileObject(pid);
        BinaryObject binObj;
        if (ORIGINAL_FILE.equals(datastream)) {
            binObj = fileObj.getOriginalFile();
        } else {
            binObj = fileObj.getBinaryObject(datastream);
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
        OutputStream outStream = response.getOutputStream();
        IOUtils.copy(binObj.getBinaryStream(), outStream, BUFFER_SIZE);
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