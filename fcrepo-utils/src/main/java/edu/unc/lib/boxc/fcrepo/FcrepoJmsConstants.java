package edu.unc.lib.boxc.fcrepo;

/**
 * Header constants for fcrepo messages
 * @author bbpennel
 */
public class FcrepoJmsConstants {

    public static final String EVENT_NS = "http://fedora.info/definitions/v4/event#";
    // Fcrepo JMS Headers
    public static final String EVENT_TYPE = "org.fcrepo.jms.eventType";
    public static final String IDENTIFIER = "org.fcrepo.jms.identifier";
    public static final String BASE_URL = "org.fcrepo.jms.baseURL";
    public static final String RESOURCE_TYPE = "org.fcrepo.jms.resourceType";

    public static final String EVENT_CREATE = "ResourceCreation";
    public static final String EVENT_DELETE = "ResourceDeletion";
    public static final String EVENT_MODIFY = "ResourceModification";
    public static final String EVENT_RELOCATE = "ResourceRelocation";

    private FcrepoJmsConstants() {
    }
}
