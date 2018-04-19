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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.swordapp.server.AuthCredentials;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.DatastreamPID;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author bbpennel
 *
 */
public abstract class AbstractFedoraManager {
    private static Logger LOG = LoggerFactory.getLogger(AbstractFedoraManager.class);

    @Autowired
    protected String swordPath;

    protected String readFileAsString(String filePath) throws java.io.IOException {
        LOG.debug("Loading path file " + filePath);
        try (java.io.InputStream inStream = this.getClass().getResourceAsStream(filePath)) {
            return IOUtils.toString(inStream);
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
            targetPID = new DatastreamPID(pidString);
        }
        return targetPID;
    }

    protected boolean hasAccess(AuthCredentials auth, PID pid, Permission permission, SwordConfigurationImpl config) {
        if (config.getAdminDepositor() != null && config.getAdminDepositor().equals(auth.getUsername())) {
            return true;
        }
        throw new RuntimeException("Not implemented");
        //        ObjectAccessControlsBean aclBean = aclService.getObjectAccessControls(pid);
        //        AccessGroupSet groups = GroupsThreadStore.getGroups();
        //
        //        return aclBean.hasPermission(groups, permission);
    }

    public String getSwordPath() {
        return swordPath;
    }

    public void setSwordPath(String swordPath) {
        this.swordPath = swordPath;
    }
}
