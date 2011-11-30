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
package edu.unc.lib.dl.ingest.aip;

import java.net.URI;
import java.util.List;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class CheckContainerFilter implements AIPIngestFilter {
	private TripleStoreQueryService tripleStoreQueryService = null;

	@Override
	public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip) throws AIPException {
		StringBuffer unknownPaths = new StringBuffer();
		for (PID topPID : aip.getTopPIDs()) {
			PID containerPID = aip.getContainerPlacement(topPID).parentPID;
			PID verifiedContainer = this.getTripleStoreQueryService().verify(containerPID);
			if (verifiedContainer == null) {
				unknownPaths.append("\t").append(containerPID);
			} else {
				List<URI> containerModels = this.getTripleStoreQueryService().lookupContentModels(containerPID);
				if (containerModels == null || !containerModels.contains(ContentModelHelper.Model.CONTAINER.getURI())) {
					throw new AIPException("Object specified as the container for this SIP is not a Container:" + containerPID);
				}
			}
		}
		if (unknownPaths.length() > 0) {
			throw new AIPException("Cannot find Container object specified: " + unknownPaths.toString());
		}
		// aip.setContainerPID(container);
		return aip;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

}
