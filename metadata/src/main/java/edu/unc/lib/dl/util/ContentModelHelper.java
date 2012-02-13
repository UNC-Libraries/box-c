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

import org.jdom.Namespace;

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
	 * These are the properties that the repository manages in the rels-ext datastream.
	 *
	 * @author count0
	 *
	 */
	public static enum CDRProperty {
		allowIndexing("allowIndexing"), defaultWebData("defaultWebData"), sourceData("sourceData"), indexText("indexText"), onyen(
				"onyen"), slug("slug"), sortOrder("sortOrder"), permitMetadataCreate("permitMetadataCreate"), permitMetadataRead(
				"permitMetadataRead"), permitMetadataUpdate("permitMetadataUpdate"), permitMetadataDelete(
				"permitMetadataDelete"), permitOriginalsCreate("permitOriginalsCreate"), permitOriginalsRead(
				"permitOriginalsRead"), permitOriginalsUpdate("permitOriginalsUpdate"), permitOriginalsDelete(
				"permitOriginalsDelete"), permitDerivativesCreate("permitDerivativesCreate"), permitDerivativesRead(
				"permitDerivativesRead"), permitDerivativesUpdate("permitDerivativesUpdate"), permitDerivativesDelete(
				"permitDerivativesDelete"), inheritPermissions("inheritPermissions"), hasSourceMimeType("hasSourceMimeType"),
				hasSourceFileSize("hasSourceFileSize"), hasSurrogate("hasSurrogate"), thumb("thumb"), derivedJP2("derivedJP2"),
				techData("techData"), originalDeposit("originalDeposit");
		private URI uri;

		CDRProperty(String suffix) {
			try {
				this.uri = new URI(JDOMNamespaceUtil.CDR_NS.getURI() + suffix);
			} catch (URISyntaxException e) {
				Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
				x.initCause(e);
				throw x;
			}
		}

		public URI getURI() {
			return this.uri;
		}

		public boolean equals(String value){
			return this.uri.toString().equals(value);
		}

		@Override
		public String toString() {
			return this.uri.toString();
		}
	}

	/**
	 * These are the entailed relationships that the repository infers between objects.
	 *
	 * @author count0
	 *
	 */
	public static enum EntailedRelationship {
		isMemberOfCollection(JDOMNamespaceUtil.RELSEXT_NS, "isMemberOfCollection");
		private URI uri;

		EntailedRelationship(Namespace ns, String suffix) {
			try {
				this.uri = new URI(ns.getURI() + suffix);
			} catch (URISyntaxException e) {
				Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
				x.initCause(e);
				throw x;
			}
		}

		public URI getURI() {
			return this.uri;
		}

		public boolean equals(String value){
			return this.uri.toString().equals(value);
		}

		@Override
		public String toString() {
			return this.uri.toString();
		}
	}

	public static enum Fedora_PID {
		CONTENT_MODEL("ContentModel-3.0"), FEDORA_OBJECT("FedoraObject-3.0"), SERVICE_DEFINITION("ServiceDefinition-3.0"), SERVICE_DEPLOYMENT(
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
	 * These are the properties that the repository manages in the rels-ext datastream.
	 *
	 * @author count0
	 *
	 */
	public static enum FedoraProperty {
		Active(JDOMNamespaceUtil.FEDORA_MODEL_NS, "Active"), hasModel(JDOMNamespaceUtil.FEDORA_MODEL_NS, "hasModel"), label(
				JDOMNamespaceUtil.FEDORA_MODEL_NS, "label"), state(JDOMNamespaceUtil.FEDORA_MODEL_NS, "state"), ;
		private URI uri;

		FedoraProperty(Namespace ns, String suffix) {
			try {
				this.uri = new URI(ns.getURI() + suffix);
			} catch (URISyntaxException e) {
				Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
				x.initCause(e);
				throw x;
			}
		}

		public URI getURI() {
			return this.uri;
		}

		public boolean equals(String value){
			return this.uri.toString().equals(value);
		}

		@Override
		public String toString() {
			return this.uri.toString();
		}
	}

	/**
	 * These are the content model object types that the repository code has to be able to identify and reference.
	 *
	 * @author count0
	 *
	 */
	public static enum Model {
		COLLECTION("Collection"), CONTAINER("Container"), GROUPAGENT("GroupAgent"), PERSONAGENT("PersonAgent"), SIMPLE(
				"Simple"), SOFTWAREAGENT("SoftwareAgent"), PRESERVEDOBJECT("PreservedObject"), JP2DERIVEDIMAGE(
				"JP2DerivedImage");

		private URI uri;
		private PID pid;

		Model(String suffix) {
			try {
				this.uri = new URI(NamespaceConstants.CDR_BASEMODEL_URI + suffix);
				this.pid = new PID(NamespaceConstants.CDR_BASEMODEL_URI + suffix);
			} catch (URISyntaxException e) {
				Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
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

		public boolean equals(String value){
			return this.uri.toString().equals(value);
		}

		public boolean equalsPID(String value){
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
		contains(JDOMNamespaceUtil.CDR_NS, "contains"), member(JDOMNamespaceUtil.CDR_NS, "member"), owner(
				JDOMNamespaceUtil.CDR_NS, "owner");
		private URI uri;

		Relationship(Namespace ns, String suffix) {
			try {
				this.uri = new URI(ns.getURI() + suffix);
			} catch (URISyntaxException e) {
				Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
				x.initCause(e);
				throw x;
			}
		}

		public URI getURI() {
			return this.uri;
		}

		public boolean equals(String value){
			return this.uri.toString().equals(value);
		}

		@Override
		public String toString() {
			return this.uri.toString();
		}
	}

	public static enum Datastream {
		RELS_EXT("RELS-EXT"), DATA_FILE("DATA_FILE"), MD_TECHNICAL("MD_TECHNICAL"), IMAGE_JP2000("IMAGE_JP2000"),
		MD_DESCRIPTIVE("MD_DESCRIPTIVE"), DC("DC"), MD_EVENTS("MD_EVENTS"), THUMB_SMALL("THUMB_SMALL"), THUMB_LARGE("THUMB_LARGE"),
		MD_CONTENTS("MD_CONTENTS"), AUDIT("AUDIT");

		private String name;

		Datastream(String name){
			this.setName(name);
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public boolean equals(String value){
			return this.name.equals(value);
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

}
