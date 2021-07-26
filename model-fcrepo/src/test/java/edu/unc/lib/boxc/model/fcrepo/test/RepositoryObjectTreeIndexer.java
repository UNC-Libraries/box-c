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
package edu.unc.lib.boxc.model.fcrepo.test;

import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.createModel;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.boxc.model.api.rdf.Ldp;

/**
 * Test utility which indexes a tree of repository objects into a single model.
 *
 * @author bbpennel
 *
 */
public class RepositoryObjectTreeIndexer {
    private Model queryModel;
    private FcrepoClient fcrepoClient;

    public RepositoryObjectTreeIndexer(Model queryModel, FcrepoClient fcrepoClient) {
        this.queryModel = queryModel;
        this.fcrepoClient = fcrepoClient;
    }

    /**
     * Clears the index and then repopulates it with the resource specified by
     * baseUri and all resources which it contains.
     *
     * @param baseUri uri of the resource to index from.
     * @throws Exception
     */
    public void indexAll(String baseUri) throws Exception {
        try (FcrepoResponse resp = fcrepoClient.get(URI.create(baseUri)).perform()) {
            Model rescModel = createModel(resp.getBody());
            indexAll(rescModel);
        }
    }

    /**
     * Clears the index and then repopulates it with the triples contained by
     * the provided model, and recursively any resources referenced by contains
     * statements.
     *
     * @param model Model to begin indexing from.
     * @throws Exception
     */
    public void indexAll(Model model) throws Exception {
        queryModel.removeAll();

        indexTree(model);
    }

    /**
     * Indexes the provided model and recursively any resources referenced by
     * contains statements.
     *
     * @param model Model to index
     * @throws Exception
     */
    public void indexTree(Model model) throws Exception {
        queryModel.add(model);

        indexRelated(model, Ldp.contains);
    }

    private void indexRelated(Model model, Property relationProp) throws Exception {
        NodeIterator containedIt = model.listObjectsOfProperty(relationProp);
        while (containedIt.hasNext()) {
            RDFNode contained = containedIt.next();
            URI rescUri = URI.create(contained.asResource().getURI());
            try (FcrepoResponse resp = fcrepoClient.head(rescUri).perform()) {
                Model containedModel;
                // If the object retrieved is a binary, request its fcr:metadata instead
                if (resp.getLinkHeaders("describedby").size() > 0) {
                    URI binUri = URI.create(contained.asResource().getURI() + "/fcr:metadata");
                    try (FcrepoResponse binResp = fcrepoClient.get(binUri).perform()) {
                        containedModel = createModel(binResp.getBody());
                    }
                } else {
                    try (FcrepoResponse rdfResp = fcrepoClient.get(rescUri).perform()) {
                        containedModel = createModel(rdfResp.getBody());
                    }
                }

                indexTree(containedModel);
            }
        }
    }
}
