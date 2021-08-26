/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
