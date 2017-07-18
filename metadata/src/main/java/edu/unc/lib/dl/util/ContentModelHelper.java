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

import org.jdom2.Namespace;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.NamespaceConstants;

public class ContentModelHelper {

    public static enum Administrative_PID {
        ADMINISTRATOR_GROUP("ADMINISTRATORS"), REPOSITORY("REPOSITORY"), REPOSITORY_MANAGEMENT_SOFTWARE(
                "REPOSITORY_SOFTWARE");

        private static final String prefix = "admin";
        private final PID pid;

        Administrative_PID(String suffix) {
            this.pid = new PID(Administrative_PID.prefix + ":" + suffix);
        }

        public PID getPID() {
            return this.pid;
        }
    }

    /**
     * These are the properties that the repository manages in the rels-ext
     * datastream.
     *
     * @author count0
     *
     */
    public static enum CDRProperty {
        allowIndexing("allowIndexing"), defaultWebData("defaultWebData"), defaultWebObject(
                "defaultWebObject"), sourceData("sourceData"), indexText(
                "indexText"), onyen("onyen"), slug("slug"), sortOrder(
                "sortOrder"), hasSourceMimeType("hasSourceMimeType"), hasSourceFileSize(
                "hasSourceFileSize"), hasChecksum("hasChecksum"), hasCreatedDate(
                "hasCreatedDate"), hasSurrogate("hasSurrogate"), thumb("thumb"), derivedJP2(
                "derivedJP2"), techData("techData"), fullText("fullText"), depositedOnBehalfOf(
                "depositedOnBehalfOf"), depositMethod("depositMethod"), depositPackageType(
                "depositPackageType"), depositPackageSubType(
                "depositPackageSubType"), inheritPermissions(
                "inheritPermissions", JDOMNamespaceUtil.CDR_ACL_NS), embargoUntil(
                "embargo-until", JDOMNamespaceUtil.CDR_ACL_NS), dataAccessCategory(
                "data-access-category", JDOMNamespaceUtil.CDR_ACL_NS), userRole(
                "user-role", JDOMNamespaceUtil.CDR_ACL_NS), isPublished(
                "isPublished"), isActive("isActive",
                JDOMNamespaceUtil.CDR_ACL_NS), sourceMetadata("sourceMetadata"), hasSourceMetadataProfile(
                "hasSourceMetadataProfile"), invalidTerm("invalidTerm"), dateCreated(
                "dateCreated"), indexValidTerms("indexValidTerms"), warnInvalidTerms(
                "warnInvalidTerms"), replaceInvalidTerms("replaceInvalidTerms"), vocabularyType(
                "vocabularyType"), vocabularyUri("vocabularyUri"), vocabularySelector(
                "vocabularySelector"), collectionDefaultView(
                "collectionDefaultView"), collectionShowView(
                "collectionShowView");

        private URI uri;
        private String predicate;
        private Namespace namespace;

        CDRProperty(String predicate) {
            try {
                this.predicate = predicate;
                this.uri = new URI(JDOMNamespaceUtil.CDR_NS.getURI()
                        + predicate);
                this.namespace = JDOMNamespaceUtil.CDR_NS;
            } catch (URISyntaxException e) {
                Error x = new ExceptionInInitializerError(
                        "Cannot initialize ContentModelHelper");
                x.initCause(e);
                throw x;
            }
        }

        CDRProperty(String predicate, Namespace namespace) {
            try {
                this.predicate = predicate;
                this.uri = new URI(namespace.getURI() + predicate);
                this.namespace = namespace;
            } catch (URISyntaxException e) {
                Error x = new ExceptionInInitializerError(
                        "Cannot initialize ContentModelHelper");
                x.initCause(e);
                throw x;
            }
        }

        public URI getURI() {
            return this.uri;
        }

        public String getPredicate() {
            return predicate;
        }

        public Namespace getNamespace() {
            return namespace;
        }

        public boolean equals(String value) {
            return this.uri.toString().equals(value);
        }

        @Override
        public String toString() {
            return this.uri.toString();
        }
    }

    /**
     * These are the entailed relationships that the repository infers between
     * objects.
     *
     * @author count0
     *
     */
    public static enum EntailedRelationship {
        isMemberOfCollection(JDOMNamespaceUtil.RELSEXT_NS,
                "isMemberOfCollection");
        private URI uri;

        EntailedRelationship(Namespace ns, String suffix) {
            try {
                this.uri = new URI(ns.getURI() + suffix);
            } catch (URISyntaxException e) {
                Error x = new ExceptionInInitializerError(
                        "Cannot initialize ContentModelHelper");
                x.initCause(e);
                throw x;
            }
        }

        public URI getURI() {
            return this.uri;
        }

        public boolean equals(String value) {
            return this.uri.toString().equals(value);
        }

        @Override
        public String toString() {
            return this.uri.toString();
        }
    }

    public static enum Fedora_PID {
        CONTENT_MODEL("ContentModel-3.0"), FEDORA_OBJECT("FedoraObject-3.0"), SERVICE_DEFINITION(
                "ServiceDefinition-3.0"), SERVICE_DEPLOYMENT(
                "ServiceDeployment-3.0");

        private static final String prefix = "fedora-system";
        private final PID pid;

        Fedora_PID(String suffix) {
            this.pid = new PID(Fedora_PID.prefix + ":" + suffix);
        }

        public PID getPID() {
            return this.pid;
        }

        @Override
        public String toString() {
            return this.pid.toString();
        }
    };

    /**
     * These are the properties that the repository manages in the rels-ext
     * datastream.
     *
     * @author count0
     *
     */
    public static enum FedoraProperty {
        Active(JDOMNamespaceUtil.FEDORA_MODEL_NS, "Active"), hasModel(
                JDOMNamespaceUtil.FEDORA_MODEL_NS, "hasModel"), label(
                JDOMNamespaceUtil.FEDORA_MODEL_NS, "label"), state(
                JDOMNamespaceUtil.FEDORA_MODEL_NS, "state"), disseminates(
                JDOMNamespaceUtil.FEDORA_VIEW_NS, "disseminates"), mimeType(
                JDOMNamespaceUtil.FEDORA_VIEW_NS, "mimeType"), lastModifiedDate(
                JDOMNamespaceUtil.FEDORA_VIEW_NS, "lastModifiedDate"), createdDate(
                JDOMNamespaceUtil.FEDORA_MODEL_NS, "createdDate");
        private URI uri;
        private Namespace namespace;
        private String fragment;

        FedoraProperty(Namespace ns, String fragment) {
            try {
                this.uri = new URI(ns.getURI() + fragment);
                this.namespace = ns;
                this.fragment = fragment;
            } catch (URISyntaxException e) {
                Error x = new ExceptionInInitializerError(
                        "Cannot initialize ContentModelHelper");
                x.initCause(e);
                throw x;
            }
        }

        // FIXME: should other kinds of properties use this
        // fragment/namespace/uri pattern?

        public String getFragment() {
            return this.fragment;
        }

        public Namespace getNamespace() {
            return this.namespace;
        }

        public URI getURI() {
            return this.uri;
        }

        public boolean equals(String value) {
            return this.uri.toString().equals(value);
        }

        @Override
        public String toString() {
            return this.uri.toString();
        }
    }

    /**
     * These are the content model object types that the repository code has to
     * be able to identify and reference.
     *
     * @author count0
     *
     */
    public static enum Model {
        COLLECTION("Collection"), CONTAINER("Container"), GROUPAGENT(
                "GroupAgent"), PERSONAGENT("PersonAgent"), SIMPLE("Simple"), SOFTWAREAGENT(
                "SoftwareAgent"), PRESERVEDOBJECT("PreservedObject"), AGGREGATE_WORK(
                "AggregateWork"), DEPOSIT_RECORD("DepositRecord"), VOCABULARY(
                "Vocabulary");

        private URI uri;
        private PID pid;

        Model(String suffix) {
            try {
                this.uri = new URI(NamespaceConstants.CDR_BASEMODEL_URI
                        + suffix);
                this.pid = new PID(NamespaceConstants.CDR_BASEMODEL_URI
                        + suffix);
            } catch (URISyntaxException e) {
                Error x = new ExceptionInInitializerError(
                        "Cannot initialize ContentModelHelper");
                x.initCause(e);
                throw x;
            }
        }

        public URI getURI() {
            return this.uri;
        }

        public PID getPID() {
            return this.pid;
        }

        public boolean equals(String value) {
            return this.uri.toString().equals(value);
        }

        public boolean equals(URI value) {
            return this.uri.equals(value);
        }

        public boolean equalsPID(String value) {
            return this.pid.getPid().equals(value);
        }

        @Override
        public String toString() {
            return this.uri.toString();
        }
    }

    /**
     * These are the relationships that the repository manages between objects.
     *
     * @author count0
     *
     */
    public static enum Relationship {
        contains(JDOMNamespaceUtil.CDR_NS, "contains"), member(
                JDOMNamespaceUtil.CDR_NS, "member"), owner(
                JDOMNamespaceUtil.CDR_NS, "owner"), originalDeposit(
                JDOMNamespaceUtil.CDR_NS, "originalDeposit"), depositedBy(
                JDOMNamespaceUtil.CDR_NS, "depositedBy"), removedChild(
                JDOMNamespaceUtil.CDR_NS, "removedChild");
        private URI uri;
        private Namespace namespace;

        Relationship(Namespace ns, String suffix) {
            try {
                this.uri = new URI(ns.getURI() + suffix);
                this.namespace = ns;
            } catch (URISyntaxException e) {
                Error x = new ExceptionInInitializerError(
                        "Cannot initialize ContentModelHelper");
                x.initCause(e);
                throw x;
            }
        }

        public URI getURI() {
            return this.uri;
        }

        public Namespace getNamespace() {
            return namespace;
        }

        public boolean equals(String value) {
            return this.uri.toString().equals(value);
        }

        @Override
        public String toString() {
            return this.uri.toString();
        }
    }

    public static enum ControlGroup {
        INTERNAL("X"), MANAGED("M"), EXTERNAL("E"), REDIRECTED("R");

        private String attributeValue;

        ControlGroup(String attributeValue) {
            this.setAttributeValue(attributeValue);
        }

        public String getAttributeValue() {
            return attributeValue;
        }

        public void setAttributeValue(String attributeValue) {
            this.attributeValue = attributeValue;
        }
    }

    public enum DatastreamCategory {
        ORIGINAL, METADATA, DERIVATIVE, ADMINISTRATIVE
    }

    public static enum Datastream {
        RELS_EXT("RELS-EXT", ControlGroup.INTERNAL, false,
                "Fedora Object-to-Object Relationship Metadata",
                DatastreamCategory.ADMINISTRATIVE), DATA_FILE("DATA_FILE",
                ControlGroup.MANAGED, true, "Original Data File",
                DatastreamCategory.ORIGINAL), MD_TECHNICAL("MD_TECHNICAL",
                ControlGroup.MANAGED, false, "PREMIS Technical Metadata",
                DatastreamCategory.ADMINISTRATIVE), MD_FULL_TEXT(
                "MD_FULL_TEXT", ControlGroup.MANAGED, false,
                "Full text metadata", DatastreamCategory.ADMINISTRATIVE), IMAGE_JP2000(
                "IMAGE_JP2000", ControlGroup.MANAGED, false,
                "Derived JP2000 image", DatastreamCategory.DERIVATIVE), MD_DESCRIPTIVE(
                "MD_DESCRIPTIVE", ControlGroup.INTERNAL, true,
                "Descriptive Metadata", DatastreamCategory.METADATA), DC("DC",
                ControlGroup.INTERNAL, false, "Internal XML Metadata",
                DatastreamCategory.METADATA), MD_EVENTS("MD_EVENTS",
                ControlGroup.MANAGED, false, "PREMIS Events Metadata",
                DatastreamCategory.METADATA), THUMB_SMALL("THUMB_SMALL",
                ControlGroup.MANAGED, false, "Thumbnail Image",
                DatastreamCategory.DERIVATIVE), THUMB_LARGE("THUMB_LARGE",
                ControlGroup.MANAGED, false, "Thumbnail Image",
                DatastreamCategory.DERIVATIVE), MD_CONTENTS("MD_CONTENTS",
                ControlGroup.INTERNAL, false, "List of Contents",
                DatastreamCategory.METADATA), AUDIT("AUDIT",
                ControlGroup.INTERNAL, false, "Audit Trail for this object",
                DatastreamCategory.METADATA), DATA_MANIFEST("DATA_MANIFEST",
                ControlGroup.MANAGED, false, "Deposit Manifest",
                DatastreamCategory.METADATA), MD_SOURCE("MD_SOURCE",
                ControlGroup.MANAGED, false, "Source Metadata",
                DatastreamCategory.ADMINISTRATIVE);

        private String name;
        private ControlGroup controlGroup;
        private boolean versionable;
        private String label;
        private DatastreamCategory category;

        Datastream(String name, ControlGroup controlGroup, boolean versionable,
                String label, DatastreamCategory category) {
            this.setName(name);
            this.setControlGroup(controlGroup);
            this.setVersionable(versionable);
            this.setLabel(label);
            this.setCategory(category);
        }

        public static Datastream getDatastream(String name) {
            if (name == null) {
                return null;
            }

            for (Datastream datastream : values()) {
                if (datastream.equals(name)) {
                    return datastream;
                }

            }
            return null;
        }

        public void setName(String name) {
            this.name = name;
        }

        public DatastreamCategory getCategory() {
            return category;
        }

        private void setCategory(DatastreamCategory category) {
            this.category = category;
        }

        public String getName() {
            return name;
        }

        public ControlGroup getControlGroup() {
            return controlGroup;
        }

        public void setControlGroup(ControlGroup controlGroup) {
            this.controlGroup = controlGroup;
        }

        public boolean isVersionable() {
            return versionable;
        }

        public void setVersionable(boolean versionable) {
            this.versionable = versionable;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public boolean equals(String value) {
            return this.name.equals(value);
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * These are the relationships used within CDR N3 submission packages.
     *
     * @author count0
     *
     */
    public static enum DepositRelationship {
        stagingLocation, mimetype, md5sum, size, createTime, label, originalLocation, hasDatastream, cleanupLocation;

        DepositRelationship() {
            try {
                this.uri = new URI(JDOMNamespaceUtil.DEPOSIT_NS.getURI()
                        + name());
            } catch (URISyntaxException e) {
                throw new Error(e);
            }
        }

        private URI uri;

        public URI getURI() {
            return this.uri;
        }

        @Override
        public String toString() {
            return this.uri.toString();
        }
    }

}
