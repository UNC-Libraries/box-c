/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.fcrepo4;

import static edu.unc.lib.dl.util.RDFModelUtil.TURTLE_MIMETYPE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.riot.Lang;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.util.RDFModelUtil;
import edu.unc.lib.dl.util.URIUtil;

/**
 * Client for interacting with a fedora repository and obtaining objects
 * contained in it
 * 
 * @author bbpennel
 *
 */
public class Repository {
	private String depositRecordBase;
	private String vocabulariesBase;
	private String contentBase;
	private String agentsBase;
	private String policiesBase;

	private FcrepoClient client;

	private String baseHost;

	private String fedoraBase;

	private String authUsername;

	private String authPassword;

	private String authHost;

	private RepositoryObjectFactory repositoryFactory;

	private RepositoryObjectDataLoader repositoryObjectDataLoader;

	/**
	 * Retrieves an existing DepositRecord object
	 * 
	 * @param pid
	 * @return
	 * @throws FedoraException
	 */
	public DepositRecord getDepositRecord(PID pid) throws FedoraException {
		DepositRecord record = new DepositRecord(pid, this, repositoryObjectDataLoader);

		// Verify that the retrieved object is a deposit record
		return record.validateType();
	}

	/**
	 * Creates a new deposit record object with the given uuid.
	 * Properties in the supplied model will be added to the deposit record. 
	 * 
	 * @param pid
	 * @param model
	 * @return
	 * @throws FedoraException
	 */
	public DepositRecord createDepositRecord(PID pid, Model model) throws FedoraException {
		URI depositRecordUri = repositoryFactory.createDepositRecord(pid.getRepositoryUri(), model);
		// Create a new pid just in case fedora didn't agree to the suggested one
		PID newPid = PIDs.get(depositRecordUri);

		DepositRecord depositRecord = new DepositRecord(newPid, this, repositoryObjectDataLoader);
		return depositRecord;
	}

	/**
	 * Mint a PID for a new content object
	 * 
	 * @return PID in the content path
	 */
	public PID mintContentPid() {
		String uuid = UUID.randomUUID().toString();
		String id = URIUtil.join(RepositoryPathConstants.CONTENT_BASE, uuid);

		return PIDs.get(id);
	}

	/**
	 * Retrieves an existing content object, or throws an
	 * ObjectTypeMismatchException if the requested object is not a content
	 * object.
	 * 
	 * @param pid
	 * @return
	 * @throws FedoraException
	 */
	public ContentObject getContentObject(PID pid) throws FedoraException {
		if (!pid.getQualifier().equals(RepositoryPathConstants.CONTENT_BASE)) {
			throw new ObjectTypeMismatchException("Requested object " + pid + " is not a content object.");
		}

		// No component path provided, the object requested should be a top
		// level object
		if (StringUtils.isEmpty(pid.getComponentPath())) {
			try (FcrepoResponse response = getClient().get(pid.getRepositoryUri())
					.accept(TURTLE_MIMETYPE)
					.perform()) {

				Model model = ModelFactory.createDefaultModel();
				model.read(response.getBody(), null, Lang.TURTLE.getName());

				Resource resc = model.getResource(pid.getRepositoryPath());

				String etag = response.getHeaderValue("ETag");

				if (resc.hasProperty(RDF.type, Cdr.Work)) {
					return getWorkObject(pid, model, etag);
				}
				if (resc.hasProperty(RDF.type, Cdr.FileObject)) {
					return getFileObject(pid, model, etag);
				}

			} catch (IOException e) {
				throw new FedoraException("Failed to read model for " + pid, e);
			} catch (FcrepoOperationFailedException e) {
				throw ClientFaultResolver.resolve(e);
			}
		}

		throw new ObjectTypeMismatchException("Requested object " + pid + " is not a content object.");
	}

	/**
	 * Retrieves an existing WorkObject
	 * 
	 * @param pid
	 * @return
	 * @throws FedoraException
	 */
	public WorkObject getWorkObject(PID pid) throws FedoraException {
		return getWorkObject(pid, null, null);
	}

	protected WorkObject getWorkObject(PID pid, Model model, String etag) {
		WorkObject workObj = new WorkObject(pid, this, repositoryObjectDataLoader);
		workObj.storeModel(model);
		workObj.setEtag(etag);

		return workObj.validateType();
	}

	/**
	 * Creates a new WorkObject with the given pid
	 * 
	 * @param pid
	 * @return
	 * @throws FedoraException
	 */
	public WorkObject createWorkObject(PID pid) throws FedoraException {
		return createWorkObject(pid, null);
	}

	/**
	 * Creates a new WorkObject with the given pid and properties.
	 * 
	 * @param pid
	 * @param model
	 * @return
	 * @throws FedoraException
	 */
	public WorkObject createWorkObject(PID pid, Model model) throws FedoraException {
		URI workUri = repositoryFactory.createWorkObject(pid.getRepositoryUri(), model);
		PID createdPid = PIDs.get(workUri);

		return new WorkObject(createdPid, this, repositoryObjectDataLoader);
	}

	/**
	 * Retrieves an existing FileObject
	 * 
	 * @param pid
	 * @return
	 * @throws FedoraException
	 */
	public FileObject getFileObject(PID pid) throws FedoraException {
		return getFileObject(pid, null, null);
	}

	protected FileObject getFileObject(PID pid, Model model, String etag) throws FedoraException {
		FileObject fileObject = new FileObject(pid, this, repositoryObjectDataLoader);
		fileObject.storeModel(model);
		fileObject.setEtag(etag);

		return fileObject.validateType();
	}

	/**
	 * Creates a new file object with the given PID.
	 * 
	 * @param pid
	 * @param model
	 * @return
	 * @throws FedoraException
	 */
	public FileObject createFileObject(PID pid, Model model) throws FedoraException {
		URI depositRecordUri = repositoryFactory.createFileObject(pid.getRepositoryUri(), model);
		PID newPid = PIDs.get(depositRecordUri);

		return new FileObject(newPid, this, repositoryObjectDataLoader);
	}

	/**
	 * Retrieves the BinaryObject identified by PID
	 * 
	 * @param pid
	 * @return
	 * @throws FedoraException
	 *             if the object retrieved is not a binary or does not exist
	 */
	public BinaryObject getBinary(PID pid) throws FedoraException {
		return getBinary(pid, null);
	}

	protected BinaryObject getBinary(PID pid, Model model) throws FedoraException {
		BinaryObject binary = new BinaryObject(pid, this, repositoryObjectDataLoader);

		// Verify that the retrieved object is a deposit record
		return binary.validateType();
	}

	/**
	 * Creates a binary object at the given path.
	 * 
	 * @param path
	 *            Repository path where the binary will be created
	 * @param slug
	 *            Name in the path for the binary resource. Optional.
	 * @param content
	 *            Input stream containing the binary content for this resource.
	 * @param filename
	 *            Filename of the binary content. Optional.
	 * @param mimetype
	 *            Mimetype of the content. Optional.
	 * @param checksum
	 *            SHA-1 digest of the content. Optional.
	 * @param model
	 *            Model containing additional triples to add to the new binary's
	 *            metadata. Optional
	 * @return A BinaryObject for this newly created resource.
	 * @throws FedoraException
	 */
	public BinaryObject createBinary(URI path, String slug, InputStream content, String filename, String mimetype, String checksum,
			Model model) throws FedoraException {

		URI binaryUri = repositoryFactory.createBinary(path, slug, content, filename, mimetype, checksum, model);

		PID newPid = PIDs.get(binaryUri);

		BinaryObject binary = new BinaryObject(newPid, this, repositoryObjectDataLoader);
		return binary;
	}

	/**
	 * Creates an event for the specified object.
	 * 
	 * @param eventPid
	 *            the PID of the event to add
	 * @param model
	 *            Model containing properties of this event. Must only contain
	 *            the properties for one event.
	 * @return URI of the event created
	 * @throws FedoraException
	 */
	public PremisEventObject createPremisEvent(PID eventPid, Model model) throws FedoraException {

		try (FcrepoResponse response = getClient().put(eventPid.getRepositoryUri())
				.body(RDFModelUtil.streamModel(model), TURTLE_MIMETYPE)
				.perform()) {

			return new PremisEventObject(PIDs.get(response.getLocation()), this, repositoryObjectDataLoader);
		} catch (IOException e) {
			throw new FedoraException("Unable to create premis event for " + eventPid, e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}
	}

	public PremisEventObject getPremisEvent(PID pid) throws FedoraException {
		return new PremisEventObject(pid, this, repositoryObjectDataLoader).validateType();
	}

	/**
	 * Mints a URL for a new event object belonging to the provided parent object 
	 * 
	 * @param parentPid The object which this event will belong to.
	 * @return
	 */
	public String mintPremisEventUrl(PID parentPid) {
		String uuid = UUID.randomUUID().toString();
		String eventUrl = URIUtil.join(parentPid.getRepositoryPath(),
				RepositoryPathConstants.EVENTS_CONTAINER, uuid);
		return eventUrl;
	}

	/**
	 * Get a Model containing the properties held by the object identified by
	 * the given metadataUri
	 * 
	 * @param metadataUri
	 *            Uri for the model to retrieve. For RDF Resources this is just
	 *            the object URI, but for non-RDF Resources this must be to the
	 *            metadata node
	 * @return Model containing the properties held by the object
	 * @throws FedoraException
	 */
	public Model getObjectModel(URI metadataUri) throws FedoraException {
		try (FcrepoResponse response = getClient().get(metadataUri)
				.accept(TURTLE_MIMETYPE)
				.perform()) {

			Model model = ModelFactory.createDefaultModel();
			model.read(response.getBody(), null, Lang.TURTLE.getName());

			return model;
		} catch (IOException e) {
			throw new FedoraException("Failed to read model for " + metadataUri, e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}
	}

	/**
	 * 
	 * @param subject
	 * @param property
	 * @param object
	 */
	public void createRelationship(PID subject, Property property, Resource object) {
		String sparqlUpdate = RDFModelUtil.createSparqlInsert(subject.getRepositoryPath(), property, object);

		InputStream sparqlStream = new ByteArrayInputStream(sparqlUpdate.getBytes(StandardCharsets.UTF_8));

		try (FcrepoResponse response = getClient().patch(subject.getRepositoryUri())
				.body(sparqlStream)
				.perform()) {
		} catch (IOException e) {
			throw new FedoraException("Unable to add relationship to object " + subject.getPid(), e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}
	}

	/**
	 * Add a member to the parent object.
	 * 
	 * @param parent
	 * @param member
	 */
	public void addMember(ContentObject parent, ContentObject member) {
		repositoryFactory.createMemberLink(parent.getPid().getRepositoryUri(),
				member.getPid().getRepositoryUri());
	}

	public String getVocabulariesBase() {
		return vocabulariesBase;
	}

	public String getContentBase() {
		return contentBase;
	}

	public String getContentPath(PID pid) {
		return null;
	}

	public ContentObject getContentObject(String path) {
		return null;
	}

	public String getAgentsBase() {
		return agentsBase;
	}

	public String getPoliciesBase() {
		return policiesBase;
	}

	public String getDepositRecordBase() {
		return depositRecordBase;
	}

	public void setDepositRecordBase(String depositRecordBase) {
		this.depositRecordBase = depositRecordBase;
	}

	public void setVocabulariesBase(String vocabulariesBase) {
		this.vocabulariesBase = vocabulariesBase;
	}

	public void setContentBase(String contentBase) {
		this.contentBase = contentBase;
	}

	public void setAgentsBase(String agentsBase) {
		this.agentsBase = agentsBase;
	}

	public void setPoliciesBase(String policiesBase) {
		this.policiesBase = policiesBase;
	}

	public String getBaseHost() {
		return baseHost;
	}

	public void setBaseHost(String baseHost) {
		this.baseHost = baseHost;
	}

	public String getFedoraBase() {
		return fedoraBase;
	}

	public void setFedoraBase(String fedoraBase) {
		this.fedoraBase = fedoraBase;
	}

	public String getAuthUsername() {
		return authUsername;
	}

	public void setAuthUsername(String authUsername) {
		this.authUsername = authUsername;
	}

	public String getAuthPassword() {
		return authPassword;
	}

	public void setAuthPassword(String authPassword) {
		this.authPassword = authPassword;
	}

	public String getAuthHost() {
		return authHost;
	}

	public void setAuthHost(String authHost) {
		this.authHost = authHost;
	}

	public void setClient(FcrepoClient client) {
		this.client = client;
	}

	public FcrepoClient getClient() {

		if (client == null) {
			client = FcrepoClient.client().credentials(authUsername, authPassword).authScope(authHost)
					.throwExceptionOnFailure().build();
		}
		return client;
	}

	public RepositoryObjectDataLoader getRepositoryObjectDataLoader() {
		return repositoryObjectDataLoader;
	}

	public void setRepositoryObjectDataLoader(RepositoryObjectDataLoader repositoryObjectDataLoader) {
		this.repositoryObjectDataLoader = repositoryObjectDataLoader;
	}

	public RepositoryObjectFactory getRepositoryObjectFactory() {
		return repositoryFactory;
	}

	public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
		this.repositoryFactory = repositoryObjectFactory;
	}
}
