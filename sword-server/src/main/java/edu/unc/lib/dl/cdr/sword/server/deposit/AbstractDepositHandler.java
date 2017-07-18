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
package edu.unc.lib.dl.cdr.sword.server.deposit;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.util.DepositReportingUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
import edu.unc.lib.dl.util.RedisWorkerConstants.Priority;

/**
 * 
 * @author bbpennel
 *
 */
public abstract class AbstractDepositHandler implements DepositHandler {
    private static final Logger log = LoggerFactory.getLogger(AbstractDepositHandler.class);

    @Autowired
    protected DepositReportingUtil depositReportingUtil;
    @Autowired
    private DepositStatusFactory depositStatusFactory;
    @Autowired
    private File depositsDirectory;

    private Collection<String> overridePermissionGroups = null;

    public DepositStatusFactory getDepositStatusFactory() {
        return depositStatusFactory;
    }

    public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }

    public Collection<String> getOverridePermissionGroups() {
        return overridePermissionGroups;
    }

    public void setOverridePermissionGroups(
            Collection<String> overridePermissionGroups) {
        this.overridePermissionGroups = overridePermissionGroups;
    }

    public File getDepositsDirectory() {
        return depositsDirectory;
    }

    public void setDepositsDirectory(File depositsDirectory) {
        this.depositsDirectory = depositsDirectory;
    }

    public File makeNewDepositDirectory(String uuid) {
        File f = new File(getDepositsDirectory(), uuid);
        f.mkdir();
        return f;
    }

    public DepositReportingUtil getDepositReportingUtil() {
        return depositReportingUtil;
    }

    public void setDepositReportingUtil(DepositReportingUtil depositReportingUtil) {
        this.depositReportingUtil = depositReportingUtil;
    }

    protected DepositReceipt buildReceipt(PID depositID, SwordConfiguration config) throws SwordError {
        DepositReceipt receipt = depositReportingUtil.retrieveDepositReceipt(depositID,
                (SwordConfigurationImpl) config);
        return receipt;
    }

    protected void registerDeposit(PID depositPid, PID destination, Deposit deposit,
            PackagingType type, Priority priority, String depositor, String owner, Map<String, String> extras)
                    throws SwordError {
        Map<String, String> chkstatus = this.depositStatusFactory.get(depositPid.getUUID());
        if (chkstatus != null && !chkstatus.isEmpty()) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, 400,
                    "Duplicate request, repository already has deposit " + depositPid);
        }

        Map<String, String> status = new HashMap<String, String>();
        status.putAll(extras);

        // generic deposit fields
        status.put(DepositField.uuid.name(), depositPid.getUUID());
        status.put(DepositField.submitTime.name(), String.valueOf(System.currentTimeMillis()));
        status.put(DepositField.fileMimetype.name(), deposit.getMimeType());
        status.put(DepositField.depositorName.name(), owner);
        status.put(DepositField.depositorEmail.name(), GroupsThreadStore.getEmail());
        status.put(DepositField.containerId.name(), destination.getPid());
        status.put(DepositField.depositMethod.name(), DepositMethod.SWORD13.getLabel());
        status.put(DepositField.packagingType.name(), type.getUri());
        status.put(DepositField.depositMd5.name(), deposit.getMd5());
        try {
            if (deposit.getFilename() != null) {
                status.put(DepositField.fileName.name(),  URLDecoder.decode(deposit.getFilename(), "UTF-8"));
            }
            if (deposit.getSlug() != null) {
                status.put(DepositField.depositSlug.name(), URLDecoder.decode(deposit.getSlug(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            log.warn("Unable to properly decode value to UTF-8", e);
            status.put(DepositField.fileName.name(), deposit.getFilename());
            status.put(DepositField.depositSlug.name(), deposit.getSlug());
        }
        if (priority != null) {
            status.put(DepositField.priority.name(), priority.name());
        }
        String permGroups = null;
        if (this.getOverridePermissionGroups() != null) {
            permGroups = StringUtils.join(this.getOverridePermissionGroups(), ';');
        } else {
            permGroups = StringUtils.join(GroupsThreadStore.getGroups(), ';');
        }
        status.put(DepositField.permissionGroups.name(), permGroups);

        status.put(DepositField.state.name(), DepositState.unregistered.name());
        status.put(DepositField.actionRequest.name(), DepositAction.register.name());
        Set<String> nulls = new HashSet<String>();
        for (String key : status.keySet()) {
            if (status.get(key) == null) {
                nulls.add(key);
            }
        }
        for (String key : nulls) {
            status.remove(key);
        }
        this.depositStatusFactory.save(depositPid.getUUID(), status);
        log.info(status.toString());
    }

}