package edu.unc.lib.boxc.services.camel.util;

/**
 * Constants related to Fedora JMS event types
 *
 * @author bbpennel
 *
 */
public class EventTypes {

    private EventTypes() {
    }

    public final static String EVENT_CREATE = "https://www.w3.org/ns/activitystreams#Create";
    public final static String EVENT_DELETE = "https://www.w3.org/ns/activitystreams#Delete";
    public final static String EVENT_UPDATE = "https://www.w3.org/ns/activitystreams#Update";
    public final static String EVENT_MOVE = "https://www.w3.org/ns/activitystreams#Move";
    public final static String EVENT_FOLLOW = "https://www.w3.org/ns/activitystreams#Follow";
}
