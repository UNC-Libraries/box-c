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
package edu.unc.lib.dl.data.ingest.solr;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

/**
 * Stores a list of documents that are transformed into a Solr update document format.
 * Entries in the list are individual XML operations, such as add or delete.  The list can
 * be exported to a single update document.  Should be thread-safe.
 *
 * @author bbpennel
 */
public class UpdateDocTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateDocTransformer.class);
    private List<Element> addDocElements;
    private List<Element> synchronizedAddDocElements;
    private String xslName = "generateAddDoc.xsl";
    private Transformer transformer;
    private List<Namespace> namespaces;


    public UpdateDocTransformer() {

        addDocElements = new ArrayList<Element>();
        synchronizedAddDocElements = Collections.synchronizedList(addDocElements);
        namespaces = new ArrayList<Namespace>();
        namespaces.add(Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/"));
        namespaces.add(Namespace.getNamespace("ns5", "http://cdr.unc.edu/definitions/1.0/base-model.xml#"));
        namespaces.add(Namespace.getNamespace("ns6", "info:fedora/fedora-system:def/model#"));
        namespaces.add(Namespace.getNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#"));
        namespaces.add(Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3"));
        namespaces.add(Namespace.getNamespace("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/"));
        namespaces.add(Namespace.getNamespace("foxml", "info:fedora/fedora-system:def/foxml#"));
        namespaces.add(Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"));
        namespaces.add(Namespace.getNamespace("dcterms", "http://purl.org/dc/terms/"));
        namespaces.add(Namespace.getNamespace("owl", "http://www.w3.org/2002/07/owl#"));
        namespaces.add(Namespace.getNamespace("cdr-fn", "http://cdr.lib.unc.edu/"));
    }

    /**
     * Initializes the transformer by retrieving the XSLT document to use and building a transformer from it.
     * @throws Exception
     */
    public void init() throws Exception {
        ApplicationContext ctx = new ClassPathXmlApplicationContext();
        Resource res = ctx.getResource("classpath:/transform/" + xslName);
        ((Closeable)ctx).close();
        Source transformSource = new StreamSource(res.getInputStream());
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setURIResolver(new URIResolver() {
            public Source resolve(String href, String base)
                    throws TransformerException {
                Source result = null;
                if (href.startsWith("/")) {
                    result = new StreamSource(UpdateDocTransformer.class
                            .getResourceAsStream(href));
                } else {
                    result = new StreamSource(UpdateDocTransformer.class
                            .getResourceAsStream("/transform/" + href));
                }
                return result;
            }
        });

        Templates transformTemplate = factory.newTemplates(transformSource);
        transformer = transformTemplate.newTransformer();
    }

    /**
     * Adds an add document element to the list
     * @param doc
     * @throws Exception
     */
    public synchronized void addDocument(Document doc) throws Exception {
        JDOMResult out = new JDOMResult();

        synchronized (transformer) {
            transformer.transform(new JDOMSource(doc), out);
        }
        Element rootElement = out.getDocument().getRootElement();
        for (Namespace namespace: namespaces) {
            rootElement.removeNamespaceDeclaration(namespace);
        }
        synchronizedAddDocElements.add(rootElement.detach());
        LOG.debug("Added " + doc.hashCode());
    }

    /**
     * Adds a "delete" pid update element to the list
     * @param pid
     * @throws Exception
     */
    public void deleteDocument(String pid) throws Exception {
        deleteQuery("id", pid);
    }

    public void deleteQuery(String query) throws Exception {
        deleteQuery("query", query);
    }

    public synchronized void deleteQuery(String field, String query) throws Exception {
        Element deleteElement = new Element("delete");
        Element deleteLimit = new Element(field);
        deleteLimit.setText(query);
        deleteElement.addContent(deleteLimit);
        synchronizedAddDocElements.add(deleteElement);
    }

    public void commit() throws Exception {
        Element commitElement = new Element("commit");
        synchronizedAddDocElements.add(commitElement);
    }

    public void clearDocs() {
        LOG.debug("Clearing update doc list");
        addDocElements.clear();
        this.synchronizedAddDocElements.clear();
    }

    /**
     * Returns the list of update documents as a single update document with
     * all the individual elements as children.
     */
    public String toString() {
        Document addDoc = new Document();
        Element addDocRoot = new Element("update");
        addDoc.setRootElement(addDocRoot);
        addDocRoot.addContent(addDocElements);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        return out.outputString(addDoc);
    }

    public synchronized String exportUpdateDocument() {
        String updateDocument = this.toString();
        this.clearDocs();
        return updateDocument;
    }

    public String getXslName() {
        return xslName;
    }

    public void setXslName(String xslName) {
        this.xslName = xslName;
    }

    public int getDocumentCount() {
        return this.addDocElements.size();
    }
}
