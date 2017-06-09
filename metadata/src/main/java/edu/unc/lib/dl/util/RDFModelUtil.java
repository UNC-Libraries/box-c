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
package edu.unc.lib.dl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

/**
 * Utility containing common methods for manipulating and transforming RDF
 * models
 * 
 * @author bbpennel
 *
 */
public class RDFModelUtil {

    public final static String TURTLE_MIMETYPE = "text/turtle";

    private RDFModelUtil() {

    }

    public static void serializeModel(Model model, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            RDFDataMgr.write(fos, model, RDFFormat.TURTLE_PRETTY);
        }
    }

    /**
     * Serializes and streams the provided model as serialized turtle
     * 
     * @param model
     * @return
     * @throws IOException
     */
    public static InputStream streamModel(Model model) throws IOException {
        return streamModel(model, RDFFormat.TURTLE_PRETTY);
    }

    /**
     * Serializes and streams the provided model, using the specified format
     * 
     * @param model
     * @param format
     * @return
     * @throws IOException
     */
    public static InputStream streamModel(Model model, RDFFormat format) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            RDFDataMgr.write(bos, model, format);
            return new ByteArrayInputStream(bos.toByteArray());
        }
    }

    /**
     * Returns a model built from the given turtle input stream
     * 
     * @param inStream
     * @return
     */
    public static Model createModel(InputStream inStream) {
        Model model = ModelFactory.createDefaultModel();
        model.read(inStream, null, "TURTLE");
        return model;
    }

    /**
     * Convert the given model into a SPARQL update query which inserts all of
     * the properties in the model.
     * 
     * @param model
     * @return sparql update query which adds all properties from the given
     *         model
     */
    public static String createSparqlInsert(Model model) {
        StringBuilder query = new StringBuilder();
        query.append("INSERT {\n");

        ResIterator it = model.listSubjects();
        while (it.hasNext()) {
            Resource resc = it.nextResource();
            String currentUri = resc.getURI();

            StmtIterator pIt = resc.listProperties();
            while (pIt.hasNext()) {
                Statement property = pIt.nextStatement();
                query.append(" <").append(currentUri).append('>').append(" <")
                        .append(property.getPredicate().getURI()).append("> ");
                if (property.getObject().isResource()) {
                    query.append('<')
                            .append(property.getObject().asResource().getURI())
                            .append('>');
                } else {
                    Node node = property.getObject().asNode();
                    query.append('"').append(node.getLiteralLexicalForm())
                            .append('"');
                    String typeUri = node.getLiteralDatatypeURI();
                    if (typeUri != null) {
                        query.append("^^<")
                                .append(node.getLiteralDatatypeURI())
                                .append('>');
                    }
                }
                query.append(" .\n");
            }

        }

        query.append("}\nWHERE {}");

        return query.toString();
    }

    public static String createSparqlInsert(String subjUri, Property property,
            Resource object) {
        return buildSparqlInsert(subjUri, property, "<" + object.getURI() + ">");
    }

    public static String createSparqlInsert(String subjUri, Property property,
            String value) {
        return buildSparqlInsert(subjUri, property, "\"" + value + "\"");
    }

    private static String buildSparqlInsert(String subjUri, Property property,
            String object) {
        StringBuilder query = new StringBuilder();
        query.append("INSERT {\n");

        if (subjUri == null) {
            query.append(" <>");
        } else {
            query.append(" <").append(subjUri).append('>');
        }

        query.append(" <").append(property.getURI()).append("> ")
                .append(object).append(" .\n").append("}\nWHERE {}");

        return query.toString();
    }
}
