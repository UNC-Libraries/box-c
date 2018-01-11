/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.deposit.validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;

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
 * @return list of missing oibject PIDs as a String
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

    /**
     * Return an iterator for the children of the given resource, based on what
     * type of container it is.
     *
     * @param resc
     * @return
     */
    private NodeIterator getChildIterator(Resource resc) {
        if (resc.hasProperty(RDF.type, RDF.Bag)) {
            return resc.getModel().getBag(resc).iterator();
        } else if (resc.hasProperty(RDF.type, RDF.Seq)) {
            return resc.getModel().getSeq(resc).iterator();
        } else {
            return null;
        }
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
