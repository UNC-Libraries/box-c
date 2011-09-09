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

	public String getFedoraUrl() {
		return fedoraUrl;
	}

	public void setFedoraUrl(String fedoraUrl) {
		this.fedoraUrl = fedoraUrl;
	}
	
}
