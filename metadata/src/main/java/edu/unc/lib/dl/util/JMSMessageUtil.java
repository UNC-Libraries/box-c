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
package edu.unc.lib.dl.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Convenience methods for working with JMS messages
 * @author count0
 *
 */
public class JMSMessageUtil {
    public static final String fedoraMessageNamespace = JDOMNamespaceUtil.CDR_MESSAGE_NS
            .getURI() + "/fedora";
    public static final String cdrMessageNamespace = JDOMNamespaceUtil.CDR_MESSAGE_NS
            .getURI() + "/cdrAdmin";
    public static final String servicesMessageNamespace = JDOMNamespaceUtil.CDR_MESSAGE_NS
            .getURI() + "/services";

    public JMSMessageUtil() {

    }

    public String getFedoraMessageNamespace() {
        return fedoraMessageNamespace;
    }

    public String getCdrMessageNamespace() {
        return cdrMessageNamespace;
    }

    public String getServicesMessageNamespace() {
        return servicesMessageNamespace;
    }

    public static enum FedoraActions {
        MODIFY_OBJECT("modifyObject"), MODIFY_DATASTREAM_BY_VALUE(
                "modifyDatastreamByValue"), MODIFY_DATASTREAM_BY_REFERENCE(
                "modifyDatastreamByReference"), ADD_DATASTREAM("addDatastream"), PURGE_OBJECT(
                "purgeObject"), PURGE_DATASTREAM("purgeDatastream"), ADD_RELATIONSHIP(
                "addRelationship"), PURGE_RELATIONSHIP("purgeRelationship"), INGEST(
                "ingest");

        private String name;
        private final URI uri;

        FedoraActions(String name) {
            this.name = name;
            try {
                this.uri = new URI(fedoraMessageNamespace + "/" + name);
            } catch (URISyntaxException e) {
                throw new RuntimeException("Error creating URI for "
                        + fedoraMessageNamespace + " " + name, e);
            }
        }

        public String getName() {
            return name;
        }

        public boolean equals(String value) {
            return this.uri.toString().equals(value);
        }

        @Override
        public String toString() {
            return this.uri.toString();
        }

        /**
         * Finds an action that matches the full action uri provided.
         *
         * @param value
         * @return
         */
        public static FedoraActions getAction(String value) {
            if (value == null) {
                return null;
            }
            for (FedoraActions action : values()) {
                if (action.equals(value)) {
                    return action;
                }
            }
            return null;
        }
    }

    public static enum CDRActions {
        MOVE("move"), REMOVE("remove"), ADD("add"), REORDER("reorder"), PUBLISH("publish"),
        EDIT_TYPE("editType"), MARK_FOR_DELETION("markForDeletion"), RESTORE_FROM_DELETION("restoreFromDeletion"),
        UPDATE_DESCRIPTION("updateDescription"), SET_AS_PRIMARY_OBJECT("setAsPrimaryObject");

        private String name;
        private final URI uri;

        CDRActions(String name) {
            this.name = name;
            try {
                this.uri = new URI(cdrMessageNamespace + "/" + name);
            } catch (URISyntaxException e) {
                throw new RuntimeException("Error creating URI for "
                        + cdrMessageNamespace + " " + name, e);
            }
        }

        public String getName() {
            return name;
        }

        public boolean equals(String value) {
            return this.uri.toString().equals(value);
        }

        @Override
        public String toString() {
            return this.uri.toString();
        }

        /**
         * Finds an action that matches the full action uri provided.
         *
         * @param value
         * @return
         */
        public static CDRActions getAction(String value) {
            if (value == null) {
                return null;
            }
            for (CDRActions action : values()) {
                if (action.equals(value)) {
                    return action;
                }
            }
            return null;
        }
    }

    public static enum ServicesActions {
        APPLY_SERVICE_STACK("APPLY_SERVICE_STACK"), APPLY_SERVICE(
                "PERFORM_SERVICE");

        private String name;
        private final URI uri;

        ServicesActions(String name) {
            this.name = name;
            try {
                this.uri = new URI(servicesMessageNamespace + "/" + name);
            } catch (URISyntaxException e) {
                throw new RuntimeException("Error creating URI for "
                        + servicesMessageNamespace + " " + name, e);
            }
        }

        public String getName() {
            return name;
        }

        public boolean equals(String value) {
            return this.uri.toString().equals(value);
        }

        @Override
        public String toString() {
            return this.uri.toString();
        }

        /**
         * Finds an action that matches the full action uri provided.
         *
         * @param value
         * @return
         */
        public static ServicesActions getAction(String value) {
            if (value == null) {
                return null;
            }
            for (ServicesActions action : values()) {
                if (action.equals(value)) {
                    return action;
                }
            }
            return null;
        }
    }

    public static String getPid(Document message) {
        if (message == null) {
            return null;
        }
        return message.getRootElement().getChildTextTrim("summary", JDOMNamespaceUtil.ATOM_NS);
    }

    public static String getAction(Document message) {
        if (message == null) {
            return null;
        }
        return message.getRootElement().getChildTextTrim("title",
                JDOMNamespaceUtil.ATOM_NS);
    }

    /**
     * Retrieves the affected datastream field value from the provided message.
     *
     * @param message
     * @return
     */
    public static String getDatastream(Document message) {
        return getCategoryByScheme(message, "fedora-types:dsID");
    }

    /**
     * Retrieves the relationship of triple change messages
     *
     * @param message
     * @return
     */
    public static String getPredicate(Document message) {
        return getCategoryByScheme(message, "fedora-types:relationship");
    }

    /**
     * Retrieves the object component of triples in relationship change
     * messages.
     *
     * @param message
     * @return
     */
    public static String getObject(Document message) {
        return getCategoryByScheme(message, "fedora-types:object");
    }

    public static String getCategoryByScheme(Document message, String scheme) {
        if (message == null) {
            return null;
        }
        List<Element> categories = message.getRootElement().getChildren(
                "category", JDOMNamespaceUtil.ATOM_NS);
        for (Element category : categories) {
            String schemeValue = category.getAttributeValue("scheme");
            if (schemeValue.equals(scheme)) {
                return category.getAttributeValue("term");
            }
        }
        return null;
    }
}
