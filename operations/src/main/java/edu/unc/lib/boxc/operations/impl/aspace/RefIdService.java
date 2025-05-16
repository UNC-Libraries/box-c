package edu.unc.lib.boxc.operations.impl.aspace;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrAspace;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * Service which assigns an ArchivesSpace Ref ID to a work
 */
public class RefIdService {
    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repositoryObjectFactory;

    public void updateRefId(RefIdRequest request) {
        var pidString = request.getPidString();
        var workPid = PIDs.get(pidString);
        var agentPrincipals = request.getAgent().getPrincipals();
        aclService.assertHasAccess(
                "User does not have permission to edit Aspace properties",
                workPid, agentPrincipals, Permission.editAspaceProperties);
        var repoObj = repoObjLoader.getRepositoryObject(workPid);
        if (!(repoObj instanceof WorkObject)) {
            throw new InvalidOperationForObjectType("Object of type " + repoObj.getClass().getName()
                    + " cannot be assigned an Aspace Ref ID.");
        }

        repositoryObjectFactory.createExclusiveRelationship(repoObj, CdrAspace.refId, request.getRefId());
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
}
