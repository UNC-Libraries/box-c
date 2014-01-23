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
package edu.unc.lib.dl.fedora;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceFaultException;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.http.CommonsHttpMessageSender;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.types.AddDatastream;
import edu.unc.lib.dl.fedora.types.AddDatastreamResponse;
import edu.unc.lib.dl.fedora.types.AddRelationship;
import edu.unc.lib.dl.fedora.types.AddRelationshipResponse;
import edu.unc.lib.dl.fedora.types.ArrayOfString;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.fedora.types.Export;
import edu.unc.lib.dl.fedora.types.ExportResponse;
import edu.unc.lib.dl.fedora.types.GetDatastream;
import edu.unc.lib.dl.fedora.types.GetDatastreamResponse;
import edu.unc.lib.dl.fedora.types.GetNextPID;
import edu.unc.lib.dl.fedora.types.GetNextPIDResponse;
import edu.unc.lib.dl.fedora.types.GetObjectXML;
import edu.unc.lib.dl.fedora.types.GetObjectXMLResponse;
import edu.unc.lib.dl.fedora.types.Ingest;
import edu.unc.lib.dl.fedora.types.IngestResponse;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.fedora.types.ModifyDatastreamByReference;
import edu.unc.lib.dl.fedora.types.ModifyDatastreamByReferenceResponse;
import edu.unc.lib.dl.fedora.types.ModifyDatastreamByValue;
import edu.unc.lib.dl.fedora.types.ModifyDatastreamByValueResponse;
import edu.unc.lib.dl.fedora.types.ModifyObject;
import edu.unc.lib.dl.fedora.types.ModifyObjectResponse;
import edu.unc.lib.dl.fedora.types.ObjectProfile;
import edu.unc.lib.dl.fedora.types.PurgeDatastream;
import edu.unc.lib.dl.fedora.types.PurgeDatastreamResponse;
import edu.unc.lib.dl.fedora.types.PurgeObject;
import edu.unc.lib.dl.fedora.types.PurgeObjectResponse;
import edu.unc.lib.dl.fedora.types.PurgeRelationship;
import edu.unc.lib.dl.fedora.types.PurgeRelationshipResponse;
import edu.unc.lib.dl.fedora.types.SetDatastreamVersionable;
import edu.unc.lib.dl.fedora.types.SetDatastreamVersionableResponse;
import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.IllegalRepositoryStateException;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class ManagementClient extends WebServiceTemplate {
	private AccessClient accessClient = null;
	private TripleStoreQueryService tripleStoreQueryService;

	// ENUMS
	private enum Action {
		addDatastream("addDatastream"), getDatastream("getDatastream"), addRelationship("addRelationship"), export(
				"export"), getNextPID("getNextPID"), ingest("ingest"), modifyDatastreamByValue("modifyDatastreamByValue"), modifyDatastreamByReference(
				"modifyDatastreamByReference"), modifyObject("modifyObject"), purgeDatastream("purgeDatastream"), purgeObject(
				"purgeObject"), purgeRelationship("purgeRelationship"), getObjectXML("getObjectXML"), setDatastreamVersionable(
				"setDatastreamVersionable");
		String uri = null;

		Action(String action) {
			uri = "http://www.fedora.info/definitions/1/0/api/#" + action;
		}

		WebServiceMessageCallback callback() {
			return new WebServiceMessageCallback() {
				public void doWithMessage(WebServiceMessage message) {
					((SoapMessage) message).setSoapAction(uri);
				}
			};
		}
	}

	public enum ChecksumType {
		DEFAULT("DEFAULT"), DISABLED("DISABLED"), HAVAL("HAVAL"), MD5("MD5"), SHA_1("SHA-1"), SHA_256("SHA-256"), SHA_385(
				"SHA-385"), SHA_512("SHA-512"), TIGER("TIGER"), WHIRLPOOL("WHIRLPOOL");
		private final String id;

		ChecksumType(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return this.id;
		}
	}

	public enum Context {
		ARCHIVE("archive"), MIGRATE("migrate"), PUBLIC("public");
		private final String id;

		Context(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return this.id;
		}
	}

	public enum Format {
		ATOM_1_0("ATOM-1.0"), FOXML_1_0("FOXML-1.0"), FOXML_1_1("FOXML-1.1"), METS_FED_EXT_1_1("METSFedoraExt-1.1");
		private final String id;

		Format(String id) {
			this.id = "info:fedora/fedora-system:" + id;
		}

		@Override
		public String toString() {
			return this.id;
		}
	}

	public enum State {
		ACTIVE("A"), INACTIVE("I"), DELETED("D");
		private final String id;

		State(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return this.id;
		}
	}

	private static final Log log = LogFactory.getLog(ManagementClient.class);

	private String fedoraContextUrl;

	private String password;

	private String username;

	public String addManagedDatastream(PID pid, String dsid, boolean force, String message, List<String> altids,
			String label, boolean versionable, String mimetype, String locationURI) throws FedoraException {
		AddDatastream req = new AddDatastream();
		req.setPid(pid.getPid());
		req.setDsID(dsid);
		req.setLogMessage(message);
		req.setDsState(State.ACTIVE.id);
		req.setControlGroup("M");
		req.setDsLocation(locationURI);
		// req.setChecksum("none");
		req.setChecksumType(ChecksumType.MD5.id);
		ArrayOfString alts = new ArrayOfString();
		alts.getItem().addAll(altids);
		req.setAltIDs(alts);
		req.setDsLabel(label);
		req.setFormatURI("");
		req.setMIMEType(mimetype);
		req.setVersionable(versionable);
		AddDatastreamResponse resp = (AddDatastreamResponse) this.callService(req, Action.addDatastream);
		String id = resp.getDatastreamID();
		// String timestamp = this.modifyInlineXMLDatastream(pid, dsid,
		// force, message, altids, label,
		// xml);
		return id;
	}

	public String addInlineXMLDatastream(PID pid, String dsid, boolean force, String message, List<String> altids,
			String label, boolean versionable, Document xml) throws FedoraException {
		File file = ClientUtils.writeXMLToTempFile(xml);
		return addInlineXMLDatastream(pid, dsid, force, message, altids, label, versionable, file);
	}
	
	public String addInlineXMLDatastream(PID pid, String dsid, boolean force, String message, List<String> altids,
			String label, boolean versionable, File contentFile) throws FedoraException {
		String location = null;
		location = this.upload(contentFile);
		contentFile.delete();
		AddDatastream req = new AddDatastream();
		req.setPid(pid.getPid());
		req.setDsID(dsid);
		req.setLogMessage(message);
		req.setDsState(State.ACTIVE.id);
		req.setControlGroup(ContentModelHelper.ControlGroup.INTERNAL.getAttributeValue());
		req.setDsLocation(location);
		req.setChecksumType(ChecksumType.MD5.id);
		ArrayOfString alts = new ArrayOfString();
		alts.getItem().addAll(altids);
		req.setAltIDs(alts);
		req.setDsLabel(label);
		req.setFormatURI("");
		req.setMIMEType("text/xml");
		req.setVersionable(versionable);
		AddDatastreamResponse resp = (AddDatastreamResponse) this.callService(req, Action.addDatastream);
		String id = resp.getDatastreamID();
		return id;
	}

	public boolean addLiteralStatement(PID pid, String relationship, String literal, String datatype)
			throws FedoraException {
		AddRelationship req = new AddRelationship();
		req.setPid(pid.getPid());
		req.setDatatype(datatype);
		req.setIsLiteral(true);
		req.setRelationship(relationship);
		req.setObject(literal);
		AddRelationshipResponse resp = (AddRelationshipResponse) this.callService(req, Action.addRelationship);
		return resp.isAdded();
	}

	/**
	 * Selectively turn versioning on or off for selected datastream. When versioning is disabled, subsequent
	 * modifications to the datastream replace the current datastream contents and no versioning history is preserved. To
	 * put it another way: No new datastream versions will be made, but all the existing versions will be retained. All
	 * changes to the datastream will be to the current version.
	 * 
	 * @param pid
	 *           The PID of the object.
	 * @param dsid
	 *           The datastream ID.
	 * @param versionable
	 *           Enable versioning of the datastream.
	 * @param message
	 *           A log message.
	 * @return timestamp of the current version
	 * @throws FedoraException
	 */
	public String setDatastreamVersionable(PID pid, String dsid, boolean versionable, String message)
			throws FedoraException {
		SetDatastreamVersionable req = new SetDatastreamVersionable();
		req.setPid(pid.getPid());
		req.setDsID(dsid);
		req.setVersionable(versionable);
		req.setLogMessage(message);
		SetDatastreamVersionableResponse resp = (SetDatastreamVersionableResponse) this.callService(req,
				Action.setDatastreamVersionable);
		return resp.getModifiedDate();
	}

	public boolean addObjectRelationship(PID pid, String relationship, PID pid2) throws FedoraException {
		AddRelationship req = new AddRelationship();
		req.setPid(pid.getURI());
		req.setIsLiteral(false);
		req.setRelationship(relationship);
		req.setObject(pid2.getURI());
		AddRelationshipResponse resp = (AddRelationshipResponse) this.callService(req, Action.addRelationship);
		return resp.isAdded();
	}

	public Datastream getDatastream(PID pid, String dsID) throws FedoraException {
		GetDatastream req = new GetDatastream();
		req.setPid(pid.getPid());
		req.setDsID(dsID);
		req.setAsOfDateTime("");
		GetDatastreamResponse resp = (GetDatastreamResponse) this.callService(req, Action.getDatastream);
		return resp.getDatastream();
	}

	public Datastream getDatastream(PID pid, String dsID, String asOfDateTime) throws FedoraException {
		GetDatastream req = new GetDatastream();
		req.setPid(pid.getPid());
		req.setDsID(dsID);
		req.setAsOfDateTime(asOfDateTime);
		GetDatastreamResponse resp = (GetDatastreamResponse) this.callService(req, Action.getDatastream);
		return resp.getDatastream();
	}

	public boolean addResourceStatement(PID pid, String relationship, String uri) throws FedoraException {
		try {
			AddRelationship req = new AddRelationship();
			req.setPid(pid.getURI());
			// req.setDatatype();
			req.setIsLiteral(false);
			req.setRelationship(relationship);
			req.setObject(uri);
			AddRelationshipResponse resp = (AddRelationshipResponse) this.callService(req, Action.addRelationship);
			return resp.isAdded();
		} catch (SoapFaultClientException e) {
			log.debug(e);
			throw new NotFoundException(e);
		}
	}

	private Object callService(Object request, Action action) throws FedoraException {
		Object response = null;
		try {
			response = this.marshalSendAndReceive(request, action.callback());
		} catch (WebServiceIOException e) {
			// Connection reset: Apache restarted during a call to ingest (at midnight)
			// 503: an error indicating that Apache is already restarting
			if (e.getMessage() != null && (e.getMessage().contains("503") || e.getMessage().contains("Connection reset"))) {
				throw new FedoraTimeoutException(e);
			} else if (java.net.SocketTimeoutException.class.isInstance(e.getCause())) {
				throw new FedoraTimeoutException(e);
			} else {
				throw new ServiceException(e);
			}
		} catch (SoapFaultClientException e) {
			log.debug("GOT SoapFaultClientException", e);
			FedoraFaultMessageResolver.resolveFault(e);
		} catch (WebServiceFaultException e) {
			throw new ServiceException("Failed to call service", e);
		}
		return response;
	}

	public Document export(Context context, Format format, String pid) throws FedoraException {
		byte[] data = this.exportRaw(context, format, pid);
		Document result = null;
		try {
			result = ClientUtils.parseXML(data);
		} catch (SAXException e) {
			throw new ServiceException("Could not parse reply.", e);
		}
		return result;
	}

	public byte[] exportRaw(Context context, Format format, String pid) throws FedoraException {
		Export o = new Export();
		o.setContext(context.id);
		o.setFormat(format.id);
		o.setPid(pid);
		ExportResponse response = (ExportResponse) this.callService(o, Action.export);
		return response.getObjectXML();
	}

	public String getFedoraContextUrl() {
		return fedoraContextUrl;
	}

	public List<PID> getNextPID(int number, String namespace) throws FedoraException {
		List<PID> result = new ArrayList<PID>();
		GetNextPID req = new GetNextPID();
		req.setNumPIDs(BigInteger.valueOf(number));
		if (namespace != null) {
			req.setPidNamespace(namespace);
		}
		GetNextPIDResponse resp = (GetNextPIDResponse) this.callService(req, Action.getNextPID);
		List<String> pids = resp.getPid();
		for (String pid : pids) {
			result.add(new PID(pid));
		}
		return result;
	}

	public Document getObjectXML(PID pid) throws FedoraException {
		GetObjectXML req = new GetObjectXML();
		req.setPid(pid.getPid());
		GetObjectXMLResponse resp = (GetObjectXMLResponse) this.callService(req, Action.getObjectXML);
		try {
			return ClientUtils.parseXML(resp.getObjectXML());
		} catch (SAXException e) {
			throw new FedoraException("Fedora Object XML could not be parsed");
		}
	}

	public String getPassword() {
		return password;
	}

	public String getUsername() {
		return username;
	}

	public PID ingest(Document xml, Format format, String message) throws FedoraException {
		return ingestRaw(ClientUtils.serializeXML(xml), format, message);
	}

	public PID ingestRaw(byte[] xmlData, Format format, String message) throws FedoraException {
		Ingest ingest = new Ingest();
		ingest.setFormat(format.id);
		ingest.setLogMessage(message);
		ingest.setObjectXML(xmlData);
		IngestResponse response = (IngestResponse) this.callService(ingest, Action.ingest);
		PID result = new PID(response.getObjectPID());
		return result;
	}

	public void init() throws Exception {
		SaajSoapMessageFactory msgFactory = new SaajSoapMessageFactory();
		msgFactory.afterPropertiesSet();
		this.setMessageFactory(msgFactory);

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("edu.unc.lib.dl.fedora.types");
		marshaller.afterPropertiesSet();
		this.setMarshaller(marshaller);
		this.setUnmarshaller(marshaller);

		CommonsHttpMessageSender messageSender = new CommonsHttpMessageSender();
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(this.getUsername(), this.getPassword());
		messageSender.setCredentials(creds);
		messageSender.setReadTimeout(300 * 1000);
		messageSender.afterPropertiesSet();
		this.setMessageSender(messageSender);

		// this.setFaultMessageResolver(new FedoraFaultMessageResolver());
		this.setDefaultUri(this.getFedoraContextUrl() + "/services/management");
		this.afterPropertiesSet();
	}

	// DEPENDENCY SETTERS AND GETTERS
	public String modifyDatastreamByValue(PID pid, String dsid, boolean force, String message, List<String> altids,
			String label, String mimetype, String checksum, ChecksumType checksumType, File contentFile) throws FedoraException {
		byte[] contentBytes;
		try {
			contentBytes = FileUtils.readFileToByteArray(contentFile);
			return modifyDatastreamByValue(pid, dsid, force, message, altids, label, mimetype, checksum, checksumType, contentBytes);
		} catch (IOException e) {
			log.error("Could not read the new content file", e);
		}
		return null;
	}
	
	public String modifyDatastreamByValue(PID pid, String dsid, boolean force, String message, List<String> altids,
			String label, String mimetype, String checksum, ChecksumType checksumType, byte[] content)
			throws FedoraException {
		// TODO: add checksum calculation
		ModifyDatastreamByValue req = new ModifyDatastreamByValue();
		req.setPid(pid.getPid());
		req.setDsID(dsid);
		req.setForce(force);
		req.setLogMessage(message);

		ArrayOfString alts = new ArrayOfString();
		if (altids != null) {
			alts.getItem().addAll(altids);
		}
		req.setAltIDs(alts);
		if (label != null) {
			req.setDsLabel(label);
		}
		req.setFormatURI("");
		if (mimetype != null) {
			req.setMIMEType(mimetype);
		}
		if (checksum != null) {
			req.setChecksum(checksum);
		}
		// else {
		// req.setChecksum("none");
		// }
		if (checksumType != null) {
			req.setChecksumType(checksumType.id);
		} else {
			req.setChecksumType(ChecksumType.MD5.id);
		}
		req.setDsContent(content);
		ModifyDatastreamByValueResponse resp = (ModifyDatastreamByValueResponse) this.callService(req,
				Action.modifyDatastreamByValue);
		return resp.getModifiedDate();
	}

	public String modifyDatastreamByReference(PID pid, String dsid, boolean force, String message, List<String> altids,
			String label, String mimetype, String checksum, ChecksumType checksumType, String dsLocation)
			throws FedoraException {
		// TODO: add checksum calculation
		ModifyDatastreamByReference req = new ModifyDatastreamByReference();
		req.setPid(pid.getPid());
		req.setDsID(dsid);
		req.setForce(force);
		req.setLogMessage(message);

		ArrayOfString alts = new ArrayOfString();
		if (altids != null) {
			alts.getItem().addAll(altids);
		}
		req.setAltIDs(alts);
		if (label != null) {
			req.setDsLabel(label);
		}
		req.setFormatURI("");
		if (mimetype != null) {
			req.setMIMEType(mimetype);
		}
		if (checksum != null) {
			req.setChecksum(checksum);
		}
		// } else {
		// req.setChecksum("none");
		// }
		if (checksumType != null) {
			req.setChecksumType(checksumType.id);
		} else {
			req.setChecksumType(ChecksumType.MD5.id);
		}
		req.setDsLocation(dsLocation);
		ModifyDatastreamByReferenceResponse resp = (ModifyDatastreamByReferenceResponse) this.callService(req,
				Action.modifyDatastreamByReference);
		return resp.getModifiedDate();
	}

	public String modifyInlineXMLDatastream(PID pid, String dsid, boolean force, String message, List<String> altids,
			String label, Document content) throws FedoraException {
		byte[] data = ClientUtils.serializeXML(content);
		// String checksum = new Checksum().getChecksum(data);
		String timestamp = this.modifyDatastreamByValue(pid, dsid, force, message, altids, label, "text/xml", null,
				ChecksumType.MD5, data);
		return timestamp;
	}

	public String modifyObject(PID pid, String label, String ownerid, State state, String message)
			throws FedoraException {
		ModifyObject req = new ModifyObject();
		req.setLabel(label);
		req.setLogMessage(message);
		req.setOwnerId(ownerid);
		req.setPid(pid.getPid());
		req.setState(state.id);
		ModifyObjectResponse resp = (ModifyObjectResponse) this.callService(req, Action.modifyObject);
		return resp.getModifiedDate();
	}

	public List<String> purgeDatastream(PID pid, String dsid, String message, boolean force, String start, String end)
			throws FedoraException {
		PurgeDatastream req = new PurgeDatastream();
		req.setPid(pid.getPid());
		req.setDsID(dsid);
		req.setForce(force);
		req.setLogMessage(message);

		req.setStartDT(start);
		req.setEndDT(end);

		PurgeDatastreamResponse resp = (PurgeDatastreamResponse) this.callService(req, Action.purgeDatastream);
		return resp.getPurgedVersionDate();
	}
	
	public boolean setExclusiveLiteral(PID pid, String relationship, String literal, String datatype) throws FedoraException {
		List<String> rel = tripleStoreQueryService.fetchAllTriples(pid).get(relationship);
		
		if (rel != null) {
			if (rel.contains(literal)) {
				rel.remove(literal);
			} else {
				// add missing rel
				this.addLiteralStatement(pid, relationship, literal, datatype);
			}
			// remove any other same predicate triples
			for (String oldValue : rel) {
				this.purgeLiteralStatement(pid, relationship, oldValue, datatype);
			}
			return true; 
		} else {
			// add missing rel
			return this.addLiteralStatement(pid, relationship, literal, datatype);
		}
	}

	public boolean purgeLiteralStatement(PID pid, String relationship, String literal, String datatype)
			throws FedoraException {
		PurgeRelationship req = new PurgeRelationship();
		req.setPid(pid.getURI());
		req.setDatatype(datatype);
		req.setIsLiteral(true);
		req.setRelationship(relationship);
		req.setObject(literal);
		PurgeRelationshipResponse resp = (PurgeRelationshipResponse) this.callService(req, Action.purgeRelationship);
		return resp.isPurged();
	}

	public String purgeObject(PID pid, String message, boolean force) throws FedoraException {
		PurgeObject req = new PurgeObject();
		req.setPid(pid.getPid());
		req.setLogMessage(message);
		req.setForce(force);
		PurgeObjectResponse resp = (PurgeObjectResponse) this.callService(req, Action.purgeObject);
		return resp.getPurgedDate();
	}

	public boolean purgeObjectRelationship(PID pid, String relationship, PID pid2) throws FedoraException {
		PurgeRelationship req = new PurgeRelationship();
		req.setPid(pid.getURI());
		req.setIsLiteral(false);
		req.setRelationship(relationship);
		req.setObject(pid2.getURI());
		PurgeRelationshipResponse resp = (PurgeRelationshipResponse) this.callService(req, Action.purgeRelationship);
		return resp.isPurged();
	}

	public void setFedoraContextUrl(String fedoraContextUrl) {
		this.fedoraContextUrl = fedoraContextUrl;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String upload(File file) {
		String result = null;
		String uploadURL = this.getFedoraContextUrl() + "/upload";
		HttpClient http = HttpClientUtil.getAuthenticatedClient(uploadURL, this.getUsername(), this.getPassword());
		PostMethod post = new PostMethod(uploadURL);
		post.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, false);
		log.debug("Uploading file with forwarded groups: " + GroupsThreadStore.getGroupString());
		post.addRequestHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, GroupsThreadStore.getGroupString());
		try {
			log.debug("Uploading to " + uploadURL);
			Part[] parts = { new FilePart("file", file) };
			post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
			http.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
			int status = http.executeMethod(post);

			InputStream in = post.getResponseBodyAsStream();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			int b;
			try {
				while ((b = in.read()) != -1) {
					pw.write(b);
				}
			} finally {
				if (pw != null) {
					pw.flush();
					pw.close();
				}
				if (in != null) {
					try {
						in.close();
					} catch (IOException ignored) {
					}
				}
			}

			if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_ACCEPTED) {
				result = sw.toString().trim();
				log.info("Upload complete, response=" + result);
			} else {
				log.warn("Upload failed, response=" + HttpStatus.getStatusText(status));
				log.debug(sw.toString().trim());
			}
		} catch (Exception ex) {
			throw new ServiceException(ex);
		} finally {
			post.releaseConnection();
		}
		return result;
	}

	public String upload(Document xml) {
		String result = null;

		// write the document to a byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XMLOutputter out = new XMLOutputter();
		try {
			out.output(xml, baos);
		} catch (IOException e) {
			throw new ServiceException("Unexpected error writing to byte array output stream", e);
		} finally {
			try {
				baos.close();
			} catch (IOException ignored) {
			}
		}

		// construct a post request to Fedora upload service
		String uploadURL = this.getFedoraContextUrl() + "/upload";
		HttpClient http = HttpClientUtil.getAuthenticatedClient(uploadURL, this.getUsername(), this.getPassword());
		PostMethod post = new PostMethod(uploadURL);
		post.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, false);
		log.debug("Uploading XML with forwarded groups: " + GroupsThreadStore.getGroupString());
		post.addRequestHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, GroupsThreadStore.getGroupString());
		try {
			log.debug("Uploading to " + uploadURL);
			Part[] parts = { new FilePart("file", new ByteArrayPartSource("md_events.xml", baos.toByteArray())) };
			post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
			http.getHttpConnectionManager().getParams().setConnectionTimeout(5000);

			int status = http.executeMethod(post);

			InputStream in = post.getResponseBodyAsStream();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			int b;
			try {
				while ((b = in.read()) != -1) {
					pw.write(b);
				}
			} finally {
				if (pw != null) {
					pw.flush();
					pw.close();
				}
				if (in != null) {
					try {
						in.close();
					} catch (IOException ignored) {
					}
				}
			}
			if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_ACCEPTED) {
				result = sw.toString().trim();
				log.debug("Upload complete, response=" + result);
			} else {
				log.warn("Upload failed, response=" + HttpStatus.getStatusText(status));
				log.debug(sw.toString().trim());
			}
		} catch (Exception ex) {
			log.error("Upload failed due to error", ex);
			throw new ServiceException(ex);
		} finally {
			post.releaseConnection();
		}
		return result;
	}

	public String getIrodsPath(String dsFedoraLocationToken) {
		log.debug("getting iRODS path for " + dsFedoraLocationToken);
		String result = null;
		try {
			String url = getFedoraContextUrl() + "/storagelocation";
			HttpClient httpClient = HttpClientUtil.getAuthenticatedClient(url, this.getUsername(), this.getPassword());
			GetMethod get = new GetMethod(url);
			get.setQueryString("pid=" + URLEncoder.encode(dsFedoraLocationToken, "utf-8"));
			int statusCode = httpClient.executeMethod(get);
			if (statusCode != HttpStatus.SC_OK) {
				throw new RuntimeException("CDR storage location GET method failed: " + get.getStatusLine());
			} else {
				log.debug("CDR storage location GET method: " + get.getStatusLine());
				byte[] resultBytes = get.getResponseBody();
				result = new String(resultBytes, "utf-8");
				result = result.trim();
			}
		} catch (Exception e) {
			throw new ServiceException("Cannot contact iRODS location service.", e);
		}
		return result;
	}

	/**
	 * Poll Fedora until the PID is found or timeout. This method will blocking until at most the specified timeout plus
	 * the timeout of the underlying HTTP connection.
	 * 
	 * @param pid
	 *           the PID to look for
	 * @param delay
	 *           the delay between Fedora requests in seconds
	 * @param timeout
	 *           the total polling time in seconds
	 * @return true when the object is found within timeout, false on timeout
	 */
	public boolean pollForObject(PID pid, int delay, int timeout) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < timeout * 1000) {
			if (Thread.interrupted())
				throw new InterruptedException();
			try {
				ObjectProfile doc = this.getAccessClient().getObjectProfile(pid, null);
				if (doc != null)
					return true;
			} catch (ServiceException e) {
				if (log.isDebugEnabled())
					log.debug("Expected service exception while polling fedora", e);
			} catch (FedoraException e) {
				// fedora responded, but object not found
				log.debug("got exception from fedora", e);
			}
			if (Thread.interrupted())
				throw new InterruptedException();
			log.info(pid + " not found, waiting " + delay + " seconds..");
			Thread.sleep(delay * 1000);
		}
		return false;
	}

	public String writePremisEventsToFedoraObject(PremisEventLogger eventLogger, PID pid) throws FedoraException {
		Document dom = null;
		MIMETypedStream mts = this.getAccessClient().getDatastreamDissemination(pid, "MD_EVENTS", null);
		ByteArrayInputStream bais = new ByteArrayInputStream(mts.getStream());
		try {
			dom = new SAXBuilder().build(bais);
			bais.close();
		} catch (JDOMException e) {
			throw new IllegalRepositoryStateException("Cannot parse MD_EVENTS: " + pid, e);
		} catch (IOException e) {
			throw new Error(e);
		}
		eventLogger.appendLogEvents(pid, dom.getRootElement());
		String eventsLoc = this.upload(dom);
		String logTimestamp = this.modifyDatastreamByReference(pid, "MD_EVENTS", false, "adding PREMIS events",
				new ArrayList<String>(), "PREMIS Events", "text/xml", null, null, eventsLoc);
		return logTimestamp;
	}

	// @Override
	// public Object sendAndReceive(String uriString, WebServiceMessageCallback requestCallback,
	// WebServiceMessageExtractor responseExtractor) {
	// Assert.notNull(responseExtractor, "'responseExtractor' must not be null");
	// Assert.hasLength(uriString, "'uri' must not be empty");
	// TransportContext previousTransportContext = TransportContextHolder.getTransportContext();
	// WebServiceConnection connection = null;
	// try {
	// connection = createConnection(URI.create(uriString));
	// if(connection instanceof CommonsHttpConnection) {
	// CommonsHttpConnection commonsConn = (CommonsHttpConnection)connection;
	// commonsConn.getPostMethod().addRequestHeader("myGroupsHeader", "someshibbolethgroups");
	// }
	// TransportContextHolder.setTransportContext(new DefaultTransportContext(connection));
	// MessageContext messageContext = new DefaultMessageContext(getMessageFactory());
	//
	// return doSendAndReceive(messageContext, connection, requestCallback, responseExtractor);
	// } catch (TransportException ex) {
	// throw new WebServiceTransportException("Could not use transport: " + ex.getMessage(), ex);
	// } catch (IOException ex) {
	// throw new WebServiceIOException("I/O error: " + ex.getMessage(), ex);
	// } finally {
	// TransportUtils.closeConnection(connection);
	// TransportContextHolder.setTransportContext(previousTransportContext);
	// }
	// }

	/**
	 * @param accessClient
	 *           the accessClient to set
	 */
	public void setAccessClient(AccessClient accessClient) {
		this.accessClient = accessClient;
	}

	/**
	 * @return the accessClient
	 */
	public AccessClient getAccessClient() {
		return accessClient;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
