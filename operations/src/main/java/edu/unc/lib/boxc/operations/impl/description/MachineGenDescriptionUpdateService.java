package edu.unc.lib.boxc.operations.impl.description;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Service for updating the machine generated description datastream
 *
 * @author snluong
 */
public class MachineGenDescriptionUpdateService {
    private static final Logger log = LoggerFactory.getLogger(MachineGenDescriptionUpdateService.class);
    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private String derivativeBasePath;

    public Path updateMachineGenDescription(MachineGenDescriptionRequest request) {
        var agent = request.getAgent();
        var pid = PIDs.get(request.getPidString());

        aclService.assertHasAccess("User does not have permission to update machine generated descriptions",
                pid, agent.getPrincipals(), Permission.editDescription);

        var binaryId = pid.getId();
        var binaryPath = idToPath(binaryId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);

        try {
            // check that object is a file object
            repositoryObjectLoader.getFileObject(pid);
            var derivativePath = Paths.get(derivativeBasePath, binaryPath, binaryId + ".txt");
            var derivative = derivativePath.toFile();
            var parentDir = derivative.getParentFile();

            // Create missing parent directories if necessary
            if (parentDir != null) {
                Files.createDirectories(parentDir.toPath());
                FileUtils.write(derivative, request.getDescription(), UTF_8);
            }
            return derivativePath;
        } catch (ObjectTypeMismatchException e) {
            log.debug("Object {} is not a file object", request.getPidString(), e);
            throw new IllegalArgumentException("Object" + request.getPidString() + "is not a file object");
        } catch (IOException e) {
            throw new ServiceException("Unable to write to gen description file for: " + binaryId, e);
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
