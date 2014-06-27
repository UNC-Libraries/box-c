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
package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.label;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.stagingLocation;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * Normalizes a simple deposit object into an N3 deposit structure.
 *
 * Expects to receive a single file in the data directory, as referenced in deposit status.
 *
 * @author count0
 * @date Jun 20, 2014
 */
public class Simple2N3BagJob extends AbstractDepositJob implements Runnable {
	
	public Simple2N3BagJob() {
		super();
	}

	public Simple2N3BagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}
	
	@Override
	public void run() {

		// deposit RDF bag
		Model model = ModelFactory.createDefaultModel();
		Bag depositBag = model.createBag(getDepositPID().getURI().toString());

		// Generate a uuid for the main object
		PID primaryPID = new PID("uuid:" + UUID.randomUUID());
		Resource primaryResource;

		// Identify the important file from the deposit
		Map<String, String> depositStatus = getDepositStatus();
		String filename = depositStatus.get(DepositField.fileName.name());
		String label = depositStatus.get(DepositField.depositSlug.name());
		
		File contentFile = new File(this.getDataDirectory(), filename);
		if(!contentFile.exists()) {
			failJob(Type.NORMALIZATION, "Failed to find upload file for simple deposit: "+filename, contentFile.getAbsolutePath());
		}

		// Simple object with the content as its source data
		primaryResource = populateSimple(model, primaryPID, contentFile, label);

		// Store primary resource as child of the deposit
		depositBag.add(primaryResource);

		// Save the model to the n3 file
		saveModel(model, DepositConstants.MODEL_FILE);

		// Add normalization event to deposit record
		recordDepositEvent(Type.NORMALIZATION, "Normalized deposit package from {0} to {1}",
				PackagingType.PROQUEST_ETD.getUri(), PackagingType.BAG_WITH_N3.getUri());
	}

	private Resource populateSimple(Model model, PID primaryPID, File contentFile, String alabel) {

		// Create the primary resource as a simple resource
		Resource primaryResource = model.createResource(primaryPID.getURI());

		// use the filename as the label
		if(alabel == null) alabel = contentFile.getName();
		model.add(primaryResource, dprop(model, label), alabel);

		// Reference the content file as the data file
		model.add(primaryResource, dprop(model, stagingLocation), DepositConstants.DATA_DIR + "/" + contentFile.getName());

		return primaryResource;
	}
}
