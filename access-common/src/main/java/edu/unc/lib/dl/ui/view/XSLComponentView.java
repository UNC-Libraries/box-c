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
package edu.unc.lib.dl.ui.view;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;

/**
 * 
 * @author bbpennel
 *
 */
public class XSLComponentView {
    private Transformer transformer;
    private String source;
    private final List<Namespace> namespaces;

    public XSLComponentView(String source) throws Exception {
        this.source = source;
        namespaces = null;
    }

    public XSLComponentView(String source, Map<String, String> namespaces) throws Exception {
        this.source = source;
        this.namespaces = new ArrayList<Namespace>();
        for (Map.Entry<String, String> namespace : namespaces.entrySet()) {
            this.namespaces.add(Namespace.getNamespace(namespace.getKey(), namespace.getValue()));
        }
    }

    public void initializeTransformer() throws Exception {
        InputStream stream = this.getClass().getResourceAsStream(source);
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setURIResolver(new URIResolver() {
            public Source resolve(String href, String base)
                    throws TransformerException {
                Source result = null;
                if (href.startsWith("/")) {
                    result = new StreamSource(XSLComponentView.class
                            .getResourceAsStream(href));
                } else {
                    result = new StreamSource(XSLComponentView.class
                            .getResourceAsStream("classpath:" + href));
                }
                return result;
            }
        });
        transformer = factory.newTemplates(new StreamSource(stream))
                .newTransformer();
    }

    public String renderView(Element doc) throws TransformerException {
        return renderView(doc, null);
    }

    /**
     * Transforms the given document into a string using the XSL transformation assigned to this object.
     * 
     * @param doc
     * @param parameters
     * @return
     * @throws TransformerException
     */
    public String renderView(Element doc, Map<String, Object> parameters) throws TransformerException {
        JDOMResult result = new JDOMResult();

        // Since we are reusing the same transformer, have to make sure it is thread safe when transforming
        synchronized (this) {
            if (parameters != null) {
                for (Map.Entry<String, Object> parameterPair : parameters.entrySet()) {
                    transformer.setParameter(parameterPair.getKey(), parameterPair.getValue());
                }
            }

            try {
                transformer.transform(new JDOMSource(doc), result);
            } finally {
                transformer.reset();
            }
        }

        Element rootElement = result.getDocument().getRootElement();
        if (rootElement.getChildren().size() == 0) {
            return null;
        }

        if (namespaces != null) {
            for (Namespace namespace : namespaces) {
                rootElement.removeNamespaceDeclaration(namespace);
            }
        }
        XMLOutputter out = new XMLOutputter();
        return out.outputString(rootElement.getChildren());
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}