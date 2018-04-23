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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.deposit.DepositHandler;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.util.ErrorURIRegistry;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.Priority;

/**
 * Manager responsible for performing ingest of new objects or packages
 *
 * @author bbpennel
 *
 */
public class CollectionDepositManagerImpl extends AbstractFedoraManager implements CollectionDepositManager {
    private static Logger log = LoggerFactory.getLogger(CollectionDepositManagerImpl.class);

    private Map<PackagingType, DepositHandler> packageHandlers;
    private List<String> priorityDepositors;

    @Override
    public DepositReceipt createNew(String collectionURI, Deposit deposit, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {

        log.debug("Preparing to do collection deposit to " + collectionURI);
        if (collectionURI == null) {
            throw new SwordError(ErrorURIRegistry.RESOURCE_NOT_FOUND, 404, "No collection URI was provided");
        }

        String depositor = auth.getUsername();
        String owner = (auth.getOnBehalfOf() != null) ? auth.getOnBehalfOf() : depositor;

        SwordConfigurationImpl configImpl = (SwordConfigurationImpl) config;

        PID containerPID = extractPID(collectionURI, SwordConfigurationImpl.COLLECTION_PATH + "/");

        if (!hasAccess(auth, containerPID, Permission.addRemoveContents, configImpl)) {
            throw new SwordError(ErrorURIRegistry.INSUFFICIENT_PRIVILEGES, 403,
                    "Insufficient privileges to deposit to container " + containerPID.getPid());
        }

        // Get the enum for the provided packaging type. Null can be a legitimate type
        PackagingType type = PackagingType.getPackagingType(deposit.getPackaging());
        try {
            DepositHandler depositHandler = packageHandlers.get(type);
            // Check to see if the depositor is in the list to receive higher priority
            Priority priority = priorityDepositors.contains(depositor) ? Priority.high : Priority.normal;

            return depositHandler.doDeposit(containerPID, deposit, type, priority, config, depositor, owner);
        } catch (JDOMException e) {
            log.warn("Failed to deposit", e);
            throw new SwordError(UriRegistry.ERROR_CONTENT, 415,
                    "A problem occurred while attempting to parse your deposit: " + e.getMessage());
        } catch (IngestException e) {
            log.warn("An exception occurred while attempting to ingest package " + deposit.getFilename() + " of type "
                    + deposit.getPackaging(), e);
            throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 500,
                    "An exception occurred while attempting to ingest package " + deposit.getFilename() + " of type "
                            + deposit.getPackaging(), e);
        } catch (SwordError e) {
            throw e;
        } catch (Exception e) {
            throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 500,
                    "Unexpected exception occurred while attempting to perform a METS deposit", e);
        }
    }

    public void setPackageHandlers(Map<PackagingType, DepositHandler> packageHandlers) {
        this.packageHandlers = packageHandlers;
    }

    public void setPriorityDepositors(String depositors) {
        priorityDepositors = Arrays.asList(depositors.split(","));
    }
}
