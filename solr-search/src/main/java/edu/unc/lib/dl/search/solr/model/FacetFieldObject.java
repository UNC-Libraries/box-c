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
package edu.unc.lib.dl.search.solr.model;

import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacet;
import java.util.ArrayList;
import org.apache.solr.client.solrj.response.FacetField;

/**
 * An individual hierarchical facet field, containing any number of specific hierarchical facet values in it.
 * @author bbpennel
 * $Id: FacetFieldObject.java 2766 2011-08-22 15:29:07Z bbpennel $
 * $URL: https://vcs.lib.unc.edu/cdr/cdr-master/trunk/solr-search/src/main/java/edu/unc/lib/dl/search/solr/model/FacetFieldObject.java $
 */
public class FacetFieldObject  {
	private String name;
	private ArrayList<GenericFacet> values;
	private boolean hierarchical;
	
	public FacetFieldObject(String name, FacetField facetField, boolean hierarchical){
		this.name = name;
		this.values = new ArrayList<GenericFacet>();
		this.hierarchical = hierarchical;
		if (facetField != null){
			for (FacetField.Count value: facetField.getValues()){
				if (hierarchical)
					this.values.add(new HierarchicalFacet(value, name));
				else this.values.add(new GenericFacet(value, name));
			}
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<GenericFacet> getValues() {
		return values;
	}

	public void setValues(ArrayList<GenericFacet> values) {
		this.values = values;
	}

	public boolean isHierarchical() {
		return hierarchical;
	}

	public void setHierarchical(boolean hierarchical) {
		this.hierarchical = hierarchical;
	}
	
	
}
