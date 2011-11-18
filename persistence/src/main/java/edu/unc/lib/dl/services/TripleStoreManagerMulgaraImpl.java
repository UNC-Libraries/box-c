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
package edu.unc.lib.dl.services;

import java.io.StringReader;
import java.rmi.RemoteException;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.input.DOMBuilder;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.CDATASection;
import org.w3c.dom.NodeList;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Provides an adapter for querying and modifying the triple store.
 * 
 * @author count0
 * 
 */
public class TripleStoreManagerMulgaraImpl implements TripleStoreManager {
	// private static final String kruleOwlPath = "classpath:mulgara/krule.owl";
	// private static final String kruleRdfPath =
	// "classpath:mulgara/rdfs-krule.rdf";
	private static final Log log = LogFactory.getLog(TripleStoreManagerMulgaraImpl.class);
	private String inferenceRulesModelUri;
	private String inferredRIModelUri;

	private String itqlEndpointURL;

	private String mulgaraInstallFilePath;
	private String name;
	private String pass;
	private String resourceIndexModelUri;
	private String serverModelUri;

	// private final File tempKruleOwl = null;
	// private final File tempKruleRdf = null;

	public TripleStoreManagerMulgaraImpl() {
		// copies the classpath resources to the temp folder for access by the
		// itql client.
		// yeah, it's a hack, but better than relying on files placed in
		// specific locations
		// on the mulgara server.
		/*
		 * try { this.tempKruleOwl = File.createTempFile("krule-", ".owl"); this.tempKruleRdf =
		 * File.createTempFile("krule-", ".rdf"); // dump rules files into temporary space for ITQL loading
		 * BufferedInputStream isOwl = new BufferedInputStream(this.getClass().getClassLoader().getResourceAsStream (
		 * TripleStoreQueryServiceMulgaraImpl.kruleOwlPath)); BufferedOutputStream osOwl = new BufferedOutputStream(new
		 * FileOutputStream(this.tempKruleOwl)); byte[] buf = new byte[1024]; int len; while ((len = isOwl.read(buf)) > 0)
		 * { osOwl.write(buf, 0, len); } isOwl.close(); osOwl.close();
		 * 
		 * BufferedInputStream isRdf = new BufferedInputStream(this.getClass().getClassLoader ().getResourceAsStream(
		 * TripleStoreQueryServiceMulgaraImpl.kruleRdfPath)); BufferedOutputStream osRdf = new BufferedOutputStream(new
		 * FileOutputStream(this.tempKruleRdf)); while ((len = isRdf.read(buf)) > 0) { osRdf.write(buf, 0, len); }
		 * isRdf.close(); osRdf.close(); } catch (IOException e) { throw new Error("There was a problem initializing the
		 * Triple Store Service", e); }
		 */
	}

	public String getInferenceRulesModelUri() {
		return this.inferenceRulesModelUri;
	}

	public String getInferredModelUri() {
		return this.inferredRIModelUri;
	}

	public String getItqlEndpointURL() {
		return itqlEndpointURL;
	}

	/**
	 * Sets up the Mulgara triple store to process inferences based on the Fedora external relationship ontology.
	 */
	// private void setupInferenceProcessing() {
	//
	// // create utility models
	// log.info(this.storeCommand("create <rmi://localhost/server1#type>
	// <mulgara:TypeModel>;"));
	// log.info(this.storeCommand("create <rmi://localhost/server1#prefix>
	// <mulgara:PrefixModel>;"));
	//
	// // clear and create the rules model
	// log.info(this.storeCommand(String.format("drop <%1$s>;",
	// this.getInferenceRulesModelUri())));
	// log.info(this.storeCommand(String.format("create <%1$s>;",
	// this.getInferenceRulesModelUri())));
	//
	// // load rules ontology (owl) and the rules for how to process RDFS
	// // semantics.
	// try {
	// this.loadData(this.getInferenceRulesModelUri(), this.tempKruleOwl);
	// this.loadData(this.getInferenceRulesModelUri(), this.tempKruleRdf);
	// } catch (Exception e) {
	// throw new Error("There was an error while loading inferencing data", e);
	// }
	//
	// // insert RELS-EXT RDFS schema into the RI model
	// // this.sendTQL(String.format("load
	// // <file:%1$s/rules/fedora-rels-ext-ontology.n3> into <%2$s>;", this
	// // .getMulgaraInstallFilePath(), this.getResourceIndexModelUri()));
	// }
	// public void loadData(String model, File data) throws Exception {
	// // define the file we want to load
	// URI dataFile = data.toURI();
	// URI targetModel;
	// URI serverModel;
	// try {
	// targetModel = new URI(model);
	// serverModel = new URI(this.getServerModelUri());
	// } catch (URISyntaxException e) {
	// throw new Error("Invalid URIs passed to loadData", e);
	// }
	//
	// // Create a factory, and connect to the server
	// ConnectionFactory factory = new ConnectionFactory();
	// Connection connection;
	// try {
	// connection = factory.newConnection(serverModel);
	// } catch (ConnectionException e) {
	// throw new Error(e);
	// }
	//
	// try {
	// // execute a LOAD command
	// connection.execute(new Load(dataFile, targetModel, true));
	// // cleaning up the connection allows the network resources to be
	// // re-used
	// connection.close();
	// } catch (QueryException e) {
	// throw new Exception("There was a problem loading your data file.", e);
	// }
	// }
	public String getMulgaraInstallFilePath() {
		return mulgaraInstallFilePath;
	}

	public String getName() {
		return name;
	}

	public String getPass() {
		return pass;
	}

	public String getResourceIndexModelUri() {
		return resourceIndexModelUri;
	}

	public String getServerModelUri() {
		return serverModelUri;
	}

	/**
	 * Gets any Mulgara service ready to house repository statements from scratch. Sets up an empty model for the
	 * resource index Sets up the rules and schema models for entailment. Sets up a target model for entailed statements.
	 */
	public void initialize() {
		// clear and create the resource index model
		log.info(this.storeCommand(String.format("drop <%1$s>;", this.getResourceIndexModelUri())));
		log.info(this.storeCommand(String.format("create <%1$s>;", this.getResourceIndexModelUri())));

		// set up the entailment stuff
		// this.setupInferenceProcessing();

		// reindex Fedora RELS-EXT
	}

	/**
	 * This method will cause the resource index model to be reloaded from RELS-EXT datastreams. Digital Object updates
	 */
	public void reindexDigitalObjects() {

	}

	private void reportSOAPFault(SOAPMessage reply) throws SOAPException {
		String error = reply.getSOAPBody().getFirstChild().getTextContent();
		throw new Error("There was a SOAP Fault from Mulgara: " + error);
	}

	private String sendTQL(String query) {
		log.info(query);
		String result = null;
		try {
			// First create the connection
			SOAPConnectionFactory soapConnFactory = SOAPConnectionFactory.newInstance();
			SOAPConnection connection = soapConnFactory.createConnection();

			// Next, create the actual message
			MessageFactory messageFactory = MessageFactory.newInstance();
			SOAPMessage message = messageFactory.createMessage();
			message.getMimeHeaders().setHeader("SOAPAction", "itqlbean:executeQueryToString");
			SOAPBody soapBody = message.getSOAPPart().getEnvelope().getBody();
			soapBody.addNamespaceDeclaration("xsd", JDOMNamespaceUtil.XSD_NS.getURI());
			soapBody.addNamespaceDeclaration("xsi", JDOMNamespaceUtil.XSI_NS.getURI());
			soapBody.addNamespaceDeclaration("itqlbean", this.getItqlEndpointURL());
			SOAPElement eqts = soapBody.addChildElement("executeQueryToString", "itqlbean");
			SOAPElement queryStr = eqts.addChildElement("queryString", "itqlbean");
			queryStr.setAttributeNS(JDOMNamespaceUtil.XSI_NS.getURI(), "xsi:type", "xsd:string");
			CDATASection queryCDATA = message.getSOAPPart().createCDATASection(query);
			queryStr.appendChild(queryCDATA);
			message.saveChanges();
			SOAPMessage reply = connection.call(message, this.getItqlEndpointURL());

			if (reply.getSOAPBody().hasFault()) {
				reportSOAPFault(reply);
				if (log.isDebugEnabled()) {
					// log the full soap body response
					DOMBuilder builder = new DOMBuilder();
					org.jdom.Document jdomDoc = builder.build(reply.getSOAPBody().getOwnerDocument());
					log.info(new XMLOutputter().outputString(jdomDoc));
				}
			} else {
				NodeList nl = reply.getSOAPPart().getEnvelope().getBody()
						.getElementsByTagNameNS("*", "executeQueryToStringReturn");
				if (nl.getLength() > 0) {
					result = nl.item(0).getTextContent();
				}
				log.debug(result);
			}
		} catch (SOAPException e) {
			throw new Error("Cannot query triple store at " + this.getItqlEndpointURL(), e);
		}
		return result;
	}

	public void setItqlEndpointURL(String baseURL) {
		this.itqlEndpointURL = baseURL;
	}

	public void setMulgaraInstallFilePath(String mulgaraInstallFilePath) {
		this.mulgaraInstallFilePath = mulgaraInstallFilePath;
	}

	// SERVICE ADMINISTRATION METHODS (initialize, reindex, reinfer)

	public void setName(String name) {
		this.name = name;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public void setServerModelUri(String serverModelUri) {
		this.serverModelUri = serverModelUri;
		this.resourceIndexModelUri = serverModelUri + "ri";
		this.inferredRIModelUri = serverModelUri + "ri_inferred";
		this.inferenceRulesModelUri = serverModelUri + "krules";
	}

	/**
	 * @param query
	 *           an ITQL command
	 * @return the message returned by Mulgara
	 * @throws RemoteException
	 *            for communication failure
	 */
	public String storeCommand(String query) {
		String result = null;
		String response = this.sendTQL(query);
		if (response != null) {
			StringReader sr = new StringReader(response);
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
			XMLEventReader r = null;
			try {
				boolean inMessage = false;
				StringBuffer message = new StringBuffer();
				r = factory.createXMLEventReader(sr);
				while (r.hasNext()) {
					XMLEvent e = r.nextEvent();
					if (e.isStartElement()) {
						StartElement s = e.asStartElement();
						if ("message".equals(s.getName().getLocalPart())) {
							inMessage = true;
						}
					} else if (e.isEndElement()) {
						EndElement end = e.asEndElement();
						if ("message".equals(end.getName().getLocalPart())) {
							inMessage = false;
						}
					} else if (inMessage && e.isCharacters()) {
						message.append(e.asCharacters().getData());
					}
				}
				result = message.toString();
			} catch (XMLStreamException e) {
				e.printStackTrace();
			} finally {
				if (r != null) {
					try {
						r.close();
					} catch (Exception ignored) {
						log.error(ignored);
					}
				}

			}
			sr.close();
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.services.TripleStoreService#updateCoreTransactionalInferences ()
	 */
	public void updateEntailedModel() {
		// inferencing is already configure by initialize.
		// clear and create the model for the entailed data
		this.sendTQL(String.format("drop <%1$s>;", this.getInferredModelUri()));
		this.sendTQL(String.format("create <%1$s>;", this.getInferredModelUri()));

		// apply all the rules to the input model
		this.sendTQL(String.format("apply <%1$s> to <%2$s> <%3$s>;", this.getInferenceRulesModelUri(),
				this.getResourceIndexModelUri(), this.getInferredModelUri()));
	}

}
