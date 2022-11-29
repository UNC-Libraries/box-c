package edu.unc.lib.boxc.deposit.impl.mets;

/**
 * METS Profile
 * @author bbpennel
 *
 */
public enum METSProfile {
    CDR_SIMPLE("http://cdr.unc.edu/METS/profiles/Simple");

    private String name;

    METSProfile(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean equals(final String name) {
        return this.name.equals(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
