package edu.unc.lib.boxc.web.sword;

/**
 * ErrorURIRegistry
 * @author bbpennel
 *
 */
public class ErrorURIRegistry {
    public static final String INVALID_INGEST_PACKAGE = "http://cdr.lib.unc.edu/sword/error/InvalidIngestPackage";
    public static final String INGEST_EXCEPTION = "http://cdr.lib.unc.edu/sword/error/IngestException";
    public static final String UPDATE_EXCEPTION = "http://cdr.lib.unc.edu/sword/error/UpdateException";
    public static final String RETRIEVAL_EXCEPTION = "http://cdr.lib.unc.edu/sword/error/RetrievalException";
    public static final String UNSUPPORTED_PACKAGE_TYPE = "http://cdr.lib.unc.edu/sword/error/UnsupportedPackageType";
    public static final String INSUFFICIENT_PRIVILEGES = "http://cdr.lib.unc.edu/sword/error/InsufficientPrivileges";
    public static final String RESOURCE_NOT_FOUND = "http://cdr.lib.unc.edu/sword/error/ResourceNotFound";

    private ErrorURIRegistry() {
    }
}
