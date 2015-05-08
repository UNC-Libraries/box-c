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
package edu.unc.lib.deposit.work;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * Sends messages to messaging service indicating which objects were successfully ingested as a part of a deposit
 * 
 * @author bbpennel
 * @date May 5, 2015
 */
public class DepositMessageHandler {

	@Autowired
	private OperationsMessageSender operationsMessageSender;
	@Autowired
	private DepositStatusFactory depositStatusFactory;
	@Autowired
	private Dataset dataset;

	public void sendDepositMessage(String depositUUID) {
		PID depositPID = new PID("uuid:" + depositUUID);

		// Retrieve the list of top level PIDs which were ingested
		List<PID> ingestedPIDs = new ArrayList<>();
		try {
			dataset.begin(ReadWrite.READ);
			Model model = dataset.getNamedModel(depositPID.getURI()).begin();
			Bag depositBag = model.getBag(depositPID.getURI());

			List<String> ingested = new ArrayList<>();
			DepositGraphUtils.walkChildrenDepthFirst(depositBag, ingested, false);
			for (String id : ingested) {
				ingestedPIDs.add(new PID(id));
			}
		} finally {
			if (dataset.isInTransaction()) {
				dataset.commit();
				dataset.end();
			}
		}

		// Get deposit details
		Map<String, String> depositStatus = depositStatusFactory.get(depositUUID);
		PID destinationPID = new PID(depositStatus.get(DepositField.containerId.name()));
		String depositorName = depositStatus.get(DepositField.depositorName.name());

		operationsMessageSender.sendAddOperation(depositorName, Arrays.asList(destinationPID), ingestedPIDs,
				Collections.<PID>emptyList(), depositUUID);
	}

	public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
		this.operationsMessageSender = operationsMessageSender;
	}

	public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
		this.depositStatusFactory = depositStatusFactory;
	}

	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}
}
