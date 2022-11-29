package edu.unc.lib.boxc.web.sword.managers;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.swordapp.server.SwordError;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.web.sword.ErrorURIRegistry;

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
