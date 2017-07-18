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
package edu.unc.lib.dl.schematron;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.TransformerFactoryImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * This class validates XML documents against ISO Schematron files. It can be
 * configured as a Spring bean or used separately. The bean must be initialized
 * through the loadSchemas() method after the map of named schemas is set.
 *
 * @author count0
 *
 */
public class SchematronValidator {
    private static final Log log = LogFactory.getLog(SchematronValidator.class);
    private Map<String, Resource> schemas = new HashMap<String, Resource>();
    private Map<String, Templates> templates = null;

    private static Filter<Element> failedAsserts = new ElementFilter(
            "failed-assert");

    public SchematronValidator() {
    }

    /**
     * Get the currently configured list of schemas
     *
     * @return a map of schema names to schematron resources
     */
    public Map<String, Resource> getSchemas() {
        return schemas;
    }

    /**
     * Reports true is the SVRL Document contains any failed assertions.
     *
     * @param svrl
     *            SVRL JDOM Document
     * @return true if assertions failed
     */
    public boolean hasFailedAssertions(Document svrl) {
        return svrl.getDescendants(failedAsserts).hasNext();
    }

    /**
     * Check whether a given document conforms to a known schema.
     *
     * @param resource
     *            a Spring resource that retrieves an XML stream
     * @param schema
     *            name of schema to use
     * @return true if document conforms to schema
     */
    public boolean isValid(Resource resource, String schema) throws IOException {
        Source source = new StreamSource(resource.getInputStream());
        return this.isValid(source, schema);
    }

    /**
     * Check whether a given document conforms to a known schema.
     *
     * @param source
     *            an XML source
     * @param schema
     *            name of schema to use
     * @return true if document conforms to schema
     */
    public boolean isValid(Source source, String schema) {
        Document svrl = this.validate(source, schema);
        if (this.hasFailedAssertions(svrl)) {
            String msg = "Schematron validation failed against schema: "
                    + schema;
            log.info(msg);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Use this to initialize the configured schemas. Generate stylesheet
     * implementations of ISO Schematron files and preload them into Transformer
     * Templates for quick use.
     */
    public void loadSchemas() {
        templates = new HashMap<String, Templates>();
        // Load up a transformer and the ISO Schematron to XSL templates.
        Templates isoSVRLTemplates = null;
        ClassPathResource svrlRes = new ClassPathResource(
                "/edu/unc/lib/dl/schematron/iso_svrl.xsl",
                SchematronValidator.class);
        Source svrlrc;
        try {
            svrlrc = new StreamSource(svrlRes.getInputStream());
        } catch (IOException e1) {
            throw new Error("Cannot load iso_svrl.xsl", e1);
        }
        TransformerFactory factory = null;
        try {
            factory = new TransformerFactoryImpl();
            // enable relative classpath-based URIs
            factory.setURIResolver(new URIResolver() {
                public Source resolve(String href, String base)
                        throws TransformerException {
                    ClassPathResource svrlRes = new ClassPathResource(href,
                            SchematronValidator.class);
                    Source result;
                    try {
                        result = new StreamSource(svrlRes.getInputStream());
                    } catch (IOException e1) {
                        throw new TransformerException(
                                "Cannot resolve " + href, e1);
                    }
                    return result;
                }
            });
            isoSVRLTemplates = factory.newTemplates(svrlrc);
        } catch (TransformerFactoryConfigurationError e) {
            log.error("Error setting up transformer factory.", e);
            throw new Error("Error setting up transformer factory", e);
        } catch (TransformerConfigurationException e) {
            log.error("Error setting up transformer.", e);
            throw new Error("Error setting up transformer", e);
        }

        // Get a transformer
        Transformer t = null;
        try {
            t = isoSVRLTemplates.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new Error("There was a problem configuring the transformer.",
                    e);
        }

        for (String schema : schemas.keySet()) {
            // make XSLT out of Schematron for each schema
            Resource resource = schemas.get(schema);
            Source schematron = null;
            try {
                schematron = new StreamSource(resource.getInputStream());
            } catch (IOException e) {
                throw new Error("Cannot load resource for schema \"" + schema
                        + "\" at " + resource.getDescription()
                        + resource.toString());
            }
            JDOMResult res = new JDOMResult();
            try {
                t.transform(schematron, res);
            } catch (TransformerException e) {
                throw new Error(
                        "Schematron issue: There were problems transforming Schematron to XSL.",
                        e);
            }

            // compile templates object for each profile
            try {
                Templates schemaTemplates = factory
                        .newTemplates(new JDOMSource(res.getDocument()));
                templates.put(schema, schemaTemplates);
            } catch (TransformerConfigurationException e) {
                throw new Error(
                        "There was a problem configuring the transformer.", e);
            }
        }

    }

    /**
     * Set the map of configured schema names and locations (uses the Spring
     * Resource interface)
     *
     * @param schemas
     */
    public void setSchemas(Map<String, Resource> schemas) {
        this.schemas = schemas;
    }

    /**
     * This is the lowest level validation call, which returns an XML validation
     * report in schematron output format. (see http://purl.oclc.org/dsdl/svrl)
     *
     * @param resource
     *            any type of Spring resource
     * @param schema
     *            name of schema to use
     * @return schematron output document
     * @throws IOException
     *             when resource cannot be read
     */
    public Document validate(Resource resource, String schema)
            throws IOException {
        Source source = new StreamSource(resource.getInputStream());
        return this.validate(source, schema);
    }

    /**
     * This is the lowest level validation call, which returns an XML validation
     * report in schematron output format. (see http://purl.oclc.org/dsdl/svrl)
     *
     * @param source
     *            XML Source to validate
     * @param schema
     *            name of the schema to use
     * @return schematron output document
     */
    public Document validate(Source source, String schema) {
        // lookup templates object
        Templates template = templates.get(schema);
        if (template == null) {
            throw new Error("Unknown Schematron schema name: " + schema);
        }

        // get a transformer
        Transformer t = null;
        try {
            t = template.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new Error("There was a problem configuring the transformer.",
                    e);
        }

        // call the transform
        JDOMResult svrlRes = new JDOMResult();
        try {
            t.transform(source, svrlRes);
        } catch (TransformerException e) {
            throw new Error(
                    "There was a problem running Schematron validation XSL.", e);
        }
        return svrlRes.getDocument();
    }

    public List<String> validateReportErrors(Source source, String schema) {
        Document svrl = this.validate(source, schema);
        return parseSVRLErrors(svrl);
    }

    public static List<String> parseSVRLErrors(Document svrl) {
        List<String> result = new ArrayList<String>();
        @SuppressWarnings("rawtypes")
        Iterator desc = svrl.getDescendants(failedAsserts);
        if (desc.hasNext()) {
            while (desc.hasNext()) {
                Element failedAssert = (Element) desc.next();
                result.add(failedAssert.getChildText("text",
                        JDOMNamespaceUtil.SCHEMATRON_VALIDATION_REPORT_NS));
            }
        }
        return result;
    }

}
