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
package edu.unc.lib.dl.cdr.sword.server.managers;

import static edu.unc.lib.dl.acl.util.GroupsThreadStore.getAgentPrincipals;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.swordapp.server.SwordError;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ErrorURIRegistry;

/**
 *
 * @author bbpennel
 *
 */
public abstract class AbstractFedoraManager {
    private static Logger LOG = LoggerFactory.getLogger(AbstractFedoraManager.class);

    @Autowired
    protected String swordPath;

    @Autowired
    protected AccessControlService aclService;

    protected String readFileAsString(String filePath) throws java.io.IOException {
        LOG.debug("Loading path file " + filePath);
        try (java.io.InputStream inStream = this.getClass().getResourceAsStream(filePath)) {
            return IOUtils.toString(inStream, "UTF-8");
        }
    }

    protected PID extractPID(String uri, String basePath) {
        String pidString = null;
        int pidIndex = uri.indexOf(basePath);
        if (pidIndex > -1) {
            pidString = uri.substring(pidIndex + basePath.length());
        }

        PID targetPID = null;
        if (pidString.trim().length() == 0) {
            targetPID = RepositoryPaths.getContentRootPid();
        } else {
            targetPID = PIDs.get(pidString);
        }
        return targetPID;
    }

    protected void assertHasAccess(String message, PID pid, Permission permission) throws SwordError {
        if (!aclService.hasAccess(pid, getAgentPrincipals().getPrincipals(), permission)) {
            throw new SwordError(ErrorURIRegistry.INSUFFICIENT_PRIVILEGES, SC_FORBIDDEN, message);
        }
    }

    public String getSwordPath() {
        return swordPath;
    }

    public void setSwordPath(String swordPath) {
        this.swordPath = swordPath;
    }
}
