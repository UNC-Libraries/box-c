package edu.unc.lib.boxc.operations.impl.destroy;

import org.apache.jena.rdf.model.Property;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Premis;

/**
 * Contains properties managed by Fedora that we want to store in Tombstones
 *
 * @author harring
 *
 */
public enum ServerManagedProperties {
    DIGEST(Premis.hasMessageDigest),
    SIZE(Premis.hasSize);

    private Property property;

    private ServerManagedProperties(Property property) {
        this.property = property;
    }

    public static boolean isServerManagedProperty(Property p) {
        return p.equals(DIGEST.property) || p.equals(SIZE.property);
    }

    public static Property mapToLocalNamespace(Property p) {
        if (p.equals(DIGEST.property)) {
            return Cdr.hasMessageDigest;
        }
        if (p.equals(SIZE.property)) {
            return Cdr.hasSize;
        }
        return p;
    }
}
