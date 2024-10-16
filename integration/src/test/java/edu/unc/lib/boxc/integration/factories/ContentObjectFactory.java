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
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;

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

    protected void prepareObject(ContentObject object, Map<String, String> options) throws Exception {
        options = validateOptions(options);
        var modsDocument = modsFactory.createDocument(options);
        if (modsDocument != null) {
            // put mods in fedora
            var request = new UpdateDescriptionService.UpdateDescriptionRequest(agent, object.getPid(), modsDocument);
            updateDescriptionService.updateDescription(request);
        }
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

    protected void addThumbnail(ContentObject object) throws IOException {
        var derivativePath = derivativeService.getDerivativePath(object.getPid(), DatastreamType.JP2_ACCESS_COPY);
        FileUtils.write(derivativePath.toFile(), "image", "UTF-8");
    }

    protected Model getAccessModel(Map<String, String> options) {
        Model accessGroup = null;
        var group = new AclModelBuilder(options.getOrDefault("title", ""));
        if (options.containsKey("readGroup")) {
            accessGroup = group.addCanViewOriginals(options.get("readGroup")).model;
        }
        if (options.containsKey("noneGroup")) {
            accessGroup = group.addNoneRole(options.get("noneGroup")).model;
        }
        if (options.containsKey("adminGroup")) {
            var adminOption = options.get("adminGroup");
            accessGroup = group.addCanManage(adminOption).model;
        }
        if (options.containsKey("metadataGroup")) {
            accessGroup = group.addCanViewMetadata(options.get("metadataGroup")).model;
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
