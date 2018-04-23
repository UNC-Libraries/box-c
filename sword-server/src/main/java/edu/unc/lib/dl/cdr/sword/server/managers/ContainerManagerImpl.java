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
package edu.unc.lib.dl.cdr.sword.server.managers;

import java.util.Map;

import org.apache.abdera.i18n.iri.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ContainerManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.util.DepositReportingUtil;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.update.AtomPubMetadataUIP;
import edu.unc.lib.dl.update.UIPException;
import edu.unc.lib.dl.update.UIPProcessor;
import edu.unc.lib.dl.update.UpdateException;
import edu.unc.lib.dl.update.UpdateOperation;
import edu.unc.lib.dl.util.ErrorURIRegistry;

/**
 *
 * @author bbpennel
 *
 */
public class ContainerManagerImpl extends AbstractFedoraManager implements ContainerManager {

    private static Logger log = LoggerFactory.getLogger(ContainerManagerImpl.class);

    //    private DigitalObjectManager digitalObjectManager;
    private UIPProcessor uipProcessor;
    //    private ManagementClient managementClient;
    private DepositReportingUtil depositReportingUtil;

    private DepositReceipt updateMetadata(String editIRI, Deposit deposit, AuthCredentials auth,
            SwordConfiguration config, UpdateOperation operation) throws SwordError, SwordServerException,
    SwordAuthException {
        PID targetPID = extractPID(editIRI, SwordConfigurationImpl.EDIT_PATH + "/");

        SwordConfigurationImpl configImpl = (SwordConfigurationImpl) config;

        if (!hasAccess(auth, targetPID, Permission.editDescription, configImpl)) {
            throw new SwordError(ErrorURIRegistry.INSUFFICIENT_PRIVILEGES, 403,
                    "Insufficient privileges to update metadata for " + targetPID.getPid());
        }

        AtomPubMetadataUIP uip;
        try {
            uip = new AtomPubMetadataUIP(targetPID, auth.getUsername(), operation, deposit.getSwordEntry().getEntry());
        } catch (UIPException e) {
            log.warn("An exception occurred while attempting to create metadata UIP for " + targetPID.getPid(), e);
            throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 500,
                    "An exception occurred while attempting to create metadata UIP for " + editIRI, e);
        }

        try {
            uipProcessor.process(uip);
        } catch (UpdateException e) {
            if (e.getCause() instanceof AuthorizationException) {
                throw new SwordError(ErrorURIRegistry.INSUFFICIENT_PRIVILEGES, 403,
                        "Failed to authorize update metadata operation", e);
            }
            throw new SwordError(ErrorURIRegistry.UPDATE_EXCEPTION, 500,
                    "An exception occurred while attempting to update object " + targetPID.getPid(), e);
        } catch (UIPException e) {
            log.warn("Failed to process UIP for " + targetPID.getPid(), e);
            throw new SwordError(ErrorURIRegistry.UPDATE_EXCEPTION, 500,
                    "A problem occurred while attempting to perform the requested update operation on "
                            + editIRI, e);
        }

        DepositReceipt receipt = new DepositReceipt();
        receipt.setLocation(new IRI(editIRI));
        receipt.setEmpty(true);

        // Update the objects in progress status
        this.setInProgress(targetPID, deposit, receipt);

        return receipt;
    }

    @Override
    public DepositReceipt replaceMetadata(String editIRI, Deposit deposit, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
        return updateMetadata(editIRI, deposit, auth, config, UpdateOperation.REPLACE);
    }

    @Override
    public DepositReceipt replaceMetadataAndMediaResource(String editIRI, Deposit deposit, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
        throw new SwordServerException("Method not yet supported");
    }

    @Override
    public DepositReceipt addMetadataAndResources(String editIRI, Deposit deposit, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
        throw new SwordServerException("Method not yet supported");
    }

    @Override
    public DepositReceipt addMetadata(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {
        return updateMetadata(editIRI, deposit, auth, config, UpdateOperation.ADD);
    }

    @Override
    public DepositReceipt addResources(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {
        // This happens in the MediaResourceManager. This method isn't referenced
        return null;
    }

    /**
     * Deletes the object identified in editIRI if the submitted auth credentials have delete rights to the targeted
     * object.
     */
    @Override
    public void deleteContainer(String editIRI, AuthCredentials auth, SwordConfiguration config) throws SwordError,
    SwordServerException, SwordAuthException {
        // Ignoring on-behalf-of for the moment
        SwordConfigurationImpl configImpl = (SwordConfigurationImpl) config;

        PID targetPID = extractPID(editIRI, SwordConfigurationImpl.EDIT_PATH + "/");

        if (!hasAccess(auth, targetPID, Permission.moveToTrash, configImpl)) {
            log.debug("Insufficient privileges to delete object " + targetPID.getPid());
            throw new SwordError(ErrorURIRegistry.INSUFFICIENT_PRIVILEGES, 403,
                    "Insufficient privileges to delete object " + targetPID.getPid());
        }

        //        try {
        //            this.digitalObjectManager.delete(targetPID, auth.getUsername(), "Deleted by "
        //              + auth.getUsername());
        //        } catch (NotFoundException e) {
        //            throw new SwordError(ErrorURIRegistry.RESOURCE_NOT_FOUND, 404,
        //                    "Unable to delete the object " + targetPID.getPid()
        //                    + ".  The object was not found in the repository.");
        //        } catch (IngestException e) {
        //            throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 500,
        //                    "Failed to delete object " + targetPID.getPid(), e);
        //        }
    }

    /**
     * Empty body request with headers. Allows for declaring an item to no longer be in-progress
     */
    @Override
    public DepositReceipt useHeaders(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {

        PID targetPID = extractPID(editIRI, SwordConfigurationImpl.EDIT_PATH + "/");

        DepositReceipt receipt = new DepositReceipt();
        receipt.setLocation(new IRI(editIRI));

        SwordConfigurationImpl configImpl = (SwordConfigurationImpl) config;

        if (!hasAccess(auth, targetPID, Permission.editAccessControl, configImpl)) {
            throw new SwordError(ErrorURIRegistry.INSUFFICIENT_PRIVILEGES, 403,
                    "Insufficient privileges to update object headers " + targetPID.getPid());
        }

        this.setInProgress(targetPID, deposit, receipt);

        return receipt;
    }

    /**
     * Determines if the request is a statement request instead of a deposit receipt request. Does not return a
     * statement.
     */
    @Override
    public boolean isStatementRequest(String editIRI, Map<String, String> accept, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * After-the-fact deposit receipt retrieval method. From the EDIT-IRI
     */
    @Override
    public DepositReceipt getEntry(String editIRIString, Map<String, String> accept, AuthCredentials auth,
            SwordConfiguration configBase) throws SwordServerException, SwordError, SwordAuthException {

        PID targetPID = extractPID(editIRIString, SwordConfigurationImpl.EDIT_PATH + "/");

        SwordConfigurationImpl config = (SwordConfigurationImpl) configBase;

        if (!hasAccess(auth, targetPID, Permission.viewDescription, config)) {
            throw new SwordError(ErrorURIRegistry.INSUFFICIENT_PRIVILEGES, 403,
                    "Insufficient privileges to get deposit receipt " + targetPID.getPid());
        }

        DepositReceipt receipt = depositReportingUtil.retrieveDepositReceipt(targetPID, config);

        return receipt;
    }

    private void setInProgress(PID targetPID, Deposit deposit, DepositReceipt receipt) throws SwordError {
        //        String state = tripleStoreQueryService.fetchState(targetPID);
        //        if (deposit.isInProgress() != Boolean.parseBoolean(state)) {
        //            try {
        //                log.debug("Updating active state of in-progress item");
        //                managementClient.addLiteralStatement(targetPID, FedoraProperty.Active.getFragment(),
        //                        FedoraProperty.Active.getNamespace(), "Active", null);
        //                receipt.setVerboseDescription(targetPID.getPid() + " is " +
        //                        ((deposit.isInProgress()) ? "" : "not")
        //                        + " in-progress");
        //            } catch (FedoraException e) {
        //                throw new SwordError(ErrorURIRegistry.UPDATE_EXCEPTION, 500,
        //                      "Failed to update active state for "
        //                        + targetPID.getPid());
        //            }
        //        }
    }

    public void setUipProcessor(UIPProcessor uipProcessor) {
        this.uipProcessor = uipProcessor;
    }

    public void setDepositReportingUtil(DepositReportingUtil depositReportingUtil) {
        this.depositReportingUtil = depositReportingUtil;
    }
}
