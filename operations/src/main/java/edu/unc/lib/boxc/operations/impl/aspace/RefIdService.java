package edu.unc.lib.boxc.operations.impl.aspace;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrAspace;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.aspace.RefIdRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;

/**
 * Service which assigns an ArchivesSpace Ref ID to a work
 */
public class RefIdService {
    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private IndexingMessageSender indexingMessageSender;

    public void updateRefId(RefIdRequest request) {
        var pidString = request.getPidString();
        var workPid = PIDs.get(pidString);
        var agent = request.getAgent();
        var agentPrincipals = agent.getPrincipals();
        aclService.assertHasAccess(
                "User does not have permission to edit Aspace properties",
                workPid, agentPrincipals, Permission.editAspaceProperties);
        var repoObj = repoObjLoader.getRepositoryObject(workPid);
        if (!(repoObj instanceof WorkObject)) {
            throw new InvalidOperationForObjectType("Object " + pidString + " of type " + repoObj.getClass().getName()
                    + " cannot be assigned an Aspace Ref ID.");
        }

        repositoryObjectFactory.createExclusiveRelationship(repoObj, CdrAspace.refId, request.getRefId());
        indexingMessageSender.sendIndexingOperation(agent.getUsername(), workPid, IndexingActionType.UPDATE_ASPACE_REF_ID);
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }
}
