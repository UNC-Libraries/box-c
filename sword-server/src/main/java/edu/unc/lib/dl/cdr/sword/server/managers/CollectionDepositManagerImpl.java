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

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.model.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.util.DepositReportingUtil;
import edu.unc.lib.dl.persist.api.ingest.DepositData;
import edu.unc.lib.dl.persist.services.ingest.DepositSubmissionService;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.ErrorURIRegistry;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.Priority;
import edu.unc.lib.dl.util.UnsupportedPackagingTypeException;

/**
 * Manager responsible for performing ingest of new objects or packages
 *
 * @author bbpennel
 *
 */
public class CollectionDepositManagerImpl extends AbstractFedoraManager implements CollectionDepositManager {
    private static Logger log = LoggerFactory.getLogger(CollectionDepositManagerImpl.class);

    private DepositSubmissionService depositService;
    private List<String> priorityDepositors;
    private DepositReportingUtil depositReportingUtil;

    @Override
    public DepositReceipt createNew(String collectionURI, Deposit deposit, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {

        log.debug("Preparing to do collection deposit to {}", collectionURI);
        if (collectionURI == null) {
            throw new SwordError(ErrorURIRegistry.RESOURCE_NOT_FOUND, 404, "No collection URI was provided");
        }

        String depositor = auth.getUsername();
        String owner = (auth.getOnBehalfOf() != null) ? auth.getOnBehalfOf() : depositor;
        AgentPrincipals agentPrincipals = new AgentPrincipals(owner, GroupsThreadStore.getGroups());

        PID containerPID = extractPID(collectionURI, SwordConfigurationImpl.COLLECTION_PATH + "/");

        // Get the enum for the provided packaging type. Null could be a legitimate type
        PackagingType type = PackagingType.getPackagingType(deposit.getPackaging());
        DepositData depositData = new DepositData(deposit.getInputStream(),
                deposit.getFilename(),
                deposit.getMimeType(),
                type,
                DepositMethod.SWORD13.getLabel(),
                agentPrincipals);

        depositData.setSlug(deposit.getSlug());
        depositData.setMd5(deposit.getMd5());
        Priority priority = priorityDepositors.contains(depositor) ? Priority.high : Priority.normal;
        depositData.setPriority(priority);

        try {
            PID depositPid = depositService.submitDeposit(containerPID, depositData);

            return buildReceipt(depositPid, config);
        } catch (AccessRestrictionException e) {
            throw new SwordError(ErrorURIRegistry.INSUFFICIENT_PRIVILEGES, 403);
        } catch (UnsupportedPackagingTypeException e) {
            throw new SwordError(ErrorURIRegistry.INVALID_INGEST_PACKAGE, 400,
                    "Unsupported deposit package type " + type);
        } catch (Exception e) {
            throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 500,
                    "Unexpected exception occurred while attempting to perform deposit", e);
        }
    }

    private DepositReceipt buildReceipt(PID depositPid, SwordConfiguration config) {
        return depositReportingUtil.retrieveDepositReceipt(depositPid,
                (SwordConfigurationImpl) config);
    }

    /**
     * @param depositService the depositService to set
     */
    public void setDepositService(DepositSubmissionService depositService) {
        this.depositService = depositService;
    }

    public void setPriorityDepositors(String depositors) {
        priorityDepositors = Arrays.asList(depositors.split(","));
    }

    /**
     * @param depositReportingUtil the depositReportingUtil to set
     */
    public void setDepositReportingUtil(DepositReportingUtil depositReportingUtil) {
        this.depositReportingUtil = depositReportingUtil;
    }
}
