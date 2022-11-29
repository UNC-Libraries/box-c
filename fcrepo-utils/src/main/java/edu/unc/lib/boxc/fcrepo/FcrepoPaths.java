package edu.unc.lib.boxc.fcrepo;

/**
 * Constants for paths related to a Fedora instance
 *
 * @author bbpennel
 */
public class FcrepoPaths {

    private static String baseUri;
    static {
        setBaseUri(System.getProperty("fcrepo.baseUri"));
    }

    private FcrepoPaths() {
    }

    private static void setBaseUri(String uri) {
        baseUri = uri;
        if (!baseUri.endsWith("/")) {
            baseUri += "/";
        }
    }

    /**
     * Get a string of the uri identifying the base of the fedora repository.
     *
     * @return
     */
    public static String getBaseUri() {
        return baseUri;
    }
}
