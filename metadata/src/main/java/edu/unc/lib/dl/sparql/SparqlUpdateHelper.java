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
package edu.unc.lib.dl.sparql;

import java.util.List;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

/**
 * Helper methods for creating basic sparql update queries
 *
 * @author bbpennel
 *
 */
public class SparqlUpdateHelper {

    private SparqlUpdateHelper() {
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

    /**
     * Create a SPARQL update query to add one property to a resource
     *
     * @param subjUri
     * @param property
     * @param object
     * @return
     */
    public static String createSparqlInsert(String subjUri, Property property,
            Object object) {

        String objectString = getObjectAsString(object);

        StringBuilder query = new StringBuilder();
        addInsert(query, subjUri, property, objectString);
        query.append("\nWHERE {}");

        return query.toString();
    }

    /**
     * Constructs a sparql query which replaces all instances of a property on a resource with a new value
     *
     * @param subjUri
     * @param property
     * @param object
     * @return
     */
    public static String createSparqlReplace(String subjUri, Property property, Object object) {
        return createSparqlReplace(subjUri, property, object, null);
    }

    /**
     * Constructs a sparql query which replaces all instances of a property on a resource with a new value
     *
     * @param subjUri
     * @param property
     * @param object
     * @param previousValues
     * @return
     */
    public static String createSparqlReplace(String subjUri, Property property,
            Object object, List<Object> previousValues) {

        String objectString = getObjectAsString(object);

        String subjString = getSubjectString(subjUri);

        String propertyString = " <" + property.getURI() + "> ";

        StringBuilder query = new StringBuilder();
        query.append("DELETE { ");
        if (previousValues != null && previousValues.size() > 0) {
            for (Object obj : previousValues) {
                query.append(subjString).append(propertyString).append(getObjectAsString(obj)).append(" . \n");
            }
        }
        query.append("}\n");

        addInsert(query, subjUri, property, objectString);

        query.append("\nWHERE { }");

        return query.toString();
    }

    /**
     * Constructs a sparql update query to delete all instances of a property from a resource.
     *
     * @param subjUri
     * @param property
     * @return
     */
    public static String createSparqlDelete(String subjUri, Property property, Object object) {
        String subjString = getSubjectString(subjUri);

        StringBuilder query = new StringBuilder();
        query.append("DELETE { ");
        query.append(subjString).append(" <").append(property.getURI()).append('>');

        // If no object was specified for deletion, then select for all values of property
        if (object == null) {
            query.append(" ?obj . }\n");
        } else {
            query.append(' ').append(getObjectAsString(object)).append(" . }\n");
        }

        query.append("\nWHERE {");
        if (object == null) {
            query.append(subjString).append(" <").append(property.getURI()).append("> ?obj . }");
        } else {
            query.append('}');
        }

        return query.toString();
    }

    /**
     * Returns a string representation of a resource or a typed literal based on
     * the type of object provided. If the object is not a known XSD datatype or
     * resource, null is returned.
     *
     * @param object
     * @return
     */
    public static String getObjectAsString(Object object) {
        if (object instanceof Resource) {
            return '<' + ((Resource) object).getURI() + '>';
        } else if (object instanceof String) {
            return '"' + object.toString() + '"';
        } else if (object instanceof Literal) {
            RDFDatatype type = ((Literal) object).getDatatype();
            return '"' + object.toString() + "\"^^<" + type.getURI() + ">";
        } else {
            RDFDatatype type = TypeMapper.getInstance().getTypeByClass(object.getClass());
            if (type == null) {
                throw new IllegalArgumentException("Cannot convert object of type " + object.getClass()
                        + ", only supported XSD datatypes and Resources are supported.");
            }
            String typeUri = type.getURI();

            return '"' + object.toString() + "\"^^<" + typeUri + ">";
        }
    }

    private static void addInsert(StringBuilder query, String subjUri, Property property,
            String object) {
        query.append("INSERT {");

        if (subjUri == null) {
            query.append(" <>");
        } else {
            query.append(" <").append(subjUri).append('>');
        }

        query.append(" <").append(property.getURI()).append("> ")
                .append(object).append(" . ").append("}");
    }

    private static String getSubjectString(String subjUri) {
        if (subjUri == null) {
            return "<>";
        } else {
            return "<" + subjUri + ">";
        }
    }
}
