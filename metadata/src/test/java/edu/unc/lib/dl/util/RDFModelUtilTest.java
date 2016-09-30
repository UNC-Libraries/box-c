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

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.vocabulary.RDF;

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
public class RDFModelUtilTest {

	@Test
	public void createSparqlInsertTest() {
		String rescUri = "http://example.com/resource";
		Property booleanProperty = createProperty("http://example.com/booleanProperty");

		// Define a set of properties to add to a resource
		Model insertModel = ModelFactory.createDefaultModel();
		Resource resc = insertModel.createResource(rescUri);
		resc.addProperty(RDF.type, Cdr.Collection);
		resc.addLiteral(booleanProperty, false);
		resc.addProperty(DcElements.title, "Title");

		// Define a hash uri resource off of the main resource
		String hashUri = rescUri + "#event";
		Resource hashResc = insertModel.createResource(hashUri);
		hashResc.addProperty(Premis.hasEventType, Premis.Capture);
		hashResc.addProperty(Premis.hasEventDetail, "Success");

		// Build the update query
		String query = RDFModelUtil.createSparqlInsert(insertModel);

		// Make a model that the query will be applied to
		Model destModel = ModelFactory.createDefaultModel();
		Resource destResc = destModel.createResource(rescUri);
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
		String rescUri = "http://example.com/resource";
		
		String query = RDFModelUtil.createSparqlInsert(rescUri, RDF.type, PcdmModels.Object);
		
		// Make a model that the query will be applied to
		Model destModel = ModelFactory.createDefaultModel();
		Resource destResc = destModel.createResource(rescUri);
		destResc.addProperty(RDF.type, Fcrepo4Repository.Container);
		
		// Execute the query on the destination model to see that it works
		UpdateAction.parseExecute(query, destModel);
		
		assertTrue(destResc.hasProperty(RDF.type, Fcrepo4Repository.Container));
		assertTrue(destResc.hasProperty(RDF.type, PcdmModels.Object));
	}
}
