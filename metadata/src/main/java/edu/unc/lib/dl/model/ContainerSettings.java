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
package edu.unc.lib.dl.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;

import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.RDFXMLUtil;

/**
 * @author bbpennel
 * @date Aug 27, 2015
 */
public class ContainerSettings {

	private String title;
	private String defaultView;
	private List<String> views;
	
	public ContainerSettings(Element foxml) {
		Element relsEl = FOXMLJDOMUtil.getDatastreamContent(Datastream.RELS_EXT, foxml);
		views = RDFXMLUtil.getLiteralValues(relsEl, CDRProperty.collectionShowView.getPredicate(),
				CDRProperty.collectionShowView.getNamespace());
		
		defaultView = RDFXMLUtil.getLiteralValue(relsEl, CDRProperty.collectionDefaultView.getPredicate(),
				CDRProperty.collectionDefaultView.getNamespace());
	}
	
	public ContainerSettings(Map<String, List<String>> triples) {
		views = triples.get(CDRProperty.collectionShowView.toString());
		List<String> defaultViewValues = triples.get(CDRProperty.collectionDefaultView.toString());
		defaultView = defaultViewValues != null? defaultViewValues.get(0) : null;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDefaultView() {
		return defaultView;
	}

	public void setDefaultView(String defaultView) {
		this.defaultView = defaultView;
	}

	public List<String> getViews() {
		return views;
	}

	public void setViews(List<String> views) {
		this.views = views;
	}
	
	public String getViewDisplayName(String view) {
		ContainerView viewEnum = ContainerView.valueOf(view);
		return viewEnum == null? null : viewEnum.getDisplayName();
	}
	
	public Map<String, Map<String, String>> getViewInfo() {
		Map<String, Map<String, String>> result = new HashMap<>();
		
		for (ContainerView view : ContainerView.values()) {
			Map<String, String> entry = new HashMap<>();
			entry.put("displayName", view.getDisplayName());
			entry.put("description", view.getDescription());
			
			result.put(view.name(), entry);
		}
		
		return result;
	}

	public static enum ContainerView {
		METADATA("Overview", "An overview of the contents of the collection and descriptive metadata"),
		STRUCTURE("Structure", "A tree view of the hierachical structure of the collection"),
		DEPARTMENTS("Departments", "A list of the departments associated with objects in this collection"),
		LIST_CONTENTS("List Contents", "A result view of files within this collection with hierarchy flattened");
		
		String displayName;
		String description;
		
		private ContainerView(String displayName, String description) {
			this.displayName = displayName;
			this.description = description;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getDescription() {
			return description;
		}
	}
}
