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
package edu.unc.lib.dcr.migration.content;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.deposit.DepositModelManager;
import edu.unc.lib.dl.util.ResourceType;

/**
 * Service which transforms a tree of content objects starting from a single root.
 *
 * @author bbpennel
 */
public class ContentTransformationService {

    private List<PID> startingPids;
    private PID depositPid;
    private ContentObjectTransformerManager transformerManager;
    private DepositModelManager modelManager;
    private RepositoryObjectLoader repoObjLoader;

    public ContentTransformationService(PID depositPid, String startingId) {
        startingPids = Arrays.stream(startingId.split(",")).map(PIDs::get).collect(Collectors.toList());
        this.depositPid = depositPid;
    }

    /**
     * Perform the transformation of the tree of content objects
     *
     * @return result code
     */
    public int perform() {
        // Populate the bag for the deposit itself
        Model depositObjModel = createDefaultModel();
        depositObjModel.createBag(depositPid.getRepositoryPath());
        modelManager.addTriples(depositPid, depositObjModel);

        // Kick off transformation of the tree from the starting object
        ContentTransformationOptions options = transformerManager.getOptions();

        Resource parentType = null;
        String depositInto = options.getDepositInto();
        if (depositInto != null) {
            PID depositInfoPid = PIDs.get(depositInto);
            RepositoryObject repoObj = repoObjLoader.getRepositoryObject(depositInfoPid);
            ResourceType rescType = repoObj.getResourceType();
            parentType = rescType.getResource();
        }

        for (PID startingPid : startingPids) {
            // Determine transformed id of starting object
            PID newPid = transformerManager.getTransformedPid(startingPid);
            transformerManager.createTransformer(startingPid, newPid, depositPid, parentType)
                    .fork();
        }

        // Wait for all transformers to finish
        return transformerManager.awaitTransformers();
    }

    public void setTransformerManager(ContentObjectTransformerManager transformerManager) {
        this.transformerManager = transformerManager;
    }

    public void setModelManager(DepositModelManager modelManager) {
        this.modelManager = modelManager;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }
}
