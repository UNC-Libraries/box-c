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

import java.util.List;

import org.jdom2.Element;

import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.RDFXMLUtil;

/**
 * @author bbpennel
 * @date Aug 27, 2015
 */
public class CollectionSettings {

	private String defaultView;
	private List<String> views;
	
	public CollectionSettings(Element foxml) {
		Element relsEl = FOXMLJDOMUtil.getDatastreamContent(Datastream.RELS_EXT, foxml);
		views = RDFXMLUtil.getLiteralValues(relsEl, CDRProperty.collectionShowView.getPredicate(),
				CDRProperty.collectionShowView.getNamespace());
		
		defaultView = RDFXMLUtil.getLiteralValue(relsEl, CDRProperty.collectionDefaultView.getPredicate(),
				CDRProperty.collectionDefaultView.getNamespace());
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

	public static enum ContainerView {
		METADATA, STRUCTURE, DEPARTMENTS, LIST_CONTENTS;
	}
}
