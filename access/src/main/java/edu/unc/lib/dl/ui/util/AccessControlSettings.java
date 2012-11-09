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

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.search.solr.util.AbstractSettings;

/**
 * Settings object containing access control properties
 * @author bbpennel
 *
 */
public class AccessControlSettings extends AbstractSettings {
	/*private Set<Datastream> adminDatastreams;
	private Set<Datastream> surrogateDatastreams;
	private Set<Datastream> fileDatastreams;
	private Set<Datastream> recordDatastreams;*/
	private String adminGroup;
	private String publicGroup;
	
	public AccessControlSettings(){
		
	}
	
	@Autowired(required = true)
	public void setProperties(Properties properties){
		//this.adminDatastreams = this.getUnmodifiableDatastreamSetFromProperty("access.datastream.admin", new HashSet<Datastream>(), properties, ",");
		//this.surrogateDatastreams = this.getUnmodifiableDatastreamSetFromProperty("access.datastream.surrogate", new HashSet<Datastream>(), properties, ",");
		//this.fileDatastreams = this.getUnmodifiableDatastreamSetFromProperty("access.datastream.file", new HashSet<Datastream>(), properties, ",");
		//this.recordDatastreams = this.getUnmodifiableDatastreamSetFromProperty("access.datastream.record", new HashSet<Datastream>(), properties, ",");
		this.setAdminGroup(properties.getProperty("access.group.admin", ""));
		this.setPublicGroup(properties.getProperty("access.group.public", ""));
	}
	
	protected Set<Datastream> getUnmodifiableDatastreamSetFromProperty(String propertyName, Set<Datastream> c, Properties properties, String delimiter){
		populateDatastreamCollectionFromProperty(propertyName, c, properties, delimiter);
		return Collections.unmodifiableSet(c);
	}
	
	protected void populateDatastreamCollectionFromProperty(String propertyName, Collection<Datastream> c, 
			Properties properties, String delimiter){
		String value = properties.getProperty(propertyName, null);
		if (value != null){
			String searchable[] = value.split(delimiter);
			for (String field: searchable){
				c.add(new Datastream(field));
			}
		}
	}

	/*public Set<Datastream> getAdminDatastreams() {
		return adminDatastreams;
	}

	public void setAdminDatastreams(Set<Datastream> adminDatastreams) {
		this.adminDatastreams = adminDatastreams;
	}

	public Set<Datastream> getSurrogateDatastreams() {
		return surrogateDatastreams;
	}

	public void setSurrogateDatastreams(Set<Datastream> surrogateDatastreams) {
		this.surrogateDatastreams = surrogateDatastreams;
	}

	public Set<Datastream> getFileDatastreams() {
		return fileDatastreams;
	}

	public void setFileDatastreams(Set<Datastream> fileDatastreams) {
		this.fileDatastreams = fileDatastreams;
	}

	public Set<Datastream> getRecordDatastreams() {
		return recordDatastreams;
	}

	public void setRecordDatastreams(Set<Datastream> recordDatastreams) {
		this.recordDatastreams = recordDatastreams;
	}*/

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
	/*public AccessType getAccessType(String datastream){
		Datastream ds = new Datastream(datastream);
		if (fileDatastreams.contains(ds)){
			return AccessType.FILE;
		} else if (surrogateDatastreams.contains(ds)){
			return AccessType.SURROGATE;
		} else if (adminDatastreams.contains(ds)){
			return AccessType.ADMIN;
		} else if (recordDatastreams.contains(ds)){
			return AccessType.RECORD;
		}
		return AccessType.NONE;
	}*/
}
