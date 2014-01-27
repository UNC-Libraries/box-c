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

import net.greghaines.jesque.Config;
import net.greghaines.jesque.Job;
import net.greghaines.jesque.client.Client;
import net.greghaines.jesque.client.ClientImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.util.DepositReportingUtil;
import edu.unc.lib.dl.fedora.PID;

public abstract class AbstractDepositHandler implements DepositHandler {
	@Autowired
	protected DepositReportingUtil depositReportingUtil;
	@Autowired
	private File bagsDirectory;
	@Autowired
	private Config jesqueConfig = null;
	@Autowired
	private Collection<String> overridePermissionGroups = null;
	
	public Config getJesqueConfig() {
		return jesqueConfig;
	}

	public void setJesqueConfig(Config jesqueConfig) {
		this.jesqueConfig = jesqueConfig;
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
	
	/**
	 * Adds the bag to the ingest resque
	 * @param bag
	 */
	protected void queueForIngest(File bag, PID depositId) {
		Job job = new Job("NormalizeBag", bag, depositId.getURI()); // job to schedule bag processing
		final Client client = new ClientImpl(getJesqueConfig());
		client.enqueue("Deposit", job);
		client.end();
	}
}