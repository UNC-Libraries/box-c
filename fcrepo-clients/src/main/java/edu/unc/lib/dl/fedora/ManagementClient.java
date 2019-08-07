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

import static edu.unc.lib.dl.util.ContentModelHelper.Administrative_PID.REPOSITORY;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.MD_EVENTS;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.RELS_EXT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceFaultException;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.AuthorizationException.AuthorizationErrorType;
import edu.unc.lib.dl.fedora.types.AddDatastream;
import edu.unc.lib.dl.fedora.types.AddDatastreamResponse;
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
import edu.unc.lib.dl.fedora.types.SetDatastreamVersionable;
import edu.unc.lib.dl.fedora.types.SetDatastreamVersionableResponse;
import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.IllegalRepositoryStateException;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.RDFXMLUtil;

/**
 * 
 * @author count0
 *
 */
public class ManagementClient extends WebServiceTemplate {
    private AccessClient accessClient = null;
    private HttpClientConnectionManager httpManager;
    private CloseableHttpClient httpClient;

    private static int RELS_EXT_RETRIES = 10;
    private static long RELS_EXT_RETRY_DELAY = 250L;

    // ENUMS
    private enum Action {
        addDatastream("addDatastream"), getDatastream("getDatastream"), addRelationship("addRelationship"),
                export("export"), getNextPID("getNextPID"), ingest("ingest"),
                modifyDatastreamByValue("modifyDatastreamByValue"),
                modifyDatastreamByReference("modifyDatastreamByReference"),
                modifyObject("modifyObject"), purgeDatastream("purgeDatastream"),
                purgeObject("purgeObject"), purgeRelationship("purgeRelationship"),
                getObjectXML("getObjectXML"), setDatastreamVersionable(
                "setDatastreamVersionable");
        String uri = null;

        Action(String action) {
            uri = "http://www.fedora.info/definitions/1/0/api/#" + action;
        }

        WebServiceMessageCallback callback() {
            return new WebServiceMessageCallback() {
                @Override
                public void doWithMessage(WebServiceMessage message) {
                    ((SoapMessage) message).setSoapAction(uri);
                }
            };
        }
    }

    public enum ChecksumType {
        DEFAULT("DEFAULT"), DISABLED("DISABLED"), HAVAL("HAVAL"), MD5("MD5"), SHA_1("SHA-1"),
                SHA_256("SHA-256"), SHA_385("SHA-385"), SHA_512("SHA-512"), TIGER("TIGER"),
                WHIRLPOOL("WHIRLPOOL");
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

    private static final Logger log = LoggerFactory.getLogger(ManagementClient.class);

    private String fedoraContextUrl;

    private String password;

    private String username;

    private String fedoraHost;

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
            String label, boolean versionable, Document xml) throws FedoraException, FileNotFoundException {
        File file = ClientUtils.writeXMLToTempFile(xml);
        return addInlineXMLDatastream(pid, dsid, force, message, altids, label, versionable, file);
    }

    public String addInlineXMLDatastream(PID pid, String dsid, boolean force, String message, List<String> altids,
            String label, boolean versionable, File contentFile) throws FedoraException, FileNotFoundException {
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

    public void addLiteralStatement(PID pid, String pred, Namespace ns, String literal, String datatype)
            throws FedoraException {
        addTriple(pid, pred, ns, true, literal, datatype);
    }

    public void addTriple(PID pid, String pred, Namespace ns, boolean isLiteral, String value, String datatype)
            throws FedoraException {

        do {
            DatastreamDocument dsDoc = getRELSEXTWithRetries(pid);

            try {
                Document doc = dsDoc.getDocument();
                RDFXMLUtil.addTriple(doc.getRootElement(), pred, ns, isLiteral, value, datatype);

                modifyDatastream(pid, RELS_EXT.getName(), "Setting exclusive relation", dsDoc.getLastModified(),
                        dsDoc.getDocument());
                return;
            } catch (OptimisticLockException e) {
                log.debug("Unable to update RELS-EXT for {}, retrying", pid, e);
            }
        } while (true);
    }

    /**
     * Selectively turn versioning on or off for selected datastream. When
     * versioning is disabled, subsequent modifications to the datastream
     * replace the current datastream contents and no versioning history is
     * preserved. To put it another way: No new datastream versions will be
     * made, but all the existing versions will be retained. All changes to the
     * datastream will be to the current version.
     *
     * @param pid
     *            The PID of the object.
     * @param dsid
     *            The datastream ID.
     * @param versionable
     *            Enable versioning of the datastream.
     * @param message
     *            A log message.
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

    public boolean addObjectRelationship(PID pid, String predicate, Namespace ns, PID pid2) throws FedoraException {
        addTriple(pid, predicate, ns, false, pid2.getURI(), null);
        return true;
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

    private Object callService(Object request, Action action) throws FedoraException {
        return callService(request, action, true);
    }

    private Object callService(Object request, Action action, boolean retry) throws FedoraException {
        Object response = null;
        try {
            response = this.marshalSendAndReceive(request, action.callback());
        } catch (WebServiceIOException e) {
            // Connection reset: Apache restarted during a call to ingest (at
            // midnight)
            // 503: an error indicating that Apache is already restarting
            if (e.getMessage() != null
                    && (e.getMessage().contains("503") || e.getMessage().contains("Connection reset"))) {
                throw new FedoraTimeoutException(e);
            } else if (java.net.SocketTimeoutException.class.isInstance(e.getCause())) {
                throw new FedoraTimeoutException(e);
            } else {
                throw new ServiceException(e);
            }
        } catch (SoapFaultClientException e) {
            log.debug("GOT SoapFaultClientException", e);
            try {
                FedoraFaultMessageResolver.resolveFault(e);
            } catch (AuthorizationException ae) {
                if (retry && AuthorizationErrorType.NOT_APPLICABLE.equals(ae.getType())) {
                    log.warn("Authorization was not applicable, attempting to reestablish connection to Fedora.");
                    try {
                        this.initializeConnections();
                    } catch (Exception e1) {
                        log.error("Failed to reestablish connection to Fedora", e);
                        throw ae;
                    }
                    return callService(request, action, false);
                }

                throw ae;
            }
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
        httpManager = new PoolingHttpClientConnectionManager();

        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000).build();

        HttpClientBuilder builder = HttpClientUtil.getAuthenticatedClientBuilder(fedoraHost, getUsername(),
                getPassword());
        builder.setDefaultRequestConfig(requestConfig);

        httpClient = builder.build();

        initializeConnections();
    }

    private void initializeConnections() throws Exception {
        SaajSoapMessageFactory msgFactory = new SaajSoapMessageFactory();
        msgFactory.afterPropertiesSet();
        this.setMessageFactory(msgFactory);

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("edu.unc.lib.dl.fedora.types");
        marshaller.afterPropertiesSet();
        this.setMarshaller(marshaller);
        this.setUnmarshaller(marshaller);

        HttpComponentsMessageSender sender = new HttpComponentsMessageSender();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(this.username, this.password);
        sender.setCredentials(credentials);
        sender.setReadTimeout(300 * 1000);
        sender.afterPropertiesSet();
        this.setMessageSender(sender);

        // this.setFaultMessageResolver(new FedoraFaultMessageResolver());
        this.setDefaultUri(this.getFedoraContextUrl() + "/services/management");
        this.afterPropertiesSet();
    }

    public void destroy() {
        if (this.httpManager != null) {
            this.httpManager.shutdown();
        }
    }

    // DEPENDENCY SETTERS AND GETTERS
    public String modifyDatastreamByValue(PID pid, String dsid, boolean force, String message, List<String> altids,
            String label, String mimetype, String checksum, ChecksumType checksumType, File contentFile)
            throws FedoraException {
        byte[] contentBytes;
        try {
            contentBytes = FileUtils.readFileToByteArray(contentFile);
            return modifyDatastreamByValue(pid, dsid, force, message, altids, label, mimetype, checksum, checksumType,
                    contentBytes);
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

    public void modifyDatastream(PID pid, String dsid, String message, String lastModifiedDate, Document content)
            throws FedoraException {
        byte[] dsBytes = ClientUtils.serializeXML(content);
        modifyDatastream(pid, dsid, message, lastModifiedDate, dsBytes);
    }

    public void modifyDatastream(PID pid, String dsid, String message, String lastModifiedDate, byte[] content)
            throws FedoraException {

        // PutMethod ignores query parameters, so put them in the URI:

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(this.getFedoraContextUrl());
        builder.pathSegment("objects");
        builder.pathSegment(pid.getPid());
        builder.pathSegment("datastreams");
        builder.pathSegment(dsid);

        if (message != null) {
            builder.queryParam("logMessage", message);
        }

        if (lastModifiedDate != null) {
            builder.queryParam("lastModifiedDate", lastModifiedDate);
        }

        HttpPut method = new HttpPut(builder.build().encode().toUriString());
        method.setEntity(new ByteArrayEntity(content));
        if (GroupsThreadStore.getGroups() != null) {
            method.addHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER,
                    GroupsThreadStore.getGroups().joinAccessGroups(";", null, false));
        }

        try (CloseableHttpResponse httpResp = httpClient.execute(method)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            if (statusCode == 403) {
                throw new AuthorizationException("Failed to update datastream " + dsid + " on object " + pid
                        + " due to insufficient permissions");
            }

            if (statusCode == 409) {
                throw new OptimisticLockException("Datastream " + dsid + " on object " + pid
                        + " has been modified more recently than the specified last modified date");
            }
        } catch (IOException e) {
            throw new ServiceException("Failed to modify datastream " + dsid + " on object " + pid, e);
        }
    }

    public String modifyObject(PID pid, String label, String ownerid, State state, String message)
            throws FedoraException {
        ModifyObject req = new ModifyObject();
        req.setLabel(label);
        req.setLogMessage(message);
        req.setOwnerId(ownerid);
        req.setPid(pid.getPid());
        req.setState(state == null ? null : state.id);
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

    public boolean purgeLiteralStatement(PID pid, String predicate, Namespace ns, String literal, String datatype)
            throws FedoraException {
        return purgeTriple(pid, predicate, ns, true, literal, datatype);
    }

    public String purgeObject(PID pid, String message, boolean force) throws FedoraException {
        PurgeObject req = new PurgeObject();
        req.setPid(pid.getPid());
        req.setLogMessage(message);
        req.setForce(force);
        PurgeObjectResponse resp = (PurgeObjectResponse) this.callService(req, Action.purgeObject);
        return resp.getPurgedDate();
    }

    public boolean purgeObjectRelationship(PID pid, String predicate, Namespace ns, PID pid2) throws FedoraException {
        return purgeTriple(pid, predicate, ns, false, pid2.getURI(), null);
    }

    public boolean purgeTriple(PID pid, String predicate, Namespace ns, boolean isLiteral, String value,
            String datatype) throws FedoraException {

        do {
            DatastreamDocument dsDoc = getRELSEXTWithRetries(pid);

            try {
                Document doc = dsDoc.getDocument();
                boolean removed = RDFXMLUtil.removeTriple(doc.getRootElement(), predicate, ns, isLiteral, value,
                        datatype);

                if (removed) {
                    modifyDatastream(pid, RELS_EXT.getName(), "Setting exclusive relation", dsDoc.getLastModified(),
                            dsDoc.getDocument());
                }
                return removed;
            } catch (OptimisticLockException e) {
                log.debug("Unable to update RELS-EXT for {}, retrying", pid, e);
            }
        } while (true);
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

    public String upload(File file) throws FileNotFoundException {
        return upload(new FileInputStream(file), true);
    }

    public String upload(String content) {
        return upload(new ByteArrayInputStream(content.getBytes()), true);
    }

    public String upload(Document xml) {
        // write the document to a byte array
        XMLOutputter out = new XMLOutputter();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            out.output(xml, baos);
            return upload(new ByteArrayInputStream(baos.toByteArray()), true);
        } catch (IOException e) {
            throw new ServiceException("Unexpected error writing to byte array output stream", e);
        }
    }

    public String upload(byte[] bytes) {
        return upload(new ByteArrayInputStream(bytes), true);
    }

    public String upload(InputStream content, boolean retry) {
        String result = null;
        String uploadURL = this.getFedoraContextUrl() + "/upload";

        RequestConfig conf = RequestConfig.custom().setExpectContinueEnabled(false).build();
        HttpPost post = new HttpPost(uploadURL);
        post.setConfig(conf);
        log.debug("Uploading file with forwarded groups: {}", GroupsThreadStore.getGroupString());
        post.addHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, GroupsThreadStore.getGroupString());

        log.debug("Uploading to {}", uploadURL);

        // Add the file to the request. It must be labeled 'file' for fedora to
        // find it
        HttpEntity fileEntity = MultipartEntityBuilder.create().addBinaryBody("file", content).build();
        post.setEntity(fileEntity);

        try (CloseableHttpResponse httpResp = httpClient.execute(post)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            String responseString = EntityUtils.toString(httpResp.getEntity(), "UTF-8");

            switch (statusCode) {
            case HttpStatus.SC_OK:
            case HttpStatus.SC_CREATED:
            case HttpStatus.SC_ACCEPTED:
                result = responseString.trim();
                log.info("Upload complete, response=" + result);
                break;
            case HttpStatus.SC_FORBIDDEN:
                log.warn("Authorization to Fedora failed, attempting to reestablish connection.");
                try {
                    this.initializeConnections();
                    return upload(content, false);
                } catch (Exception e) {
                    log.error("Failed to reestablish connection to Fedora", e);
                }
                break;
            case HttpStatus.SC_SERVICE_UNAVAILABLE:
                throw new FedoraTimeoutException("Fedora service unavailable, upload failed");
            default:
                log.warn("Upload failed, response=" + statusCode);
                log.debug(responseString.toString().trim());
                break;
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
        return result;

    }

    public String getIrodsPath(String dsFedoraLocationToken) {
        log.debug("getting iRODS path for {}", dsFedoraLocationToken);
        String result = null;

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("pid", dsFedoraLocationToken));

        StringBuilder url = new StringBuilder();
        url.append(getFedoraContextUrl()).append("/storagelocation?").append(URLEncodedUtils.format(params, "UTF-8"));

        HttpGet get = new HttpGet(url.toString());

        try (CloseableHttpResponse httpResp = httpClient.execute(get)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                throw new RuntimeException("CDR storage location GET method failed: " + httpResp.getStatusLine());
            } else {
                log.debug("CDR storage location GET method: " + httpResp.getStatusLine());
                result = EntityUtils.toString(httpResp.getEntity(), "UTF-8").trim();
            }
        } catch (Exception e) {
            throw new ServiceException(
                    "Error while contacting iRODS location service with datastream location " + dsFedoraLocationToken,
                    e);
        }
        return result;
    }

    /**
     * Poll Fedora until the PID is found or timeout. This method will blocking
     * until at most the specified timeout plus the timeout of the underlying
     * HTTP connection.
     *
     * @param pid
     *            the PID to look for
     * @param delay
     *            the delay between Fedora requests in seconds
     * @param timeout
     *            the total polling time in seconds
     * @return true when the object is found within timeout, false on timeout
     */
    public boolean pollForObject(PID pid, int delay, int timeout) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout * 1000) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            try {
                ObjectProfile doc = this.getAccessClient().getObjectProfile(pid, null);
                if (doc != null) {
                    return true;
                }
            } catch (ServiceException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Expected service exception while polling fedora", e);
                }
            } catch (FedoraException e) {
                // fedora responded, but object not found
                log.debug("got exception from fedora", e);
            }
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            log.info(pid + " not found, waiting " + delay + " seconds..");
            Thread.sleep(delay * 1000);
        }
        return false;
    }

    /**
     * Returns true if the repository is available for connections
     *
     * @return
     */
    public boolean isRepositoryAvailable() {

        try {
            ObjectProfile doc = this.getAccessClient().getObjectProfile(REPOSITORY.getPID(), null);
            return doc != null;
        } catch (Exception e) {
            // If an exception occurs, then the repository is not reachable
        }

        return false;
    }

    /**
     * Blocks until the repository is accessible or the process is interrupted
     */
    public void waitForRepositoryAvailable() {
        while (!isRepositoryAvailable()) {
            try {
                log.info("Waiting for the repository to become available");
                Thread.sleep(10000L);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public String writePremisEventsToFedoraObject(PremisEventLogger eventLogger, PID pid) throws FedoraException {
        Document dom = null;
        boolean newDatastream = false;

        try {
            MIMETypedStream mts = this.getAccessClient().getDatastreamDissemination(pid, "MD_EVENTS", null);
            ByteArrayInputStream bais = new ByteArrayInputStream(mts.getStream());
            dom = new SAXBuilder().build(bais);
            bais.close();
        } catch (JDOMException e) {
            throw new IllegalRepositoryStateException("Cannot parse MD_EVENTS: " + pid, e);
        } catch (IOException e) {
            throw new Error(e);
        } catch (NotFoundException e) {
            log.warn("Could not find MD_EVENTS for {}, creating a new document", pid);

            dom = new Document();
            Element premis = new Element("premis", JDOMNamespaceUtil.PREMIS_V2_NS)
                    .addContent(PremisEventLogger.getObjectElement(pid));
            dom.setRootElement(premis);

            newDatastream = true;
        }

        eventLogger.appendLogEvents(pid, dom.getRootElement());
        String eventsLoc = this.upload(dom);
        String logTimestamp;
        if (newDatastream) {
            logTimestamp = this.addManagedDatastream(pid, MD_EVENTS.getName(), false, "adding PREMIS events",
                    new ArrayList<String>(), MD_EVENTS.getLabel(), MD_EVENTS.isVersionable(), "text/xml", eventsLoc);
        } else {
            logTimestamp = this.modifyDatastreamByReference(pid, MD_EVENTS.getName(), false, "adding PREMIS events",
                    new ArrayList<String>(), MD_EVENTS.getLabel(), "text/xml", null, null, eventsLoc);
        }

        return logTimestamp;
    }

    public void writePremisEventsToFedoraObject(final PremisEventLogger eventLogger, final Collection<PID> pids) {
        final AccessGroupSet groups = GroupsThreadStore.getGroups();

        Runnable premisRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    GroupsThreadStore.storeGroups(groups);

                    for (PID pid : pids) {
                        try {
                            writePremisEventsToFedoraObject(eventLogger, pid);
                        } catch (FedoraException e) {
                            log.error("Failed to update premis for {}", pid, e);
                        }
                    }
                } finally {
                    GroupsThreadStore.clearGroups();
                }
            }
        };

        Thread thread = new Thread(premisRunnable);
        thread.start();
    }

    /**
     * Returns response containing the jdom document representing the datastream
     * and the last modified date. If it does not exist, then null is returned.
     * If the document cannot be parsed, a ServiceException is thrown.
     *
     * @param pid
     * @param datastreamName
     * @return
     * @throws FedoraException
     */
    public DatastreamDocument getXMLDatastreamIfExists(PID pid, String datastreamName) throws FedoraException {

        log.debug("Attempting to get datastream " + datastreamName + " for object " + pid);

        try {

            while (true) {

                edu.unc.lib.dl.fedora.types.Datastream datastream = this.getDatastream(pid, datastreamName);

                if (datastream == null) {
                    return null;
                }

                log.debug("Got datastream, attempting to get dissemination version with create date "
                        + datastream.getCreateDate());

                try {

                    MIMETypedStream mts = accessClient.getDatastreamDissemination(pid, datastreamName,
                            datastream.getCreateDate());

                    try (ByteArrayInputStream bais = new ByteArrayInputStream(mts.getStream())) {
                        Document dsDoc = new SAXBuilder().build(bais);
                        return new DatastreamDocument(dsDoc, datastream.getCreateDate());
                    } catch (JDOMException | IOException e) {
                        throw new ServiceException(
                                "Failed to parse datastream " + datastreamName + " for object " + pid, e);
                    }

                } catch (NotFoundException e) {
                    log.debug("No dissemination version for create date " + datastream.getCreateDate()
                            + " found, retrying");
                }

            }

        } catch (NotFoundException e) {
            return null;
        }

    }

    public DatastreamDocument getRELSEXTWithRetries(PID pid) throws FedoraException {
        for (int tries = RELS_EXT_RETRIES; tries > 0; tries--) {
            DatastreamDocument relsExtResp = getXMLDatastreamIfExists(pid, RELS_EXT.getName());
            if (relsExtResp == null) {
                log.debug("Could not find RELS-EXT for {}, retrying", pid);
                try {
                    Thread.sleep(RELS_EXT_RETRY_DELAY);
                } catch (InterruptedException e) {
                    break;
                }
            } else {
                return relsExtResp;
            }
        }
        throw new NotFoundException("Unable to retrieve RELS-EXT for " + pid);
    }

    public void setExclusiveTripleRelation(PID pid, String predicate, Namespace namespace, PID exclusivePID)
            throws FedoraException {
        setExclusiveTriple(pid, predicate, namespace, exclusivePID.toString(), false, null);
    }

    public void setExclusiveLiteral(PID pid, String predicate, Namespace namespace, String newExclusiveValue,
            String datatype) throws FedoraException {
        setExclusiveTriple(pid, predicate, namespace, newExclusiveValue, true, datatype);
    }

    private void setExclusiveTriple(PID pid, String predicate, Namespace namespace, String value, boolean isLiteral,
            String datatype) throws FedoraException {

        do {
            DatastreamDocument dsDoc = getRELSEXTWithRetries(pid);

            try {
                Document doc = dsDoc.getDocument();
                RDFXMLUtil.setExclusiveTriple(doc.getRootElement(), predicate, namespace, isLiteral, value, datatype);

                modifyDatastream(pid, RELS_EXT.getName(), "Setting exclusive relation", dsDoc.getLastModified(),
                        dsDoc.getDocument());
                return;
            } catch (OptimisticLockException e) {
                log.debug("Unable to update RELS-EXT for {}, retrying", pid, e);
            }
        } while (true);
    }

    // @Override
    // public Object sendAndReceive(String uriString, WebServiceMessageCallback
    // requestCallback,
    // WebServiceMessageExtractor responseExtractor) {
    // Assert.notNull(responseExtractor, "'responseExtractor' must not be
    // null");
    // Assert.hasLength(uriString, "'uri' must not be empty");
    // TransportContext previousTransportContext =
    // TransportContextHolder.getTransportContext();
    // WebServiceConnection connection = null;
    // try {
    // connection = createConnection(URI.create(uriString));
    // if (connection instanceof CommonsHttpConnection) {
    // CommonsHttpConnection commonsConn = (CommonsHttpConnection)connection;
    // commonsConn.getPostMethod().addRequestHeader("myGroupsHeader",
    // "someshibbolethgroups");
    // }
    // TransportContextHolder.setTransportContext(new
    // DefaultTransportContext(connection));
    // MessageContext messageContext = new
    // DefaultMessageContext(getMessageFactory());
    //
    // return doSendAndReceive(messageContext, connection, requestCallback,
    // responseExtractor);
    // } catch (TransportException ex) {
    // throw new WebServiceTransportException("Could not use transport: " +
    // ex.getMessage(), ex);
    // } catch (IOException ex) {
    // throw new WebServiceIOException("I/O error: " + ex.getMessage(), ex);
    // } finally {
    // TransportUtils.closeConnection(connection);
    // TransportContextHolder.setTransportContext(previousTransportContext);
    // }
    // }

    /**
     * @param accessClient
     *            the accessClient to set
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

    public String getFedoraHost() {
        return fedoraHost;
    }

    public void setFedoraHost(String fedoraHost) {
        this.fedoraHost = fedoraHost;
    }
}
