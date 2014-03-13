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

import org.springframework.beans.factory.annotation.Autowired;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.util.DepositReportingUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.BagInfoTxtExtensions;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagInfoTxt;

public abstract class AbstractDepositHandler implements DepositHandler {
	@Autowired
	protected DepositReportingUtil depositReportingUtil;
	@Autowired
	private DepositStatusFactory depositStatusFactory;
	@Autowired
	private File bagsDirectory;
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

	public File getBagsDirectory() {
		return bagsDirectory;
	}

	public void setBagsDirectory(File bagsDirectory) {
		this.bagsDirectory = bagsDirectory;
	}
	
	public File getNewBagDirectory(String bagName) {
		File f = new File(getBagsDirectory(), bagName);
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
	
	protected void registerDeposit(String uuid, Bag bag) {		
		// create deposit status
		BagInfoTxt info = bag.getBagInfoTxt();
		Map<String, String> status = new HashMap<String, String>();
		status.put(DepositField.bagDate.name(), info.getBaggingDate());
		status.put(DepositField.bagDirectory.name(), bag.getFile().getAbsolutePath());
		status.put(DepositField.contactEmail.name(), info.getContactEmail());
		status.put(DepositField.contactName.name(), info.getContactName());
		status.put(DepositField.containerId.name(), info.get(BagInfoTxtExtensions.CONTAINER_ID));
		status.put(DepositField.depositMethod.name(), info.get(BagInfoTxtExtensions.DEPOSIT_METHOD));
		status.put(DepositField.extIdentifier.name(), info.getExternalIdentifier());
		status.put(DepositField.intSenderDescription.name(), info.getInternalSenderDescription());
		status.put(DepositField.intSenderIdentifier.name(), info.getInternalSenderIdentifier());
		status.put(DepositField.payLoadOctets.name(), info.getPayloadOxum());
		status.put(DepositField.startTime.name(), String.valueOf(System.currentTimeMillis()));
		status.put(DepositField.status.name(), DepositState.registered.name());
		status.put(DepositField.uuid.name(), uuid);
		Set<String> nulls = new HashSet<String>();
		for(String key : status.keySet()) {
			if(status.get(key) == null) nulls.add(key);
		}
		for(String key : nulls) status.remove(key);
		this.depositStatusFactory.save(uuid, status);
	}

}