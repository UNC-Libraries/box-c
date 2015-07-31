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
package edu.unc.lib.dl.cdr.services.rest.modify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.update.CDRMetadataBulkUIP;
import edu.unc.lib.dl.update.UIPException;
import edu.unc.lib.dl.update.UIPProcessor;
import edu.unc.lib.dl.update.UpdateException;

/**
 * Controller which accepts CDR bulk metadata update packages and begins update operations
 * 
 * @author bbpennel
 * @date Jul 30, 2015
 */
@Controller
public class ImportXMLController {
	private static final Logger log = LoggerFactory.getLogger(ImportXMLController.class);
	
	@Autowired
	private UIPProcessor cdrUIPProcessor;
	private Path dataPath;
	
	@PostConstruct
	public void init() throws IOException {
		dataPath = Paths.get("/opt/data/metadataImport/");
	}
	
	@RequestMapping(value = "importXML", method = RequestMethod.POST)
	public @ResponseBody Object importXML(@RequestParam("file") MultipartFile xmlFile, HttpServletRequest request) throws Exception {
		Map<String, String> result = new HashMap<>();
		
		File importFile = File.createTempFile("import", ".xml", dataPath.toFile());
		FileUtils.writeByteArrayToFile(importFile, xmlFile.getBytes());
		
		String emailAddress = request.getHeader("mail");
		if (emailAddress == null) {
			emailAddress = GroupsThreadStore.getUsername() + "@email.unc.edu";
		}
		
		ImportRunnable runnable = new ImportRunnable(emailAddress, GroupsThreadStore.getUsername(),
				GroupsThreadStore.getGroups(), importFile);

		new Thread(runnable).start();
		
		
		result.put("message", "Import of metadata has begun, " + emailAddress
				+ " will be emailed when the update completes");
		
		return result;
	}
	
	private class ImportRunnable implements Runnable {
		private final String email;
		private final String username;
		private final AccessGroupSet groups;
		private final File importFile;

		@Override
		public void run() {
			try {
				GroupsThreadStore.storeGroups(groups);
				CDRMetadataBulkUIP bulkUIP = new CDRMetadataBulkUIP(email, username, groups,
						importFile);
				
				cdrUIPProcessor.process(bulkUIP);
				
				// Delete the import file if it was successful
				importFile.delete();
			} catch (UpdateException | UIPException e) {
				log.error("Failed to update metadata for {}", username, e);
			} finally {
				GroupsThreadStore.clearStore();
			}
		}

		public ImportRunnable(String email, String username, AccessGroupSet groups, File importFile) {
			this.email = email;
			this.username = username;
			this.groups = groups;
			this.importFile = importFile;
		}
		
	}
}
