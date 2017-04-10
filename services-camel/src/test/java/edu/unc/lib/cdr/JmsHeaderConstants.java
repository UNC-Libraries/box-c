package edu.unc.lib.cdr;

public abstract class JmsHeaderConstants {

	public static final String EVENT_NS = "http://fedora.info/definitions/v4/event#";
	public static final String EVENT_TYPE = "org.fcrepo.jms.eventType";
	public static final String IDENTIFIER = "org.fcrepo.jms.identifier";
	public static final String RESOURCE_TYPE = "org.fcrepo.jms.resourceType";

	public static final String EVENT_CREATE = "ResourceCreation";
	public static final String EVENT_DELETE = "ResourceDeletion";
	public static final String EVENT_MODIFY = "ResourceModification";
	public static final String EVENT_RELOCATE = "ResourceRelocation";
}
