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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.util.DepositReportingUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

public abstract class AbstractDepositHandler implements DepositHandler {
	@Autowired
	protected DepositReportingUtil depositReportingUtil;
	@Autowired
	private DepositStatusFactory depositStatusFactory;
	@Autowired
	private File depositsDirectory;
	@Autowired
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
			PackagingType type, String depositor, String owner, Map<String, String> extras) {
		Map<String, String> status = new HashMap<String, String>();
		status.putAll(extras);
		
		// generic deposit fields
		status.put(DepositField.uuid.name(), depositPid.getUUID());
		status.put(DepositField.submitTime.name(), String.valueOf(System.currentTimeMillis()));
		status.put(DepositField.fileName.name(), deposit.getFilename());
		String email = SwordConfigurationImpl.getUserEmailAddress();
		status.put(DepositField.depositorName.name(), email != null ? depositor : owner);
		status.put(DepositField.depositorEmail.name(), email != null ? email : owner+"@email.unc.edu");
		status.put(DepositField.containerId.name(), destination.getPid());
		status.put(DepositField.depositMethod.name(), DepositMethod.SWORD13.getLabel());
		status.put(DepositField.packagingType.name(), type.getUri());
		status.put(DepositField.depositMd5.name(), deposit.getMd5());
		status.put(DepositField.depositSlug.name(), deposit.getSlug());
		String permGroups = null;
		if (this.getOverridePermissionGroups() != null) {
			permGroups = StringUtils.join(this.getOverridePermissionGroups(), ',');
		} else {
			permGroups = StringUtils.join(GroupsThreadStore.getGroups(), ',');
		}
		status.put(DepositField.permissionGroups.name(), permGroups);
		status.put(DepositField.status.name(), DepositState.registered.name());
		Set<String> nulls = new HashSet<String>();
		for(String key : status.keySet()) {
			if(status.get(key) == null) nulls.add(key);
		}
		for(String key : nulls) status.remove(key);
		this.depositStatusFactory.save(depositPid.getUUID(), status);
	}

}