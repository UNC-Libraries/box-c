package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.fprop;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.SIMPLE;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractFileServerToBagJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.staging.StagingException;


public class DirectoryIngestJob extends AbstractFileServerToBagJob {
	public DirectoryIngestJob() {
		super();
	}

	public DirectoryIngestJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void runJob() {
		Model model = getWritableModel();
		com.hp.hpl.jena.rdf.model.Bag top = model.createBag(getDepositPID().getURI().toString());
		
		Map<String, String> status = getDepositStatus();
		String sourcePath = status.get(DepositField.sourcePath.name());	
		File sourceFile = new File(sourcePath);
		File[] listOfFiles = sourceFile.listFiles();
		
		Property labelProp = dprop(model, DepositRelationship.label);
		Property hasModelProp = fprop(model, FedoraProperty.hasModel);
		Property locationProp = dprop(model, DepositRelationship.stagingLocation);
		Resource simpleResource = model.createResource(SIMPLE.getURI().toString());
		
		// Turn the bag itself into the top level folder for this deposit
		PID containerPID = new PID("uuid:" + UUID.randomUUID());
		com.hp.hpl.jena.rdf.model.Bag bagFolder = model.createBag(containerPID.getURI());
		model.add(bagFolder, labelProp, status.get(DepositField.fileName.name()));
		model.add(bagFolder, hasModelProp, model.createResource(CONTAINER.getURI().toString()));
		top.add(bagFolder);
		
		addDescription(containerPID, status);
		
		// Add all of the payload objects into the bag folder
		for (File file : listOfFiles) {
			String filePath = file.getName();
			
			Resource fileResource = getFileResource(bagFolder, sourcePath, filePath);
			
			String filename = filePath.substring(filePath.lastIndexOf("/") + 1);
			model.add(fileResource, labelProp, filename);
			model.add(fileResource, hasModelProp, simpleResource);
			
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
}
