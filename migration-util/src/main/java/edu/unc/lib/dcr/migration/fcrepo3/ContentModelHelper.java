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
package edu.unc.lib.dcr.migration.fcrepo3;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.jdom2.Namespace;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.NamespaceConstants;

/**
 * Temporarily restoring constants for these boxc3 properties
 *
 * @author bbpennel
 */
public class ContentModelHelper {

    /**
     * These are the properties that the repository manages in the rels-ext datastream.
     *
     * @author count0
     *
     */
    public static enum CDRProperty {
        allowIndexing("allowIndexing"),
        defaultWebData("defaultWebData"),
        defaultWebObject("defaultWebObject"),
        sourceData("sourceData"),
        indexText("indexText"),
        onyen("onyen"),
        slug("slug"),
        sortOrder("sortOrder"),
        hasSourceMimeType("hasSourceMimeType"),
        hasSourceFileSize("hasSourceFileSize"),
        hasChecksum("hasChecksum"),
        hasCreatedDate("hasCreatedDate"),
        hasSurrogate("hasSurrogate"),
        thumb("thumb"),
        derivedJP2("derivedJP2"),
        techData("techData"),
        fullText("fullText"),
        depositedOnBehalfOf("depositedOnBehalfOf"),
        depositMethod("depositMethod"),
        depositPackageType("depositPackageType"),
        depositPackageSubType("depositPackageSubType"),
        inheritPermissions("inheritPermissions", JDOMNamespaceUtil.CDR_ACL_NS),
        embargoUntil("embargo-until", JDOMNamespaceUtil.CDR_ACL_NS),
        dataAccessCategory("data-access-category", JDOMNamespaceUtil.CDR_ACL_NS),
        userRole("user-role", JDOMNamespaceUtil.CDR_ACL_NS),
        isPublished("isPublished"),
        isActive("isActive", JDOMNamespaceUtil.CDR_ACL_NS),
        sourceMetadata("sourceMetadata"),
        hasSourceMetadataProfile("hasSourceMetadataProfile"),
        invalidTerm("invalidTerm"),
        dateCreated("dateCreated"),
        indexValidTerms("indexValidTerms"),
        warnInvalidTerms("warnInvalidTerms"),
        replaceInvalidTerms("replaceInvalidTerms"),
        vocabularyType("vocabularyType"),
        vocabularyUri("vocabularyUri"),
        vocabularySelector("vocabularySelector"),
        collectionDefaultView("collectionDefaultView"),
        collectionShowView("collectionShowView");

        private URI uri;
        private Property property;

        CDRProperty(String predicate) {
            this(predicate, JDOMNamespaceUtil.CDR_NS);
        }

        CDRProperty(String predicate, Namespace namespace) {
            try {
                this.uri = new URI(namespace.getURI() + predicate);
                this.property = createProperty(uri.toString());
            } catch (URISyntaxException e) {
                Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
                x.initCause(e);
                throw x;
            }
        }

        public Property getProperty() {
            return property;
        }
    }

    /**
     * These are the properties that the repository manages in the rels-ext datastream.
     *
     * @author count0
     *
     */
    public static enum FedoraProperty {
        hasModel(JDOMNamespaceUtil.FEDORA_MODEL_NS, "hasModel"),
        label(JDOMNamespaceUtil.FEDORA_MODEL_NS, "label"),
        state(JDOMNamespaceUtil.FEDORA_MODEL_NS, "state"),
        disseminates(JDOMNamespaceUtil.FEDORA_VIEW_NS, "disseminates"),
        mimeType(JDOMNamespaceUtil.FEDORA_VIEW_NS, "mimeType"),
        lastModifiedDate(JDOMNamespaceUtil.FEDORA_VIEW_NS, "lastModifiedDate"),
        createdDate(JDOMNamespaceUtil.FEDORA_MODEL_NS, "createdDate"),
        ownerId(JDOMNamespaceUtil.FEDORA_MODEL_NS, "ownerId");

        private URI uri;
        private Property property;

        FedoraProperty(Namespace ns, String fragment) {
            try {
                this.uri = new URI(ns.getURI() + fragment);
                this.property = createProperty(uri.toString());
            } catch (URISyntaxException e) {
                Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
                x.initCause(e);
                throw x;
            }
        }

        public Property getProperty() {
            return property;
        }
    }

    /**
     * These are the content model object types that the repository code has to be able to identify and reference.
     *
     * @author count0
     *
     */
    public static enum ContentModel {
        COLLECTION("Collection"),
        CONTAINER("Container"),
        GROUPAGENT("GroupAgent"),
        PERSONAGENT("PersonAgent"),
        SIMPLE("Simple"),
        SOFTWAREAGENT("SoftwareAgent"),
        PRESERVEDOBJECT("PreservedObject"),
        AGGREGATE_WORK("AggregateWork"),
        DEPOSIT_RECORD("DepositRecord"),
        VOCABULARY("Vocabulary");

        private URI uri;
        private Resource resource;

        ContentModel(String suffix) {
            try {
                this.uri = new URI(NamespaceConstants.CDR_BASEMODEL_URI + suffix);
                this.resource = createResource(uri.toString());
            } catch (URISyntaxException e) {
                Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
                x.initCause(e);
                throw x;
            }
        }

        public Resource getResource() {
            return resource;
        }
    }

    /**
     * These are the relationships that the repository manages between objects.
     *
     * @author count0
     *
     */
    public static enum Relationship {
        contains(JDOMNamespaceUtil.CDR_NS, "contains"),
        member(JDOMNamespaceUtil.CDR_NS, "member"),
        owner(JDOMNamespaceUtil.CDR_NS, "owner"),
        originalDeposit(JDOMNamespaceUtil.CDR_NS, "originalDeposit"),
        depositedBy(JDOMNamespaceUtil.CDR_NS, "depositedBy"),
        removedChild(JDOMNamespaceUtil.CDR_NS, "removedChild");

        private URI uri;
        private Property property;

        Relationship(Namespace ns, String suffix) {
            try {
                this.uri = new URI(ns.getURI() + suffix);
                this.property = createProperty(uri.toString());
            } catch (URISyntaxException e) {
                Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
                x.initCause(e);
                throw x;
            }
        }

        public Property getProperty() {
            return property;
        }
    }

    public enum Bxc3UserRole {
        list("list"),
        accessCopiesPatron("access-copies-patron"),
        metadataPatron("metadata-patron"),
        patron("patron"),
        observer("observer"),
        ingester("ingester"),
        metadataEditor("metadata-editor"),
        processor("processor"),
        curator("curator"),
        administrator("administrator");

        private Property property;

        Bxc3UserRole(String predicate) {
            property = createProperty(JDOMNamespaceUtil.CDR_ROLE_NS.getURI() + predicate);
        }

        public Property getProperty() {
            return property;
        }

        public boolean equals(Property property) {
            return this.property.equals(property);
        }
    }
}
