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
package edu.unc.lib.dl.ui.util;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.search.solr.util.AbstractSettings;
import edu.unc.lib.dl.security.access.AccessType;

/**
 * Settings object containing access control properties
 * @author bbpennel
 *
 */
public class AccessControlSettings extends AbstractSettings {
	private Set<String> adminDatastreams;
	private Set<String> surrogateDatastreams;
	private Set<String> fileDatastreams;
	private Set<String> recordDatastreams;
	private String adminGroup;
	private String publicGroup;
	
	public AccessControlSettings(){
		
	}
	
	@Autowired(required = true)
	public void setProperties(Properties properties){
		this.adminDatastreams = this.getUnmodifiableSetFromProperty("access.datastream.admin", new HashSet<String>(), properties, ",");
		this.surrogateDatastreams = this.getUnmodifiableSetFromProperty("access.datastream.surrogate", new HashSet<String>(), properties, ",");
		this.fileDatastreams = this.getUnmodifiableSetFromProperty("access.datastream.file", new HashSet<String>(), properties, ",");
		this.recordDatastreams = this.getUnmodifiableSetFromProperty("access.datastream.record", new HashSet<String>(), properties, ",");
		this.setAdminGroup(properties.getProperty("access.group.admin", ""));
		this.setPublicGroup(properties.getProperty("access.group.public", ""));
	}

	public Set<String> getAdminDatastreams() {
		return adminDatastreams;
	}

	public void setAdminDatastreams(Set<String> adminDatastreams) {
		this.adminDatastreams = adminDatastreams;
	}

	public Set<String> getSurrogateDatastreams() {
		return surrogateDatastreams;
	}

	public void setSurrogateDatastreams(Set<String> surrogateDatastreams) {
		this.surrogateDatastreams = surrogateDatastreams;
	}

	public Set<String> getFileDatastreams() {
		return fileDatastreams;
	}

	public void setFileDatastreams(Set<String> fileDatastreams) {
		this.fileDatastreams = fileDatastreams;
	}

	public Set<String> getRecordDatastreams() {
		return recordDatastreams;
	}

	public void setRecordDatastreams(Set<String> recordDatastreams) {
		this.recordDatastreams = recordDatastreams;
	}

	public String getAdminGroup() {
		return adminGroup;
	}

	public void setAdminGroup(String adminGroup) {
		this.adminGroup = adminGroup;
	}

	public String getPublicGroup() {
		return publicGroup;
	}

	public void setPublicGroup(String publicGroup) {
		this.publicGroup = publicGroup;
	}
	
	/**
	 * Returns the access type that applies to the datastream provided.
	 * @param datastream
	 * @return
	 */
	public AccessType getAccessType(String datastream){
		if (fileDatastreams.contains(datastream)){
			return AccessType.FILE;
		} else if (surrogateDatastreams.contains(datastream)){
			return AccessType.SURROGATE;
		} else if (adminDatastreams.contains(datastream)){
			return AccessType.ADMIN;
		} else if (recordDatastreams.contains(datastream)){
			return AccessType.RECORD;
		}
		return AccessType.NONE;
	}
}
