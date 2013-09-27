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

import static org.jrdf.graph.AnyObjectNode.ANY_OBJECT_NODE;
import static org.jrdf.graph.AnySubjectNode.ANY_SUBJECT_NODE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jrdf.graph.Graph;
import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.GraphElementFactoryException;
import org.jrdf.graph.GraphException;
import org.jrdf.graph.Literal;
import org.jrdf.graph.Triple;
import org.jrdf.graph.TripleFactory;
import org.jrdf.graph.TripleFactoryException;
import org.jrdf.graph.URIReference;
import org.jrdf.util.ClosableIterator;

import edu.unc.lib.dl.fedora.PID;

public class JRDFGraphUtil {
	private static final Log log = LogFactory.getLog(JRDFGraphUtil.class);

	public static String getRelatedLiteralObject(Graph graph, PID pid, URI predicate) {
		String result = null;
		ClosableIterator<Triple> tripleIter = null;
		try {
			URIReference subject = graph.getElementFactory().createResource(new URI("info:fedora/" + pid.getPid()));
			URIReference pred = graph.getElementFactory().createResource(predicate);
			Triple findTop = graph.getTripleFactory().createTriple(subject, pred, ANY_OBJECT_NODE);
			tripleIter = graph.find(findTop);
			if (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				Literal n = (Literal) t.getObject();
				result = n.getLexicalForm();
			}
		} catch (GraphException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (TripleFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (GraphElementFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (URISyntaxException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} finally {
			if (tripleIter != null)
				tripleIter.close();
		}
		return result;
	}

	public static void removeAllRelatedByPredicate(Graph graph, PID pid, URI predicate) {
		ClosableIterator<Triple> tripleIter = null;
		try {
			URIReference subject = graph.getElementFactory().createResource(new URI("info:fedora/" + pid.getPid()));
			URIReference pred = graph.getElementFactory().createResource(predicate);
			Triple findTop = graph.getTripleFactory().createTriple(subject, pred, ANY_OBJECT_NODE);
			tripleIter = graph.find(findTop);
			graph.remove(tripleIter);
		} catch (GraphException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (TripleFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (GraphElementFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (URISyntaxException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} finally {
			if (tripleIter != null)
				tripleIter.close();
		}
	}
	
	public static List<String> getRelationshipLiteralObjects(Graph graph, PID pid, URI predicate) {
		List<String> result = new ArrayList<String>();
		ClosableIterator<Triple> tripleIter = null;
		try {
			URIReference subject = graph.getElementFactory().createResource(new URI("info:fedora/" + pid.getPid()));
			URIReference pred = graph.getElementFactory().createResource(predicate);
			Triple findTop = graph.getTripleFactory().createTriple(subject, pred, ANY_OBJECT_NODE);
			tripleIter = graph.find(findTop);
			
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				if (t.getObject() instanceof Literal) {
					Literal n = (Literal) t.getObject();
					result.add(n.getLexicalForm());
				} if (t.getObject() instanceof URIReference) {
					URIReference n = (URIReference) t.getObject();
					result.add(n.getURI().toString());
				}
			}
		} catch (GraphException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (TripleFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (GraphElementFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (URISyntaxException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} finally {
			if (tripleIter != null)
				tripleIter.close();
		}
		return result;
	}

	public static List<URI> getRelationshipObjectURIs(Graph graph, PID pid, URI predicate) {
		List<URI> result = new ArrayList<URI>();
		ClosableIterator<Triple> tripleIter = null;
		try {
			URIReference subject = graph.getElementFactory().createResource(new URI("info:fedora/" + pid.getPid()));
			URIReference pred = graph.getElementFactory().createResource(predicate);
			Triple findTop = graph.getTripleFactory().createTriple(subject, pred, ANY_OBJECT_NODE);
			tripleIter = graph.find(findTop);
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				if (t.getObject() instanceof URIReference) {
					URIReference n = (URIReference) t.getObject();
					result.add(n.getURI());
				}
			}
		} catch (GraphException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (TripleFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (GraphElementFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (URISyntaxException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} finally {
			if (tripleIter != null)
				tripleIter.close();
		}
		return result;
	}

	public static PID getPIDRelationshipSubject(Graph graph, URI predicate, String objectString) {
		PID result = null;
		ClosableIterator<Triple> tripleIter = null;
		try {
			Literal objectLiteral = graph.getElementFactory().createLiteral(objectString);
			URIReference pred = graph.getElementFactory().createResource(predicate);
			Triple findTop = graph.getTripleFactory().createTriple(ANY_SUBJECT_NODE, pred, objectLiteral);
			tripleIter = graph.find(findTop);
			if (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				if (t.getSubject() instanceof URIReference) {
					URIReference n = (URIReference) t.getSubject();
					String res = n.getURI().toString();
					result = new PID(res.substring(res.indexOf("/") + 1));
				}
			}
		} catch (GraphException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (TripleFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (GraphElementFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} finally {
			if (tripleIter != null)
				tripleIter.close();
		}
		return result;
	}
	
	public static PID getPIDRelationshipSubject(Graph graph, URI predicate, PID objectPID) {
		PID result = null;
		ClosableIterator<Triple> tripleIter = null;
		try {
			URIReference object = graph.getElementFactory().createResource(new URI("info:fedora/" + objectPID.getPid()));
			URIReference pred = graph.getElementFactory().createResource(predicate);
			Triple findTop = graph.getTripleFactory().createTriple(ANY_SUBJECT_NODE, pred, object);
			tripleIter = graph.find(findTop);
			if (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				if (t.getSubject() instanceof URIReference) {
					URIReference n = (URIReference) t.getSubject();
					String res = n.getURI().toString();
					result = new PID(res.substring(res.indexOf("/") + 1));
				}
			}
		} catch (GraphException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (TripleFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (GraphElementFactoryException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} catch (URISyntaxException e) {
			log.error("programmer error: ", e);
			throw new Error(e);
		} finally {
			if (tripleIter != null)
				tripleIter.close();
		}
		return result;
	}

	/**
	 * Adds a triple statement to the supplied graph.
	 * 
	 * @param graph
	 *           the graph
	 * @param subject
	 *           URI of the subject
	 * @param predicate
	 *           URI of the predicate
	 * @param object
	 *           URI of the object
	 * @return true if successful
	 */
	public static boolean addTriple(Graph graph, URI subject, URI predicate, URI object) {
		boolean result = false;
		try {
			GraphElementFactory elementFactory = graph.getElementFactory();
			TripleFactory tripleFactory = graph.getTripleFactory();
			URIReference subjectRef = elementFactory.createResource(subject);
			URIReference predicateRef = elementFactory.createResource(predicate);
			URIReference objectRef = elementFactory.createResource(object);
			Triple uidTriple = tripleFactory.createTriple(subjectRef, predicateRef, objectRef);
			graph.add(uidTriple);
			result = true;
		} catch (GraphException ignored) {
		} catch (TripleFactoryException ignored) {
		} catch (GraphElementFactoryException ignored) {
		}
		return result;
	}

	/**
	 * Adds a triple statement to the supplied graph.
	 * 
	 * @param graph
	 *           the graph
	 * @param subject
	 *           URI of the subject
	 * @param predicate
	 *           URI of the predicate
	 * @param object
	 *           String literal object
	 * @return true if successful
	 */
	public static boolean addTriple(Graph graph, URI subject, URI predicate, String object) {
		boolean result = false;
		try {
			GraphElementFactory elementFactory = graph.getElementFactory();
			TripleFactory tripleFactory = graph.getTripleFactory();
			URIReference subjectRef = elementFactory.createResource(subject);
			URIReference predicateRef = elementFactory.createResource(predicate);
			Literal objectLit = elementFactory.createLiteral(object);
			Triple triple = tripleFactory.createTriple(subjectRef, predicateRef, objectLit);
			graph.add(triple);
			result = true;
		} catch (GraphException ignored) {
		} catch (TripleFactoryException ignored) {
		} catch (GraphElementFactoryException ignored) {
		}
		return result;
	}

	public static List<URI> getContentModels(Graph graph, PID pid) {
		return getRelationshipObjectURIs(graph, pid, ContentModelHelper.FedoraProperty.hasModel.getURI());
	}

	public static void addFedoraPIDRelationship(Graph graph, PID subject, ContentModelHelper.Relationship relationship,
			PID object) {
		try {
			addTriple(graph, new URI("info:fedora/" + subject.getPid()), relationship.getURI(), new URI("info:fedora/"
					+ object.getPid()));
		} catch (URISyntaxException e) {
			log.error("Unexpected exception", e);
			throw new Error("programmer error: ", e);
		}
	}

	public static void addCDRProperty(Graph graph, PID subject, ContentModelHelper.CDRProperty property, String literal) {
		try {
			addTriple(graph, new URI("info:fedora/" + subject.getPid()), property.getURI(), literal);
		} catch (URISyntaxException e) {
			log.error("Unexpected exception", e);
			throw new Error("programmer error: ", e);
		}
	}

	public static void addCDRProperty(Graph graph, PID subject, ContentModelHelper.CDRProperty property, URI object) {
		try {
			addTriple(graph, new URI("info:fedora/" + subject.getPid()), property.getURI(), object);
		} catch (URISyntaxException e) {
			log.error("Unexpected exception", e);
			throw new Error("programmer error: ", e);
		}
	}

	public static void addFedoraRelationship(Graph graph, PID subject, ContentModelHelper.Relationship relationship, String literal) {
		try {
			addTriple(graph, new URI("info:fedora/" + subject.getPid()), relationship.getURI(), literal);
		} catch (URISyntaxException e) {
			log.error("Unexpected exception", e);
			throw new Error("programmer error: ", e);
		}
	}
	
	public static void addFedoraProperty(Graph graph, PID subject, ContentModelHelper.FedoraProperty property, String literal) {
		try {
			addTriple(graph, new URI("info:fedora/" + subject.getPid()), property.getURI(), literal);
		} catch (URISyntaxException e) {
			log.error("Unexpected exception", e);
			throw new Error("programmer error: ", e);
		}
	}
	
	public static void addFedoraProperty(Graph graph, PID subject, ContentModelHelper.FedoraProperty property, URI object) {
		try {
			addTriple(graph, new URI("info:fedora/" + subject.getPid()), property.getURI(), object);
		} catch (URISyntaxException e) {
			log.error("Unexpected exception", e);
			throw new Error("programmer error: ", e);
		}
	}
}
