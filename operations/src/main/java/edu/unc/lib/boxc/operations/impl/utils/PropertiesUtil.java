package edu.unc.lib.boxc.operations.impl.utils;

import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import org.apache.jena.rdf.model.Property;

public class PropertiesUtil {
    public static String getValue(RepositoryObject repositoryObject, Property property) {
        var propertyValue = repositoryObject.getResource().getProperty(property);
        if (propertyValue == null) {
            return null;
        }
        return propertyValue.getString();
    }
}
