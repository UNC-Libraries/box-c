package edu.unc.lib.boxc.operations.impl.description;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Service for updating the machine generated description datastream
 *
 * @author snluong
 */
public class MachineGenDescriptionUpdateService {
    private static final Logger log = LoggerFactory.getLogger(MachineGenDescriptionUpdateService.class);
    private AccessControlService aclService;
    private RepositoryObjectFactory repositoryObjectFactory;
    private RepositoryObjectLoader repositoryObjectLoader;
    private String derivativeBasePath;

    public void updateMachineGenDescription(MachineGenDescriptionRequest request) {
        var agent = request.getAgent();
        var pid = PIDs.get(request.getPidString());

        aclService.assertHasAccess("User does not have permission to update machine generated descriptions",
                pid, agent.getPrincipals(), Permission.editDescription);

        try {
            var file = repositoryObjectLoader.getFileObject(pid);
            file.
            Path derivativePath = Paths.get(derivativeBasePath, "machine_generated_description.txt");
            File derivative = derivativePath.toFile();
            File parentDir = derivative.getParentFile();

            // Create missing parent directories if necessary
            if (parentDir != null) {
                try {
                    Files.createDirectories(parentDir.toPath());
                } catch (IOException e) {
                    throw new IOException("Failed to create parent directories for " + derivativePath + ".", e);
                }

                FileUtils.write(derivative, request.getDescription(), UTF_8);
            }
        } catch (ObjectTypeMismatchException e) {
            log.debug("Object {} is not a file object", request.getPidString(), e);
            throw new IllegalArgumentException("Object is not a file object", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    public void setDerivativeBasePath(String derivativeBasePath) {
        this.derivativeBasePath = derivativeBasePath;
    }
}
