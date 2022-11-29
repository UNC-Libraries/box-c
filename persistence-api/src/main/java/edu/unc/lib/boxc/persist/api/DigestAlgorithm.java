package edu.unc.lib.boxc.persist.api;

import org.apache.jena.rdf.model.Property;

import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;

/**
 * Digest algorithms supported by bxc
 *
 * @author bbpennel
 */
public enum DigestAlgorithm {
    SHA1("sha1", CdrDeposit.sha1sum),
    MD5("md5", CdrDeposit.md5sum);

    public static final DigestAlgorithm DEFAULT_ALGORITHM = DigestAlgorithm.SHA1;

    private final String name;
    private final Property depositProp;

    private DigestAlgorithm(String name, Property depositProp) {
        this.depositProp = depositProp;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Property getDepositProperty() {
        return depositProp;
    }

    public static DigestAlgorithm getByDepositProperty(Property depositProp) {
        for (DigestAlgorithm alg : values()) {
            if (alg.depositProp.equals(depositProp)) {
                return alg;
            }
        }
        return null;
    }
}
