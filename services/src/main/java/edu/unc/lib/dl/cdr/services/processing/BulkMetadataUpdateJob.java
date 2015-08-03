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
package edu.unc.lib.dl.cdr.services.processing;

import java.io.File;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.update.BulkMetadataUIP;
import edu.unc.lib.dl.update.UIPException;
import edu.unc.lib.dl.update.UIPProcessor;
import edu.unc.lib.dl.update.UpdateException;

/**
 * @author bbpennel
 * @date Jul 31, 2015
 */
public class BulkMetadataUpdateJob implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(BulkMetadataUpdateJob.class);
	
	private UIPProcessor uipProcessor;
	private final String email;
	private final String username;
	private final AccessGroupSet groups;
	private final File importFile;

	public BulkMetadataUpdateJob(String email, String username, Collection<String> groups, String importPath) {
		this.email = email;
		this.username = username;
		this.groups = new AccessGroupSet();
		this.groups.addAll(groups);
		this.importFile = new File(importPath);
	}
	
	@Override
	public void run() {
		try {
			GroupsThreadStore.storeGroups(groups);
			BulkMetadataUIP bulkUIP = new BulkMetadataUIP(email, username, groups,
					importFile);
			
			uipProcessor.process(bulkUIP);
			
			// Delete the import file if it was successful
			importFile.delete();
		} catch (UpdateException | UIPException e) {
			log.error("Failed to update metadata for {}", username, e);
		} finally {
			GroupsThreadStore.clearStore();
		}
	}

	public UIPProcessor getUipProcessor() {
		return uipProcessor;
	}

	public void setUipProcessor(UIPProcessor uipProcessor) {
		this.uipProcessor = uipProcessor;
	}
}
