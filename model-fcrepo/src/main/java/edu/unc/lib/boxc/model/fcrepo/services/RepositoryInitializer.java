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
package edu.unc.lib.boxc.model.fcrepo.services;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.slf4j.Logger;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.FcrepoPaths;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;

/**
 * Initializes the structure of the repository
 *
 * @author bbpennel
 *
 */
public class RepositoryInitializer {
    private static final Logger log = getLogger(RepositoryInitializer.class);

    private RepositoryObjectFactory objFactory;

    /**
     * Initializes objects required for the base functionality of the repository
     */
    public void initializeRepository() {
        // Initialize the content base container
        URI contentUri = createContainer(RepositoryPathConstants.CONTENT_BASE,
                "Content Tree");

        // Add the content tree root object
        createContentRoot(contentUri);

        // Initialize the path where deposit records are stored
        createContainer(RepositoryPathConstants.DEPOSIT_RECORD_BASE,
                "Deposit Records");
    }

    private URI createContainer(String id, String title) {
        String containerString = URIUtil.join(FcrepoPaths.getBaseUri(), id);
        URI containerUri = URI.create(containerString);

        // Abort initialization of already present container
        if (objFactory.objectExists(containerUri)) {
            return containerUri;
        }

        log.warn("Initializing object '{}' with id {}", title, id);

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource(containerString);
        resc.addProperty(DC.title, title);

        objFactory.createOrTransformObject(containerUri, model);

        return containerUri;
    }

    private URI createContentRoot(URI contentUri) {
        String contentRootString = URIUtil.join(
                contentUri, RepositoryPathConstants.CONTENT_ROOT_ID);
        URI contentRootUri = URI.create(contentRootString);

        // Don't initialize the object if it is already present.
        if (objFactory.objectExists(contentRootUri)) {
            return contentRootUri;
        }

        log.warn("Initializing content root object {}", contentRootUri);

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource(contentRootString);
        resc.addProperty(DC.title, "Content Collections Root");


        objFactory.createContentRootObject(contentRootUri, model);

        return contentRootUri;
    }

    public void setObjFactory(RepositoryObjectFactory objFactory) {
        this.objFactory = objFactory;
    }
}
