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
