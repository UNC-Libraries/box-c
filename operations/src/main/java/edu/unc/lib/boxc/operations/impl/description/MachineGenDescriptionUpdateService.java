package edu.unc.lib.boxc.operations.impl.description;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for updating the machine generated description datastream
 *
 * @author bbpennel
 */
public class MachineGenDescriptionUpdateService {
    private static final Logger log = LoggerFactory.getLogger(MachineGenDescriptionUpdateService.class);
    private AccessControlService aclService;
    private RepositoryObjectFactory repositoryObjectFactory;
    private RepositoryObjectLoader repositoryObjectLoader;

    public void updateMachineGenDescription(MachineGenDescriptionRequest request) {
        var agent = request.getAgent();
        var pid = PIDs.get(request.getPidString());

        aclService.assertHasAccess("User does not have permission to update machine generated descriptions",
                pid, agent.getPrincipals(), Permission.viewMetadata);

        try {
            var file = repositoryObjectLoader.getFileObject(pid);
            repositoryObjectFactory.createExclusiveRelationship(file, Cdr.hasMachineGenDescription,
                    request.getDescription());
        } catch (ObjectTypeMismatchException e) {
            log.debug("Object {} is not a file object", request.getPidString(), e);
            throw new IllegalArgumentException("Object is not a file object", e);
        }

    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }
}
