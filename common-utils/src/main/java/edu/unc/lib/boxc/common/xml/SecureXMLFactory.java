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
package edu.unc.lib.boxc.common.xml;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jdom2.input.SAXBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import java.util.List;

/**
 * Factory for common XML based classes with XXE turned off and other security settings
 *
 * @author bbpennel
 */
public class SecureXMLFactory {

    private static final String SAXON_FEATURE_PREFIX = "http://saxon.sf.net/feature/parserFeature?uri=";
    private static final String DISALLOW_DOC_TYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAM_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";

    /**
     * Private constructor
     */
    private SecureXMLFactory() {
    }

    /**
     * @return a new non-validating SAXBuilder with default security settings
     */
    public static SAXBuilder createSAXBuilder() {
        SAXBuilder builder = new SAXBuilder();
        builder.setFeature(DISALLOW_DOC_TYPE,true);
        builder.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
        builder.setFeature(EXTERNAL_PARAM_ENTITIES, false);
        return builder;
    }

    /**
     * @return a new XMLInputFactory with XXE disabled. Generally used for creating stAX readers
     */
    public static XMLInputFactory createXMLInputFactory() {
        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        xmlFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return xmlFactory;
    }

    /**
     * @param relativeClass class to resolve resources relative to
     * @return a new XSLT 2.0 TransformerFactory which can resolve URIs to resources at
     *    the path of the provided class.
     */
    public static TransformerFactory createTransformerFactory(Class<?> relativeClass) {
        TransformerFactory tf = new net.sf.saxon.TransformerFactoryImpl();
        // Disable external entities within the XMLReader used by the transformer
        tf.setAttribute(SAXON_FEATURE_PREFIX + DISALLOW_DOC_TYPE, true);
        tf.setAttribute(SAXON_FEATURE_PREFIX + EXTERNAL_GENERAL_ENTITIES, false);
        tf.setAttribute(SAXON_FEATURE_PREFIX + EXTERNAL_PARAM_ENTITIES, false);
        tf.setURIResolver(new URIResolver() {
            @Override
            public Source resolve(String href, String base) throws TransformerException {
                relativeClass.getResourceAsStream(href);
                try {
                    return new StreamSource(relativeClass.getResourceAsStream(href));
                } catch (Exception e) {
                    throw new TransformerException("Cannot resolve " + href, e);
                }
            }
        });
        return tf;
    }

    /**
     * @return create a schema factory with default xxe settings.
     */
    public static SchemaFactory createSchemaFactory() {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new RuntimeException("Unable to configure schema factory", e);
        }
        return factory;
    }

    /**
     * Create a Schema instance initialized with the provided Sources
     * @param schemaFactory
     * @param sources list of source paths, typically in "/schemas/xml.xsd" format
     * @return new Schema object
     */
    public static Schema createSchema(SchemaFactory schemaFactory, List<String> sources) throws SAXException {
        var sourceArray = sources.stream()
                .map(s -> new StreamSource(SecureXMLFactory.class.getResourceAsStream(s)))
                .toArray(Source[]::new);
        return schemaFactory.newSchema(sourceArray);
    }
}
