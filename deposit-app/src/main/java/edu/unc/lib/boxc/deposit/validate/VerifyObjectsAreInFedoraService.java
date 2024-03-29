package edu.unc.lib.boxc.deposit.validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * A service for verifying that objects in a deposit actually make it into Fedora
 *
 * @author harring
 *
 */
public class VerifyObjectsAreInFedoraService {

    private FcrepoClient fcrepoClient;

/**
 * Builds a report of missing PIDs for a given deposit
 *
 * @param depositPid
 * @param objectPIDs
 * @return list of missing object PIDs as a String
 */
    public String listObjectPIDs(String depositPid, List<PID> objectPIDs) {
        StringBuilder buildListOfObjectPIDs = new StringBuilder();
        buildListOfObjectPIDs.append("The following objects from deposit ")
                .append(depositPid)
                .append(" did not make it to Fedora:\n");
        for (PID pid : objectPIDs) {
            buildListOfObjectPIDs.append(pid.toString() + "\n");
        }
        return buildListOfObjectPIDs.toString();
    }

    /**
     * Creates a list of PIDs for which the deposited objects did not make it into Fedora
     *
     * @param parentResc
     * @param fcrepoClient
     * @return a list of PIDs for deposited objects missing in Fedora
     */
    public List<PID> listObjectsNotInFedora(Collection<String> pids) {
        List<PID> objectsNotInFedora = new ArrayList<>();
        for (String pid : pids) {
            PID childPid = PIDs.get(pid);
            if (!objectExists(childPid)) {
                objectsNotInFedora.add(childPid);
            }
        }
        return objectsNotInFedora;
    }

    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }

    private boolean objectExists(PID pid) {
        try (FcrepoResponse response = fcrepoClient.head(pid.getRepositoryUri())
                .perform()) {
            return true;
        } catch (IOException e) {
            throw new FedoraException("Failed to close HEAD response for " + pid, e);
        } catch (FcrepoOperationFailedException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return false;
            }
            throw new FedoraException("Failed to check on object " + pid
                    + " during initialization", e);
        }
    }

}
