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
