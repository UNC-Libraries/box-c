package edu.unc.lib.boxc.indexing.solr.test;

import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.createModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.boxc.indexing.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;

/**
 * Test utility for indexing repository objects in solr
 *
 * @author bbpennel
 */
public class RepositoryObjectSolrIndexer {

    @Autowired(required = false)
    private SolrUpdateDriver solrUpdateDriver;

    @Autowired(required = false)
    private DocumentIndexingPackageFactory factory;

    @Autowired(required = false)
    private DocumentIndexingPipeline pipeline;

    @Autowired(required = false)
    private FcrepoClient fcrepoClient;

    private static final Set<RDFNode> INDEXABLE_TYPES = new HashSet<>(Arrays.asList(
            Cdr.ContentRoot, Cdr.AdminUnit, Cdr.Collection, Cdr.Folder, Cdr.Work, Cdr.FileObject));

    /**
     * Index the specified resource and all objects it ldp:contains
     * @param pid
     */
    public void indexTree(PID pid) {
        indexRecursive(pid);
        solrUpdateDriver.commit();
    }

    private void indexRecursive(PID pid) {
        Model rescModel;
        try (FcrepoResponse resp = fcrepoClient.get(pid.getRepositoryUri()).perform()) {
            rescModel = createModel(resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        RDFNode resourceType = getResourceType(rescModel);

        if (resourceType == null) {
            return;
        }

        indexObject(pid);

        if (Cdr.FileObject.equals(resourceType)) {
            return;
        }

        NodeIterator containedIt = rescModel.listObjectsOfProperty(Ldp.contains);
        while (containedIt.hasNext()) {
            RDFNode contained = containedIt.next();
            indexTree(PIDs.get(contained.asNode().getURI()));
        }
    }

    private RDFNode getResourceType(Model model) {
        return INDEXABLE_TYPES.stream()
            .filter(type -> model.containsLiteral(null, RDF.type, type))
            .findFirst()
            .orElse(null);
    }

    /**
     * Index a list of resources
     * @param pids
     */
    public void index(PID... pids) {
        for (PID pid: pids) {
            indexObject(pid);
        }
        solrUpdateDriver.commit();
    }

    /**
     * Index a single resource
     * @param pid
     */
    private void indexObject(PID pid) {
        SolrUpdateRequest updateRequest = new SolrUpdateRequest(
                pid.getRepositoryPath(), IndexingActionType.ADD);

        DocumentIndexingPackage dip = factory.createDip(pid);
        updateRequest.setDocumentIndexingPackage(dip);

        pipeline.process(dip);
        solrUpdateDriver.addDocument(dip.getDocument());
    }

    /**
     * Clear the solr index
     */
    public void clearIndex() {
        solrUpdateDriver.deleteByQuery("*:*");
        solrUpdateDriver.commit();
    }

    public void setSolrUpdateDriver(SolrUpdateDriver solrUpdateDriver) {
        this.solrUpdateDriver = solrUpdateDriver;
    }

    public void setDocumentIndexingPackageFactory(DocumentIndexingPackageFactory factory) {
        this.factory = factory;
    }

    public void setDocumentIndexingPipeline(DocumentIndexingPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }
}
