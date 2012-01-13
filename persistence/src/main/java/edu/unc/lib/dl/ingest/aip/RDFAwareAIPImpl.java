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
package edu.unc.lib.dl.ingest.aip;

import static org.jrdf.graph.AnyObjectNode.ANY_OBJECT_NODE;
import static org.jrdf.graph.AnyPredicateNode.ANY_PREDICATE_NODE;
import static org.jrdf.graph.AnySubjectNode.ANY_SUBJECT_NODE;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.jrdf.JRDFFactory;
import org.jrdf.SortedMemoryJRDFFactoryImpl;
import org.jrdf.graph.BlankNode;
import org.jrdf.graph.Graph;
import org.jrdf.graph.GraphElementFactoryException;
import org.jrdf.graph.GraphException;
import org.jrdf.graph.Literal;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.TripleFactoryException;
import org.jrdf.graph.URIReference;
import org.jrdf.parser.ParseException;
import org.jrdf.parser.Parser;
import org.jrdf.parser.StatementHandlerException;
import org.jrdf.parser.rdfxml.GraphRdfXmlParser;
import org.jrdf.util.ClosableIterator;
import org.jrdf.util.EscapeURL;
import org.jrdf.writer.BlankNodeRegistry;
import org.jrdf.writer.RdfNamespaceMap;
import org.jrdf.writer.mem.BlankNodeRegistryImpl;
import org.jrdf.writer.mem.RdfNamespaceMapImpl;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.util.ContainerPlacement;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Extends the IngestContext behavior (in Decorator fashion) to allow query and manipulation of RELS-EXT RDF through an
 * in-memory graph. The in-memory graph is an efficient way of expressing object properties and relationships and can be
 * used to perform business logic and add properties without keep the RDF XML.
 *
 * @author count0
 *
 */
public class RDFAwareAIPImpl implements ArchivalInformationPackage {
	private static final JRDFFactory JRDF_FACTORY = SortedMemoryJRDFFactoryImpl.getFactory();

	public static RDFAwareAIPImpl getInstance(ArchivalInformationPackage aip) throws AIPException {
		RDFAwareAIPImpl result;
		if (aip instanceof RDFAwareAIPImpl) {
			result = (RDFAwareAIPImpl) aip;
		} else {
			result = new RDFAwareAIPImpl(aip);
		}
		return result;
	}

	private static final Log log = LogFactory.getLog(RDFAwareAIPImpl.class);
	private final ArchivalInformationPackage baseAIP;
	private Graph graph;

	/**
	 * RDFIngestContext can be used as a decorator to add RDF graph capabilities in runtime.
	 *
	 * @param baseAIP
	 *           an ingest context
	 */
	public RDFAwareAIPImpl(ArchivalInformationPackage baseAIP) throws AIPException {
		this.baseAIP = baseAIP;
		this.graph = JRDF_FACTORY.getNewGraph();
		addRELSEXT2Graph();
		addFedoraLabel2Graph();
		if (log.isInfoEnabled())
			printGraph();
	}

	/**
     *
     */
	private void addFedoraLabel2Graph() {
		for (PID pid : this.baseAIP.getPIDs()) {
			Document foxml = this.baseAIP.getFOXMLDocument(pid);
			String label = FOXMLJDOMUtil.getLabel(foxml);
			try {
				if (label != null) {
					JRDFGraphUtil.addTriple(graph, new URI(pid.getURI()), ContentModelHelper.FedoraProperty.label.getURI(),
							label);
				}
			} catch (URISyntaxException e) {
				throw new Error("unexpected exception forming uri for string:" + pid.getURI(), e);
			}
		}
	}

	public void addRELSEXT2Graph() throws AIPException {
		URL url = null;
		try {
			url = new URL("http://example.com/");
		} catch (MalformedURLException e) {
			throw new Error("programming error", e);
		}
		for (PID pid : this.baseAIP.getPIDs()) {
			Document doc = this.baseAIP.getFOXMLDocument(pid);
			String str = getRELSEXT(doc);
			if (str == null) {
				continue;
			}
			StringReader r = new StringReader(str);
			try {
				Parser parser = new GraphRdfXmlParser(this.graph);
				parser.parse(r, EscapeURL.toEscapedString(url));
			} catch (GraphException e) {
				log.error(e);
			} catch (IOException e) {
				log.error(e);
			} catch (ParseException e) {
				log.error(e);
			} catch (StatementHandlerException e) {
				log.error(e);
			} finally {
				r.close();
			}
		}
	}

	/**
	 * Call this method when you are done making modifications to the RDF Graph that may be needed by ingest filters that
	 * are not RDF-aware. This method persists the RDF Graph in individual FOXML RELS-EXT datastreams.
	 */
	public void commitGraphChanges() throws IngestException {
		log.debug("Commiting graph changes to FOXML");
		if (log.isDebugEnabled())
			printGraph();
		for (PID pid : this.baseAIP.getPIDs()) {
			Document doc = this.baseAIP.getFOXMLDocument(pid);
			saveFOXMLDocument(pid, doc);
		}
	}

	@Override
	public void delete() {
		this.baseAIP.delete();
	}

	@Override
	public PremisEventLogger getEventLogger() {
		return this.baseAIP.getEventLogger();
	}

	@Override
	public File getFileForUrl(String path) {
		return this.baseAIP.getFileForUrl(path);
	}

	@Override
	public Document getFOXMLDocument(PID pid) {
		return this.baseAIP.getFOXMLDocument(pid);
	}

	@Override
	public File getFOXMLFile(PID pid) {
		return this.baseAIP.getFOXMLFile(pid);
	}

	public Graph getGraph() {
		return this.graph;
	}

	@Override
	public Set<PID> getPIDs() {
		return this.baseAIP.getPIDs();
	}

	private String getRELSEXT(Document foxml) {
		String result = null;
		// get the RELS-EXT element
		Namespace foxmlNS = Namespace.getNamespace("foxml", JDOMNamespaceUtil.FOXML_NS.getURI());
		Namespace rdfNS = Namespace.getNamespace("rdf", JDOMNamespaceUtil.RDF_NS.getURI());
		XPath relsNodeXPath;
		try {
			relsNodeXPath = XPath
					.newInstance("/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF");
			relsNodeXPath.addNamespace(foxmlNS);
			relsNodeXPath.addNamespace(rdfNS);
			Object o = relsNodeXPath.selectSingleNode(foxml);
			if (o == null) {
				log.debug("RELS-EXT not found");
				return null;
			}
			Element el = (Element) o;
			StringWriter sw = new StringWriter();
			XMLOutputter outputter = new XMLOutputter();
			try {
				outputter.output(el, sw);
			} catch (IOException e) {
				throw new Error("Unexpected Error: Failed to write out element string: " + el);
			}
			result = sw.toString();
		} catch (JDOMException e) {
			throw new Error(e);
		}
		return result;
	}

	@Override
	public File getTempFOXDir() {
		return this.baseAIP.getTempFOXDir();
	}

	// @Override
	// public Integer getTopPIDContainerOrder(PID toppid) {
	// return this.baseAIP.getTopPIDContainerOrder(toppid);
	// }
	//
	// @Override
	// public String getTopPIDContainerPath(PID pid) {
	// return this.baseAIP.getTopPIDContainerPath(pid);
	// }

	@Override
	public Set<PID> getTopPIDs() {
		return this.baseAIP.getTopPIDs();
	}

	/**
	 * Get the uri part of the namespace.
	 *
	 * @param resourceURI
	 *           String URI
	 * @return String namespace URI
	 */
	private String[] getURIParts(URIReference resource) {
		String[] result = new String[2];
		String resourceURI = resource.getURI().toString();
		int index1 = resourceURI.lastIndexOf('#');
		int index2 = resourceURI.lastIndexOf('/');
		int index = Math.max(index1, index2);
		result[0] = (index > 0 && index < resourceURI.length()) ? resourceURI.substring(0, ++index) : resourceURI;
		result[1] = (index > 0 && index < resourceURI.length()) ? resourceURI.substring(index, resourceURI.length())
				: resourceURI;
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.ReportingIngestBundle#prepareIngest()
	 */
	@Override
	public void prepareIngest(String message, String submitter) throws IngestException {
		log.debug("RDFIngestContext preparing for ingest");
		commitGraphChanges();
		this.baseAIP.prepareIngest(message, submitter);
	}

	public void printGraph() {
		log.debug("Printing the RDF Graph in the RDFIngestContext");
		ClosableIterator<Triple> iter = null;
		try {
			iter = this.graph.find(ANY_SUBJECT_NODE, ANY_PREDICATE_NODE, ANY_OBJECT_NODE);
			while (iter.hasNext()) {
				log.debug("Graph: " + iter.next());
			}
			log.debug("Total number of statements: " + this.graph.getNumberOfTriples());
		} catch (GraphException e) {
			log.error("problem with JRDF graph:", e);
		} finally {
			if (iter != null)
				iter.close();
		}
	}

	@Override
	public void saveFOXMLDocument(PID pid, Document doc) {
		RdfNamespaceMap nsmap = new RdfNamespaceMapImpl();
		BlankNodeRegistry blankReg = new BlankNodeRegistryImpl();
		try {
			nsmap.load(graph);
		} catch (GraphException e) {
			throw new Error("Programmer error", e);
		}

		Element rdfElement = new Element("RDF", JDOMNamespaceUtil.RDF_NS);
		HashMap<String, Namespace> namespacesMap = new HashMap<String, Namespace>();
		for (Map.Entry<String, String> name : nsmap.getNameEntries()) {
			String uri = name.getValue();
			Namespace ns = Namespace.getNamespace(name.getKey(), uri);
			namespacesMap.put(uri, ns);
			rdfElement.addNamespaceDeclaration(ns);
		}
		String subjectURI = "info:fedora/" + pid.getPid();
		Element rdfDescription = new Element("Description", JDOMNamespaceUtil.RDF_NS);
		rdfDescription.setAttribute("about", subjectURI);
		rdfElement.addContent(rdfDescription);

		// get the subject
		URIReference subject = null;
		try {
			subject = this.graph.getElementFactory().createResource(new URI(subjectURI));
		} catch (GraphElementFactoryException e) {
			throw new Error(e);
		} catch (URISyntaxException e) {
			throw new Error(e);
		}

		// look for triples about this pid
		ClosableIterator<Triple> tripleIter = null;
		try {
			Triple aboutSubject = graph.getTripleFactory().createTriple(subject, ANY_PREDICATE_NODE, ANY_OBJECT_NODE);
			tripleIter = graph.find(aboutSubject);
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				if (ContentModelHelper.FedoraProperty.label.getURI().toString().equals(t.getPredicate().toString())) {
					continue;
				}
				Element statement = writeSubjectAssumedTriple(t, namespacesMap, blankReg);
				rdfDescription.addContent(statement);
				// log.debug(statement);
			} // end of triple loop
			FOXMLJDOMUtil.setDatastreamXmlContent(doc, "RELS-EXT", "Relationship Metadata", rdfElement, false);
		} catch (GraphException e) {
			throw new Error(e);
		} catch (TripleFactoryException e) {
			throw new Error(e);
		} finally {
			if (tripleIter != null)
				tripleIter.close();
		}

		// rewrite the FOXML to the file
		this.baseAIP.saveFOXMLDocument(pid, doc);
	}

	// @Override
	// public void setTopPIDLocation(String containerPath, PID topPID, Integer
	// order) {
	// this.baseAIP.setTopPIDLocation(containerPath, topPID, order);
	// }

	public void setTopPIDs(Set<PID> topPIDs) {
		this.baseAIP.setTopPIDs(topPIDs);
	}

	/**
	 * Writes XML element(s) to represent an RDF triple where the subject is already implied by the surrounding XML.
	 *
	 * @param t
	 *           the triple to write
	 * @param namespacesMap
	 *           a map of namespaces in use
	 * @param blankReg
	 *           the blank node registry in use
	 * @return
	 */
	private Element writeSubjectAssumedTriple(Triple t, HashMap<String, Namespace> namespacesMap,
			BlankNodeRegistry blankReg) {
		URIReference pred = (URIReference) t.getPredicate();
		String[] parts = getURIParts(pred);
		Namespace ns = namespacesMap.get(parts[0]);
		Element statement = new Element(parts[1], ns);
		ObjectNode object = t.getObject();
		if (object instanceof Literal) {
			Literal litObject = (Literal) object;
			statement.setText(litObject.getEscapedLexicalForm());
			if (litObject.getDatatypeURI() != null) {
				String type = litObject.getDatatypeURI().toString();
				statement.setAttribute("datatype", type);
			}
			if (litObject.getLanguage() != null) {
				// not yet implemented
				// Element e = new Element("lang", Namespace.XML_NAMESPACE);
				// e.setText(litObject.getLanguage());
				// statement.addContent(e);
			}
		} else if (object instanceof URIReference) {
			URIReference uriObject = (URIReference) object;
			statement.setAttribute("resource", uriObject.getURI().toString());
		} else if (object instanceof BlankNode) { // its a blank node, some
			// recursion!
			BlankNode node = (BlankNode) object;
			ClosableIterator<Triple> blankTripleIter = null;
			try {
				Triple aboutBlankNode = graph.getTripleFactory().createTriple(node, ANY_PREDICATE_NODE, ANY_OBJECT_NODE);
				blankTripleIter = graph.find(aboutBlankNode);
				while (blankTripleIter.hasNext()) {
					Triple blankTrip = blankTripleIter.next();
					Element el = writeSubjectAssumedTriple(blankTrip, namespacesMap, blankReg);
					statement.addContent(el);
				}
			} catch (GraphException e) {
				throw new Error(e);
			} catch (TripleFactoryException e) {
				throw new Error(e);
			} finally {
				if (blankTripleIter != null)
					blankTripleIter.close();
			}
		}
		return statement;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage#getEmailRecipients()
	 */
	@Override
	public List<URI> getEmailRecipients() {
		return this.baseAIP.getEmailRecipients();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage#getSendEmail()
	 */
	@Override
	public boolean getSendEmail() {
		return this.baseAIP.getSendEmail();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage#setEmailRecipients (java.util.List)
	 */
	@Override
	public void setEmailRecipients(List<URI> recipients) {
		this.baseAIP.setEmailRecipients(recipients);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage#setSendEmail(boolean )
	 */
	@Override
	public void setSendEmail(boolean sendEmail) {
		this.baseAIP.setSendEmail(sendEmail);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage#getTopPIDPlacement (edu.unc.lib.dl.fedora.PID)
	 */
	@Override
	public ContainerPlacement getContainerPlacement(PID pid) {
		return this.baseAIP.getContainerPlacement(pid);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage#setTopPIDPlacement (java.lang.String,
	 * edu.unc.lib.dl.fedora.PID, java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public void setContainerPlacement(PID parentPID, PID topPID, Integer designatedOrder, Integer sipOrder) {
		this.baseAIP.setContainerPlacement(parentPID, topPID, designatedOrder, sipOrder);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		// cleanup graph, etc.
		if (this.graph != null) {
			this.graph.close();
			this.graph = null;
		}
	}
}
