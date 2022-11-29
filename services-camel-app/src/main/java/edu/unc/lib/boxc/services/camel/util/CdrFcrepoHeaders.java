package edu.unc.lib.boxc.services.camel.util;

/**
 * Constants for Apache Camel object enhancement services
 *
 * @author lfarrell
 *
 */
public abstract class CdrFcrepoHeaders {

    public static final String CdrBinaryMimeType = "CdrMimeType";

    public static final String CdrBinaryChecksum = "CdrChecksum";

    public static final String CdrBinaryPidId = "CdrBinaryPidId";

    public static final String CdrObjectType = "CdrObjectType";

    // URI identifying the location of content for a binary
    public static final String CdrBinaryPath = "CdrBinaryPath";

    // URI identifying the location
    public static final String CdrImagePath = "CdrImagePath";

    // File path for a temp file
    public static final String CdrTempPath = "CdrTempPath";

    public static final String CdrUpdateAction = "CdrUpdateAction";

    public static final String CdrEnhancementSet = "CdrEnhancementSet";

    public static final String CdrSolrUpdateAction = "CdrSolrUpdateAction";

    public static final String CdrSolrIndexingPriority = "CdrSolrIndexingPriority";
}
