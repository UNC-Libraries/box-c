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

import java.util.Arrays;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.Datastream;

public class FedoraUtil {
	private String fedoraUrl;
	
	/**
	 * Returns a URL for a specific datastream of the object identified by pid, according
	 * to the RESTful Fedora API.
	 * Example:
	 * <fedoraBaseURL>/objects/uuid:5fdc16d9-8272-41f7-a7da-a953192174df/datastreams/DC/content
	 * @param pid
	 * @param datastream
	 * @return
	 */
	public String getDatastreamUrl(String pid, String datastream){
		StringBuilder url = new StringBuilder();
		//url.append(fedoraUrl).append("/objects/").append(pid).append("/datastreams/").append(datastream).append("/content");
		//Temporarily using a proxy servlet while fedora access control isn't in place
		url.append("content?id=").append(pid).append("&ds=").append(datastream);
		return url.toString(); 
	}
	
	public String getDatastreamUrl(BriefObjectMetadataBean metadata, String datastreamName){
		// Prefer the matching datastream from this object over the same datastream with a different pid prefix
		Datastream preferredDS = getPreferredDatastream(metadata, datastreamName);
		
		if (preferredDS == null)
			return "";
		
		StringBuilder url = new StringBuilder();
		
		if (metadata.getContentTypeFacet() != null && metadata.getContentTypeFacet().size() > 0) {
			String fileExtension = metadata.getContentTypeFacet().get(0).getDisplayValue();
			int extensionIndex = Arrays.binarySearch(new String[]{"doc", "docx", "htm", "html", "pdf", "ppt", "pptx", "rtf", "txt", "xls", "xlsx", "xml"}, fileExtension);
			if (extensionIndex >= 0)
				url.append("indexable");
		}
		
		url.append("content?id=");
		if (preferredDS.getOwner() == null) {
			url.append(metadata.getId());
		} else {
			url.append(preferredDS.getOwner());
		}
		url.append("&ds=").append(preferredDS.getName());
		return url.toString(); 
	}
	
	public static Datastream getPreferredDatastream(BriefObjectMetadataBean metadata, String datastreamName) {
		Datastream preferredDS = null;
		Datastream incomingDS = new Datastream(datastreamName);
		for (Datastream ds: metadata.getDatastreamObjects()){
			if (ds.equals(incomingDS)) {
				preferredDS = ds;
				if ((incomingDS.getOwner() == null && preferredDS.getOwner() == null) || (incomingDS.getOwner() != null && preferredDS.getOwner() != null)){
						break;
				}
			}
		}
		
		return preferredDS;
	}

	public String getFedoraUrl() {
		return fedoraUrl;
	}

	public void setFedoraUrl(String fedoraUrl) {
		this.fedoraUrl = fedoraUrl;
	}
	
}
