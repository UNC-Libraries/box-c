package edu.unc.lib.boxc.operations.jms.destroy;


import static edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsHelper.assertCanDestroy;
import static edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsHelper.serializeDestroyRequest;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.MessageSender;

/**
 * Service that manages the destruction of objects in the repository and their replacement by tombstones
 *
 * @author harring
 *
 */
public class DestroyObjectsService extends MessageSender {

    private static final Logger log = LoggerFactory.getLogger(DestroyObjectsService.class);

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;

    public String destroyObjects(AgentPrincipals agent, String... ids) {
        return destroyObjects(agent, false, ids);
    }

    /**
     * Checks whether the active user has permissions to destroy the listed objects,
     * and then kicks off a job to destroy the permitted ones asynchronously
     *
     * @param agent security principals of the agent making request
     * @param completely if true, the objects will be completely cleaned up
     * @param ids list of objects to destroy
     * @return the id of the destroy job created
     */
    public String destroyObjects(AgentPrincipals agent, boolean completely, String... ids) {
        if (ids.length == 0) {
            throw new IllegalArgumentException("Must provide ids for one or more objects to destroy");
        }

        for (String id : ids) {
            PID pid = PIDs.get(id);
            RepositoryObject obj = repoObjLoader.getRepositoryObject(pid);
            assertCanDestroy(agent, obj, aclService);
        }
        String jobId = UUID.randomUUID().toString();
        DestroyObjectsRequest request = new DestroyObjectsRequest(jobId, agent, ids);
        request.setDestroyCompletely(completely);
        sendMessage(serializeDestroyRequest(request));

        log.info("Destroy job for {} objects started by {}", ids.length, agent.getUsernameUri());
        return jobId;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }
}
