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
import static edu.unc.lib.deposit.work.DepositGraphUtils.fprop;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.label;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.stagingLocation;
import static edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.AGGREGATE_WORK;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.COLLECTION;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.SIMPLE;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
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
public class Simple2N3BagJob extends AbstractDepositJob {

	private static final Logger log = LoggerFactory.getLogger(Simple2N3BagJob.class);

	public Simple2N3BagJob() {
		super();
	}

	public Simple2N3BagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void runJob() {

		// deposit RDF bag
		Model model = getWritableModel();
		Bag depositBag = model.createBag(getDepositPID().getURI().toString());

		// Generate a uuid for the main object
		PID primaryPID = new PID("uuid:" + UUID.randomUUID());

		// Identify the important file from the deposit
		Map<String, String> depositStatus = getDepositStatus();
		String filename = depositStatus.get(DepositField.fileName.name());
		String slug = depositStatus.get(DepositField.depositSlug.name());
		String mimetype = depositStatus.get(DepositField.fileMimetype.name());

		String contentModel = depositStatus.get(hasModel.toString());

		// Create the primary resource as a simple resource
		Resource primaryResource = model.createResource(primaryPID.getURI());

		if (contentModel == null || SIMPLE.equals(contentModel)) {
			populateSimple(model, primaryResource, slug, filename, mimetype);
		} else {
			populateContainer(model, primaryResource, primaryPID, slug, contentModel);
		}

		// Store primary resource as child of the deposit
		depositBag.add(primaryResource);

		if (!this.getDepositDirectory().exists()) {
			log.info("Creating deposit dir {}", this.getDepositDirectory().getAbsolutePath());
			this.getDepositDirectory().mkdir();
		}

		// Add normalization event to deposit record
		recordDepositEvent(Type.NORMALIZATION, "Normalized deposit package from {0} to {1}",
				PackagingType.SIMPLE_OBJECT.getUri(), PackagingType.BAG_WITH_N3.getUri());
	}

	private void populateSimple(Model model, Resource primaryResource, String alabel, String filename,
			String mimetype) {
		File contentFile = new File(this.getDataDirectory(), filename);
		if (!contentFile.exists()) {
			failJob("Failed to find upload file for simple deposit: " + filename,
					contentFile.getAbsolutePath());
		}

		if(alabel == null) alabel = contentFile.getName();
		model.add(primaryResource, dprop(model, label), alabel);
		if (mimetype != null) {
			model.add(primaryResource, dprop(model, DepositRelationship.mimetype), mimetype);
		}

		// Reference the content file as the data file
		try {
			model.add(primaryResource, dprop(model, stagingLocation),
					DepositConstants.DATA_DIR + "/" + UriUtils.encodeUri(contentFile.getName(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			log.error("fail to encode filepath {}", contentFile.getName(), e);
		}
	}

	private Resource populateContainer(Model model, Resource primaryResource, PID primaryPID, String alabel,
			String contentModel) {

		File modsFile = new File(this.getDataDirectory(), "mods.xml");
		if (modsFile.exists()) {
			File descDir = new File(this.getDepositDirectory(), DepositConstants.DESCRIPTION_DIR);
			descDir.mkdir();

			File destinationFile = new File(descDir, primaryPID.getUUID() + ".xml");

			try {
				FileUtils.copyFile(modsFile, destinationFile);
			} catch (IOException e) {
				log.error("Failed to copy descriptive file", e);
			}
		}

		// set the label
		model.add(primaryResource, dprop(model, label), alabel);

		// Set container models depending on the type requested
		model.add(primaryResource, fprop(model, hasModel), model.createResource(CONTAINER.toString()));
		if (COLLECTION.equals(contentModel)) {
			model.add(primaryResource, fprop(model, hasModel), model.createResource(COLLECTION.toString()));
		} else if (AGGREGATE_WORK.equals(contentModel)) {
			model.add(primaryResource, fprop(model, hasModel), model.createResource(AGGREGATE_WORK.toString()));

			// TODO if a file is provided, generate child and mark it as default web object
		}

		return primaryResource;
	}
}
