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
package edu.unc.lib.boxc.search.api.facets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An individual hierarchical facet field, containing any number of specific hierarchical facet values in it.
 *
 * @author bbpennel
 */
public class FacetFieldObject {
    private String name;
    private List<SearchFacet> values;
    private boolean hierarchical;

    public FacetFieldObject(String name, List<SearchFacet> values) {
        this.name = name;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SearchFacet> getValues() {
        return values;
    }

    public void setValues(Collection<SearchFacet> values) {
        this.values = new ArrayList<SearchFacet>(values);
    }

    public boolean isHierarchical() {
        return hierarchical;
    }

    public void setHierarchical(boolean hierarchical) {
        this.hierarchical = hierarchical;
    }

}
