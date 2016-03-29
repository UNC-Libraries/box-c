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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.staging.StagingException;

/**
 * Normalizes a simple directory submission into n3 for deposit
 * 
 * @author lfarrell
 */
public class DirectoryToBagJob extends AbstractFileServerToBagJob {
	private static final Logger log = LoggerFactory.getLogger(DirectoryToBagJob.class);
	
	public DirectoryToBagJob() {
		super();
	}

	public DirectoryToBagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void runJob() {
		Model model = getWritableModel();
		Bag depositBag = model.createBag(getDepositPID().getURI().toString());
		
		Map<String, String> status = getDepositStatus();
		String sourcePath = status.get(DepositField.sourcePath.name());
		File sourceFile = new File(sourcePath);
		
		// List all files and directories in the deposit, excluding the base directory
		Collection<File> fileListings = FileUtils.listFilesAndDirs(sourceFile, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
		Iterator<File> filesIt = fileListings.iterator();
		while (filesIt.hasNext()) {
			File file = filesIt.next();
			if (file.equals(sourceFile)) {
				filesIt.remove();
				break;
			}
		}
		
		Property labelProp = dprop(model, DepositRelationship.label);
		Property hasModelProp = fprop(model, FedoraProperty.hasModel);
		Property md5sumProp = dprop(model, md5sum);
		Property locationProp = dprop(model, DepositRelationship.stagingLocation);
		Resource simpleResource = model.createResource(SIMPLE.getURI().toString());
		Resource containerResource = model.createResource(CONTAINER.getURI().toString());

		// Turn the base directory itself into the top level folder for this deposit
		Bag sourceBag = getSourceBag(depositBag, sourceFile);
		
		int i = 0;
		// Add all of the payload objects into the bag folder
		for (File file : fileListings) {
			log.debug("Adding object {}: {}", i++, file.getName());
			
			Boolean isDir = file.isDirectory();
			String checksum = null;

			Path filePath = sourceFile.toPath().getParent().relativize(file.toPath());
			String filePathString = filePath.toString();
			String filename = filePath.getFileName().toString();
			
			if (!isDir) {
				Resource fileResource = getFileResource(sourceBag, filePathString);
				model.add(fileResource, labelProp, filename);
				
				String fullPath = file.toString();
				
				try {
					checksum = DigestUtils.md5Hex(new FileInputStream(fullPath));
				} catch (IOException e) {
					failJob(e, "Unable to compute checksum. File not found at  {}", fullPath);
				}
				
				model.add(fileResource, hasModelProp, simpleResource);
				model.add(fileResource, md5sumProp, checksum);
				
				// Find staged path for the file
				Path storedPath = Paths.get(file.getAbsolutePath());
				try {
					URI stagedURI = stages.getStagedURI(storedPath.toUri());
					
					if (stagedURI != null) {
						model.add(fileResource, locationProp, stagedURI.toString());
					}
				} catch (StagingException e) {
					failJob(e, "Unable to get staged path for file {}", storedPath);
				}
			} else {
				Bag folderBag = getFolderBag(sourceBag, filePathString);
				model.add(folderBag, labelProp, filename);
				model.add(folderBag, hasModelProp, containerResource);
			}
		}
	}
}