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

import java.io.File;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.fedora.PID;

public class SingleFolderSIP implements SubmissionInformationPackage {
	private PID containerPID = null;
	private final String label = null;
	private File modsXML = null;
	private Agent owner = null;
	private Agent depositor = null;
	private String slug = null;
	private boolean allowIndexing = true;
	private boolean isCollection = false;

	public boolean isAllowIndexing() {
		return allowIndexing;
	}

	public void setAllowIndexing(boolean allowIndexing) {
		this.allowIndexing = allowIndexing;
	}

	public PID getContainerPID() {
		return containerPID;
	}

	public String getLabel() {
		return label;
	}

	public File getModsXML() {
		return modsXML;
	}

	public Agent getOwner() {
		return this.owner;
	}

	public String getSlug() {
		return slug;
	}

	public void setContainerPID(PID containerPID) {
		this.containerPID = containerPID;
	}

	public void setModsXML(File modsXML) {
		this.modsXML = modsXML;
	}

	public void setOwner(Agent owner) {
		this.owner = owner;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public boolean isCollection() {
		return isCollection;
	}

	public void setCollection(boolean isCollection) {
		this.isCollection = isCollection;
	}

	public void setDepositor(Agent depositor){
		this.depositor = depositor;
	}
	
	@Override
	public Agent getDepositor() {
		return depositor;
	}
}
