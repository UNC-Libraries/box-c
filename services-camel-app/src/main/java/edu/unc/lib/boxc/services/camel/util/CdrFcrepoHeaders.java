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

    // URI identifying the location for an image file
    public static final String CdrImagePath = "CdrImagePath";

    // URI identifying the location for an audio file
    public static final String CdrAudioPath = "CdrAudioPath";

    // URI identifying the location for a video file
    public static final String CdrVideoPath = "CdrVideoPath";

    // File path for a temp file
    public static final String CdrTempPath = "CdrTempPath";

    // Flag indicating that the CdrImagePath is a temporary file and should be cleaned up
    public static final String CdrImagePathCleanup = "CdrImagePathIsTemp";

    public static final String CdrUpdateAction = "CdrUpdateAction";

    public static final String CdrEnhancementSet = "CdrEnhancementSet";

    public static final String CdrSolrUpdateAction = "CdrSolrUpdateAction";

    public static final String CdrSolrIndexingPriority = "CdrSolrIndexingPriority";
}
