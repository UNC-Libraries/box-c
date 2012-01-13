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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Element;

import edu.unc.lib.dl.ingest.IngestException;

/**
 * Ingests a SIP zip archive to the configured Fedora Repository.
 *
 * @author count0
 */
public class AIPIngestPipeline {
	private static Log log = LogFactory.getLog(AIPIngestPipeline.class);

	private List<AIPIngestFilter> preIngestFilters;

	public AIPIngestPipeline() {
	}

	public List<AIPIngestFilter> getPreIngestFilters() {
		return preIngestFilters;
	}

	/**
	 * Takes an IngestContext and runs all the configured filters that make up the ingest processing pipeline. It returns
	 * an IngestContext that is ready for storage in a Fedora repository.
	 *
	 * The repositoryPath may be passed along by the calling context or left null if the SIP either contains a collection
	 * or specifies it's parent in PREMIS. Any combination of repository path strategies will throw an error.
	 *
	 * @param sipzip
	 *           a SIP archive file
	 * @param repositoryPath
	 *           this is the path in which the SIP objects are placed.
	 * @return an XML ingest report
	 */
	public ArchivalInformationPackage processAIP(ArchivalInformationPackage aip) throws IngestException {
		try {
			for (AIPIngestFilter filter : this.preIngestFilters) {
				aip = filter.doFilter(aip);
			}
		} catch (AIPException e) {
			// Log all recognized ingest filter exception cases in the
			// IngestException.
			aip.getEventLogger().logException("There was an unexpected exception.", e);
			Element report = aip.getEventLogger().getAllEvents();
			aip.delete();
			IngestException throwing = new IngestException(e.getMessage(), e);
			throwing.setErrorXML(report);
			throw throwing;
		}
		System.gc();
		return aip;
	}

	public void setPreIngestFilters(List<AIPIngestFilter> preIngestFilters) {
		this.preIngestFilters = preIngestFilters;
	}
}
