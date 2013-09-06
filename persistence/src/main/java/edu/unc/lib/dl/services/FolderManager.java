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
package edu.unc.lib.dl.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.sip.SingleFolderSIP;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.PathUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class FolderManager {
	private static final Log log = LogFactory.getLog(FolderManager.class);
	private DigitalObjectManager digitalObjectManager = null;
	private TripleStoreQueryService tripleStoreQueryService = null;

	/**
	 * Given a repository path string, creates any folders that do not yet exist along this path.
	 * 
	 * @param path
	 *           the desired folder path
	 * @throws IngestException
	 *            if the path cannot be created
	 */
	public PID createPath(String path, String owner, String user) throws IngestException {
		log.debug("attempting to create path: " + path);
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		if (!PathUtil.isValidPathString(path)) {
			throw new IngestException("The proposed path is not valid: " + path);
		}
		List<String> slugs = new ArrayList<String>();
		slugs.addAll(Arrays.asList(path.split("/")));
		slugs.remove(0);
		Stack<String> createFolders = new Stack<String>();
		String containerPath = null;
		for (int i = slugs.size(); i >= 0; i--) {
			String test = "/" + StringUtils.join(slugs.subList(0, i), "/");
			PID pid = this.getTripleStoreQueryService().fetchByRepositoryPath(test);
			if (pid == null) {
				// does not exist yet, must create it
				createFolders.add(slugs.get(i - 1));
			} else {
				List<URI> types = this.getTripleStoreQueryService().lookupContentModels(pid);
				if (!types.contains(ContentModelHelper.Model.CONTAINER.getURI())) {
					StringBuffer s = new StringBuffer();
					for (URI type : types) {
						s.append(type.toString()).append("\t");
					}
					throw new IngestException("The object at the path '" + test
							+ "' is not a folder.  It has these content models:\n" + s.toString());
				}
				containerPath = test;
				break;
			}
		}

		// add folders
		PID lastpid = null;
		while (createFolders.size() > 0) {
			String slug = createFolders.pop();
			SingleFolderSIP sip = new SingleFolderSIP();
			PID containerPID = this.getTripleStoreQueryService().fetchByRepositoryPath(containerPath);
			sip.setContainerPID(containerPID);
			if ("/".equals(containerPath)) {
				containerPath = containerPath + slug;
			} else {
				containerPath = containerPath + "/" + slug;
			}
			sip.setSlug(slug);
			sip.setAllowIndexing(true);
			DepositRecord record = new DepositRecord(user, owner, DepositMethod.Unspecified);
			record.setMessage("creating a new folder path");
			IngestResult result = this.getDigitalObjectManager().addWhileBlocking(sip, record);
			lastpid = result.derivedPIDs.iterator().next();
		}
		return lastpid;
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
