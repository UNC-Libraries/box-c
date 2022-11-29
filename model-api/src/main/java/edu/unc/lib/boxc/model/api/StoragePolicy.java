package edu.unc.lib.boxc.model.api;

/**
 * Storage policy describing how a datastream should be stored and addressed.
 *
 * @author bbpennel
 *
 */
public enum StoragePolicy {
    INTERNAL("Stored within the repository's internally managed datastore."),
    PROXIED("Managed and served via the repository from an external URI."),
    EXTERNAL("Stored, addressed and managed outside of repository."),
    REDIRECTED("Tracked by repository, requests are redirected to an external URI.");

    private final String description;

    private StoragePolicy(String description) {
        this.description = description;
    }

    /**
     * @return description of the storage policy
     */
    public String getDescription() {
        return description;
    }
}
