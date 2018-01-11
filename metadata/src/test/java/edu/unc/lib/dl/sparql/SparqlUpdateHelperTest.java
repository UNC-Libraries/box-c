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

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.junit.Test;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.rdf.Premis;

/**
 *
 * @author bbpennel
 *
 */
public class SparqlUpdateHelperTest {
    private static String RESC_URI = "http://example.com/resource";

    @Test
    public void createSparqlInsertTest() {
        Property booleanProperty = createProperty("http://example.com/booleanProperty");

        // Define a set of properties to add to a resource
        Model insertModel = ModelFactory.createDefaultModel();
        Resource resc = insertModel.createResource(RESC_URI);
        resc.addProperty(RDF.type, Cdr.Collection);
        resc.addLiteral(booleanProperty, false);
        resc.addProperty(DcElements.title, "Title");

        // Define a hash uri resource off of the main resource
        String hashUri = RESC_URI + "#event";
        Resource hashResc = insertModel.createResource(hashUri);
        hashResc.addProperty(Premis.hasEventType, Premis.Capture);

        // Build the update query
        String query = SparqlUpdateHelper.createSparqlInsert(insertModel);

        // Make a model that the query will be applied to
        Model destModel = ModelFactory.createDefaultModel();
        Resource destResc = destModel.createResource(RESC_URI);
        destResc.addProperty(RDF.type, Fcrepo4Repository.Container);

        // Execute the query on the destination model to see that it works
        UpdateAction.parseExecute(query, destModel);

        // Verify that the new properties were added to the destination objects
        assertTrue(destResc.hasProperty(RDF.type, Cdr.Collection));
        assertTrue(destResc.hasProperty(DcElements.title, "Title"));
        destResc.getProperty(booleanProperty).getBoolean();

        // Make sure the hash uri got added too
        Resource destHash = destModel.getResource(hashUri);
        assertTrue(destHash.hasProperty(Premis.hasEventType, Premis.Capture));
    }

    @Test
    public void createSparqlSingleResourceInsertTest() {
        String query = SparqlUpdateHelper.createSparqlInsert(RESC_URI, RDF.type, PcdmModels.Object);

        // Make a model that the query will be applied to
        Model destModel = ModelFactory.createDefaultModel();
        Resource destResc = destModel.createResource(RESC_URI);
        destResc.addProperty(RDF.type, Fcrepo4Repository.Container);

        // Execute the query on the destination model to see that it works
        UpdateAction.parseExecute(query, destModel);

        assertTrue(destResc.hasProperty(RDF.type, Fcrepo4Repository.Container));
        assertTrue(destResc.hasProperty(RDF.type, PcdmModels.Object));
    }

    @Test
    public void getObjectAsStringStringTest() {
        String objString = SparqlUpdateHelper.getObjectAsString("test");

        assertEquals("\"test\"", objString);
    }

    @Test
    public void getObjectAsStringResourceTest() {
        Resource resc = ModelFactory.createDefaultModel().getResource(RESC_URI);
        String objString = SparqlUpdateHelper.getObjectAsString(resc);

        assertEquals("<" + RESC_URI + ">", objString);
    }

    @Test
    public void getObjectAsStringBooleanTest() {
        String objString = SparqlUpdateHelper.getObjectAsString(false);

        assertEquals("\"false\"^^<http://www.w3.org/2001/XMLSchema#boolean>", objString);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getObjectAsStringNonapplicableTest() {
        SparqlUpdateHelper.getObjectAsString(new Object());
    }

    @Test
    public void createSparqlReplaceTest() {
        String expectedTitle = "Title";

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(RESC_URI);
        resc.addProperty(DC.title, "1");
        resc.addProperty(DC.title, "2");

        List<Object> previousTitles = new ArrayList<>();
        previousTitles.add("1");
        previousTitles.add("2");
        String sparql = SparqlUpdateHelper.createSparqlReplace(RESC_URI, DC.title, expectedTitle, previousTitles);
        UpdateRequest request = UpdateFactory.create(sparql);
        UpdateAction.execute(request, model);

        StmtIterator stmtIt = resc.listProperties(DC.title);
        assertEquals(expectedTitle, stmtIt.next().getString());
        assertFalse(stmtIt.hasNext());
    }

    @Test
    public void createSparqlReplaceAddOnlyTest() {
        String expectedTitle = "Title";

        String sparql = SparqlUpdateHelper.createSparqlReplace(RESC_URI, DC.title, expectedTitle);

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(RESC_URI);
        resc.addProperty(DC.contributor, "contrib");

        UpdateRequest request = UpdateFactory.create(sparql);
        UpdateAction.execute(request, model);

        StmtIterator stmtIt = resc.listProperties(DC.title);
        assertEquals(expectedTitle, stmtIt.next().getString());
        assertFalse(stmtIt.hasNext());
    }

    @Test
    public void createSparqlDeleteWithObject() {
        String deleteTitle = "Delete me";
        String retainTitle = "Keep me";

        String sparql = SparqlUpdateHelper.createSparqlDelete(RESC_URI, DC.title, deleteTitle);

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(RESC_URI);
        resc.addProperty(DC.title, retainTitle);
        resc.addProperty(DC.title, deleteTitle);

        // Execute the query
        UpdateRequest request = UpdateFactory.create(sparql);
        UpdateAction.execute(request, model);

        StmtIterator stmtIt = resc.listProperties(DC.title);
        assertEquals(retainTitle, stmtIt.next().getString());
        assertFalse(stmtIt.hasNext());
    }

    @Test
    public void createSparqlDeleteNullObject() {
        String sparql = SparqlUpdateHelper.createSparqlDelete(RESC_URI, DC.title, null);

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(RESC_URI);
        resc.addProperty(DC.title, "Title1");
        resc.addProperty(DC.title, "Title2");

        // Execute the query
        UpdateRequest request = UpdateFactory.create(sparql);
        UpdateAction.execute(request, model);

        assertFalse(resc.hasProperty(DC.title));
    }
}
