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
package edu.unc.lib.boxc.integration.factories;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.indexing.solr.test.RepositoryObjectSolrIndexer;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.util.Map;

/**
 * @author snluong
 */
public class ContentObjectFactory {
    protected RepositoryObjectFactory repositoryObjectFactory;
    protected RepositoryObjectTreeIndexer repositoryObjectTreeIndexer;
    protected RepositoryObjectSolrIndexer repositoryObjectSolrIndexer;
    protected RepositoryObjectLoader repositoryObjectLoader;
    protected ModsFactory modsFactory;
    protected UpdateDescriptionService updateDescriptionService;
    protected DerivativeService derivativeService;
    protected final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("adminGroup"));

    public void prepareObject(ContentObject object, Map<String, String> options) throws Exception {
        options = validateOptions(options);
        var modsDocument = modsFactory.createDocument(options);
        var modsString = new XMLOutputter(Format.getPrettyFormat()).outputString(modsDocument);
        var inputStream = IOUtils.toInputStream(modsString, "utf-8");

        // put mods in fedora
        updateDescriptionService.updateDescription(agent, object.getPid(), inputStream);
        // index folder in triple store
        indexTripleStore(object);
        // index into solr
        indexSolr(object);
    }

    public void indexTripleStore(ContentObject object) throws Exception {
        repositoryObjectTreeIndexer.indexAll(object.getUri().toString());
    }

    public void indexSolr(ContentObject object) {
        repositoryObjectSolrIndexer.index(object.getPid());
    }

    public Map<String, String> validateOptions(Map<String, String> options) {
        if (options.containsKey("title") && StringUtils.isEmpty(options.get("title"))) {
            options.put("title", "Object" + System.nanoTime());
        }
        return options;
    }

    public void addThumbnail(ContentObject object) throws IOException {
        var derivativePath = derivativeService.getDerivativePath(object.getPid(), DatastreamType.THUMBNAIL_LARGE);
        FileUtils.write(derivativePath.toFile(), "image", "UTF-8");
    }

    public Model getAccessGroup(Map<String, String> options) {
        Model accessGroup;
        if (options.containsKey("readGroup")) {
            accessGroup = new AclModelBuilder(options.get("title"))
                    .addCanViewOriginals(options.get("readGroup"))
                    .model;
        } else {
            accessGroup = null;
        }
        return accessGroup;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setRepositoryObjectTreeIndexer(RepositoryObjectTreeIndexer repositoryObjectTreeIndexer) {
        this.repositoryObjectTreeIndexer = repositoryObjectTreeIndexer;
    }

    public void setRepositoryObjectSolrIndexer(RepositoryObjectSolrIndexer repositoryObjectSolrIndexer) {
        this.repositoryObjectSolrIndexer = repositoryObjectSolrIndexer;
    }

    public void setModsFactory(ModsFactory modsFactory) {
        this.modsFactory = modsFactory;
    }

    public void setUpdateDescriptionService(UpdateDescriptionService updateDescriptionService) {
        this.updateDescriptionService = updateDescriptionService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }
}
