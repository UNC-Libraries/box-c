package edu.unc.lib.boxc.operations.impl.aspace;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrAspace;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.aspace.RefIdRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Service which assigns an ArchivesSpace Ref ID to a work
 */
public class RefIdService {
    private static final Logger log = LoggerFactory.getLogger(RefIdService.class);
    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private IndexingMessageSender indexingMessageSender;

    public void updateRefId(RefIdRequest request) {
        var pidString = request.getPidString();
        var workPid = PIDs.get(pidString);
        var agent = request.getAgent();
        var agentPrincipals = agent.getPrincipals();
        log.debug("Updating Ref ID for {}", pidString);
        aclService.assertHasAccess(
                "User does not have permission to edit Aspace properties",
                workPid, agentPrincipals, Permission.editAspaceProperties);

        var repoObj = repoObjLoader.getRepositoryObject(workPid);
        if (!(repoObj instanceof WorkObject)) {
            throw new InvalidOperationForObjectType("Object " + pidString + " of type " + repoObj.getClass().getName()
                    + " cannot be assigned an Aspace Ref ID.");
        }

        var currentRefId = getCurrentRefId(repoObj);
        var requestRefId = request.getRefId();
        // if currentRefId is null (does not exist) and the request ID is blank, do nothing
        if (Objects.equals(null,currentRefId) && StringUtils.isBlank(requestRefId)) {
            log.debug("The current Ref ID is null and the requested Ref ID is blank");
            return;
        }
        // if we're just updating to the same existing ID, do nothing
        if (!StringUtils.isBlank(currentRefId) && Objects.equals(currentRefId, requestRefId)) {
            log.debug("The current Ref ID exists: {} and the requested Ref ID is the same", currentRefId);
            return;
        }
        // now, if the request ID is blank delete the property
        if (StringUtils.isBlank(requestRefId)) {
            log.debug("The requested Ref ID is blank, deleting the property");
            repositoryObjectFactory.deleteProperty(repoObj, CdrAspace.refId);
        } else {
            repositoryObjectFactory.createExclusiveRelationship(repoObj, CdrAspace.refId, request.getRefId());
        }
        indexingMessageSender.sendIndexingOperation(agent.getUsername(), workPid, IndexingActionType.UPDATE_ASPACE_REF_ID);
    }

    private String getCurrentRefId(RepositoryObject repositoryObject) {
        var property = repositoryObject.getResource().getProperty(CdrAspace.refId);
        if (property == null) {
            log.debug("property is null");
            return null;
        }
        return property.getString();
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
