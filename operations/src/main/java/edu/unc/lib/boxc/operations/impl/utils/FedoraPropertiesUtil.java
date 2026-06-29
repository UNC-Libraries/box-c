package edu.unc.lib.boxc.operations.impl.utils;

import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import org.apache.jena.rdf.model.Property;

/**
 * Util for retrieving Fedora properties from a repository object
 */
public class FedoraPropertiesUtil {
    private FedoraPropertiesUtil() {
    }

    /**
     * Gets the string value of the fedora property or returns null if property isn't found
     * @param repositoryObject
     * @param property
     * @return
     */
    public static String getValue(RepositoryObject repositoryObject, Property property) {
        var propertyValue = repositoryObject.getResource().getProperty(property);
        if (propertyValue == null) {
            return null;
        }
        return propertyValue.getString();
    }
}
