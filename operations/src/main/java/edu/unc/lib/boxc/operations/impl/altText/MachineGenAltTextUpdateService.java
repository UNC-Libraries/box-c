package edu.unc.lib.boxc.operations.impl.altText;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

import static edu.unc.lib.boxc.operations.impl.utils.MachineGenUtil.getDerivativePath;
import static edu.unc.lib.boxc.operations.impl.utils.MachineGenUtil.writeToFile;

public class MachineGenAltTextUpdateService {

    private static final Logger log = LoggerFactory.getLogger(MachineGenAltTextUpdateService.class);
    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private String derivativeBasePath;

    public Path updateMachineGenAltText(MachineGenAltTextRequest request) {
        var agent = request.getAgent();
        var pid = PIDs.get(request.getPidString());

        aclService.assertHasAccess("User does not have permission to update machine generated alt text",
                pid, agent.getPrincipals(), Permission.editDescription);

        var binaryId = pid.getId();

        try {
            // check that object is a file object
            repositoryObjectLoader.getFileObject(pid);
            var derivativePath = getDerivativePath(derivativeBasePath, binaryId);
            writeToFile(derivativePath, request.getAltText());
            return derivativePath;
        } catch (ObjectTypeMismatchException e) {
            log.debug("Object {} is not a file object", request.getPidString(), e);
            throw new IllegalArgumentException("Object " + request.getPidString() + " is not a file object");
        } catch (IOException e) {
            throw new ServiceException("Unable to write to gen alt text file for: " + binaryId, e);
        }
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setDerivativeBasePath(String derivativeBasePath) {
        this.derivativeBasePath = derivativeBasePath;
    }
}
