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
package edu.unc.lib.dl.ingest.sip;

import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.dl.ingest.IngestException;

public class SIPProcessorFactory {
	private Map<String, SIPProcessor> sipProcessors = null;

	private Map<String, Class<SIPProcessor>> sipClasses = null;

	/**
	 * Finds an appropriate processor for the given sip.
	 * 
	 * @param sip
	 *           a SIP
	 * @return a processor that can handle the sip
	 */
	public SIPProcessor getSIPProcessor(SubmissionInformationPackage sip) throws IngestException {
		for (String classname : this.sipClasses.keySet()) {
			Class<SIPProcessor> c = this.sipClasses.get(classname);
			if (c.isInstance(sip)) {
				return this.getSipProcessors().get(classname);
			}
		}
		throw new IngestException("Could not find an ingest processor for the given sip.");
	}

	public Map<String, SIPProcessor> getSipProcessors() {
		return sipProcessors;
	}

	public void setSipProcessors(Map<String, SIPProcessor> sipProcessors) {
		this.sipProcessors = sipProcessors;
	}

	public void init() {
		this.sipClasses = new HashMap<String, Class<SIPProcessor>>();
		try {
			for (String classname : this.sipProcessors.keySet()) {
				Class<SIPProcessor> c = extracted(classname);
				this.sipClasses.put(classname, c);
			}
		} catch (ClassNotFoundException e) {
			throw new Error("Cannot initialize ingest processor factory.", e);
		}
	}

	@SuppressWarnings("unchecked")
	private Class<SIPProcessor> extracted(String classname) throws ClassNotFoundException {
		return (Class<SIPProcessor>) Class.forName(classname, false, this.getClass().getClassLoader());
	}
}
