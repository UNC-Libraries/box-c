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
package edu.unc.lib.dl.test;

import static edu.unc.lib.dl.util.RDFModelUtil.createModel;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.dl.rdf.Ldp;
import edu.unc.lib.dl.rdf.PcdmModels;

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

    public void indexAll(Model model) throws Exception {
        queryModel.removeAll();

        indexTree(model);
    }

    private void indexTree(Model model) throws Exception {
        queryModel.add(model);

        indexRelated(model, Ldp.contains);
        indexRelated(model, PcdmModels.hasMember);
    }

    private void indexRelated(Model model, Property relationProp) throws Exception {
        NodeIterator containedIt = model.listObjectsOfProperty(relationProp);
        while (containedIt.hasNext()) {
            RDFNode contained = containedIt.next();
            URI rescUri = URI.create(contained.asResource().getURI());
            try (FcrepoResponse resp = fcrepoClient.get(rescUri).perform()) {
                Model containedModel;
                // If the object retrieved is a binary, request its fcr:metadata instead
                if (resp.getLinkHeaders("describedby").size() > 0) {
                    URI binUri = URI.create(contained.asResource().getURI() + "/fcr:metadata");
                    try (FcrepoResponse binResp = fcrepoClient.get(binUri).perform()) {
                        containedModel = createModel(binResp.getBody());
                    }
                } else {
                    containedModel = createModel(resp.getBody());
                }

                indexTree(containedModel);
            }
        }
    }
}
