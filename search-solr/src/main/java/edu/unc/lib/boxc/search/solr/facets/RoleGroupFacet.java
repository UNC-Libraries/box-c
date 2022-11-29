package edu.unc.lib.boxc.search.solr.facets;

import org.apache.solr.client.solrj.response.FacetField;

import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import edu.unc.lib.boxc.search.api.exceptions.InvalidFacetException;

/**
 *
 * @author bbpennel
 *
 */
public class RoleGroupFacet extends GenericFacet {

    private String roleName;
    private String roleUri;
    private String groupName;

    public RoleGroupFacet(String fieldName, FacetField.Count countObject) {
        super(fieldName, countObject);
        this.setFieldName(fieldName);

        if (countObject != null && countObject.getName() != null) {
            this.setValue(countObject.getName());
        }
    }

    public RoleGroupFacet(String fieldName, String facetValue) {
        super(fieldName, facetValue);
        this.setFieldName(fieldName);
        if (facetValue != null) {
            this.setValue(facetValue);
        }
    }

    public RoleGroupFacet(RoleGroupFacet facet) {
        super(facet);
        this.roleName = facet.roleName;
        this.roleUri = facet.roleUri;
        this.groupName = facet.groupName;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
        if (this.value != null && !this.value.trim().isEmpty()) {
            String[] components = this.value.split("\\|");
            if (components.length < 2) {
                throw new InvalidFacetException("Facet value " + value +
                        " was invalid, it must contain a role URI and a group.");
            }

            this.roleName = components[0];
            this.roleUri = JDOMNamespaceUtil.CDR_ROLE_NS.getURI() + this.roleName;

            this.groupName = components[1];
            this.displayValue = this.groupName + " as " + this.roleName;
        }
    }
}
