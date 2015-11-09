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
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.md5sum;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.SIMPLE;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.staging.Stages;
import edu.unc.lib.staging.StagingException;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.Bag.Format;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.BagHelper;
import gov.loc.repository.bagit.Manifest;
import gov.loc.repository.bagit.utilities.SimpleResult;

/**
 * Transforms bagit bags stored in a staging location into n3 for deposit
 * 
 * @author bbpennel
 * @author daines
 * @date Nov 9, 2015
 */
public class BagIt2N3BagJob extends AbstractDepositJob {
	
	@Autowired
	private Stages stages;
	
	public BagIt2N3BagJob() {
		super();
	}

	public BagIt2N3BagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void runJob() {
		
		Model model = getWritableModel();
		com.hp.hpl.jena.rdf.model.Bag top = model.createBag(getDepositPID().getURI().toString());
		
		Map<String, String> status = getDepositStatus();
		String sourcePath = status.get(DepositField.sourcePath.name());
		
		if (BagHelper.getVersion(new File(sourcePath)) == null) {
			failJob("Can't find BagIt bag", "A BagIt bag could not be found at the source path.");
		}
		
		BagFactory bagFactory = new BagFactory();
		
		File sourceFile = new File(sourcePath);
		Bag bag = bagFactory.createBag(sourceFile);
		
		if (bag.getFormat() != Format.FILESYSTEM) {
			failJob("Unsupported BagIt bag format", "Only filesystem bags are supported.");
		}
		
		// Verify that the bag has all the required parts
		SimpleResult completeResult = bag.verifyComplete();
		if (!bag.verifyComplete().isSuccess()) {
			// Bag did not validate, generate error report and throw exception
			StringBuilder msg = new StringBuilder();
			for (String error : completeResult.getErrorMessages()) {
				msg.append(error).append("\n");
			}
			
			failJob("Unable to normalize bag " + sourcePath + ", it was not complete according to bagit specifications",
					msg.toString());
		}
		
		Collection<BagFile> payload = bag.getPayload();
		
		Property labelProp = dprop(model, DepositRelationship.label);
		Property hasModelProp = fprop(model, FedoraProperty.hasModel);
		Property md5sumProp = dprop(model, md5sum);
		Property locationProp = dprop(model, DepositRelationship.stagingLocation);
		Resource simpleResource = model.createResource(SIMPLE.getURI().toString());
		
		for (BagFile file : payload) {
			String filePath = file.getFilepath();
			
			Map<Manifest.Algorithm, String> checksums = bag.getChecksums(filePath);
			
			Resource fileResource = getFileResource(top, sourcePath, filePath);
			
			// add checksum, size, label
			String filename = filePath.substring(filePath.lastIndexOf("/") + 1);
			model.add(fileResource, labelProp, filename);
			model.add(fileResource, hasModelProp, simpleResource);
			if (checksums.containsKey(Manifest.Algorithm.MD5)) {
				model.add(fileResource, md5sumProp, checksums.get(Manifest.Algorithm.MD5));
			}
			
			// Find staged path for the file
			Path storedPath = Paths.get(sourceFile.getAbsolutePath(), filePath);
			try {
				URI stagedURI = stages.getStagedURI(storedPath.toUri());
				
				if (stagedURI != null) {
					model.add(fileResource, locationProp, stagedURI.toString());
				}
			} catch (StagingException e) {
				failJob(e, "Unable to get staged path for file {}", storedPath);
			}
			
		}
		
	}
	
	private Resource getFileResource(com.hp.hpl.jena.rdf.model.Bag top, String basepath, String filepath) {
		com.hp.hpl.jena.rdf.model.Bag folderBag = getFolderBag(top, basepath, filepath);

		UUID uuid = UUID.randomUUID();
		PID pid = new PID("uuid:" + uuid.toString());

		Resource fileResource = top.getModel().createResource(pid.getURI());
		folderBag.add(fileResource);

		return fileResource;
	}
	
	private com.hp.hpl.jena.rdf.model.Bag getFolderBag(com.hp.hpl.jena.rdf.model.Bag top, String basepath, String filepath) {
		
		Model model = top.getModel();
		
		// find or create a folder resource for the filepath
		String[] pathSegments = filepath.split("/");
		
		// Nothing to do with paths that only have data
		if (pathSegments.length <= 2) {
			return top;
		}
		
		Property labelProp = dprop(model, DepositRelationship.label);
		Property hasModelProp = model.createProperty(FedoraProperty.hasModel.getURI().toString());
		Resource containerResource = model.createResource(CONTAINER.getURI().toString());
		
		com.hp.hpl.jena.rdf.model.Bag currentNode = top;
		
		segmentLoop: for (int i = 1; i < pathSegments.length - 1; i++) {
			String segment = pathSegments[i];
			
			// Search to see if a folder with the same name as this segment exists as a child
			NodeIterator nodeIt = currentNode.iterator();
			while (nodeIt.hasNext()) {
				Resource child = nodeIt.nextNode().asResource();
				
				String label = child.getProperty(labelProp).getString();
				if (label.equals(segment) && child.hasProperty(hasModelProp, containerResource)) {
					// Folder already exists, select it and move on
					currentNode = model.getBag(child);
					nodeIt.close();
					continue segmentLoop;
				}
			}
			
			nodeIt.close();
			// No existing folder was found, create one
			PID pid = new PID("uuid:" + UUID.randomUUID().toString());
			
			com.hp.hpl.jena.rdf.model.Bag childBag = model.createBag(pid.getURI());
			currentNode.add(childBag);
			
			model.add(childBag, labelProp, segment);
			model.add(childBag, hasModelProp, containerResource);
			
			currentNode = childBag;
		}
		
		return currentNode;
	}

	public void setStages(Stages stages) {
		this.stages = stages;
	}
	
}
