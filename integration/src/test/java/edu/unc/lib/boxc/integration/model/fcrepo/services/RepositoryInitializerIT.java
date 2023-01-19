package edu.unc.lib.boxc.integration.model.fcrepo.services;

import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.TURTLE_MIMETYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.FcrepoPaths;
import edu.unc.lib.boxc.integration.fcrepo.AbstractFedoraIT;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;

/**
 *
 * @author bbpennel
 *
 */
public class RepositoryInitializerIT extends AbstractFedoraIT {

    private RepositoryInitializer repoInitializer;

    @BeforeEach
    public void init() {
        repoInitializer = new RepositoryInitializer();
        repoInitializer.setObjFactory(repoObjFactory);
    }

    /**
     * Ensure that expected objects were initialized
     *
     * @throws Exception
     */
    @Test
    public void fullInitializationTest() throws Exception {
        repoInitializer.initializeRepository();

        URI contentContainerUri = getContainerUri(RepositoryPathConstants.CONTENT_BASE);
        assertObjectExists(contentContainerUri);

        String contentRootString = URIUtil.join(
                contentContainerUri, RepositoryPathConstants.CONTENT_ROOT_ID);
        URI contentRootUri = URI.create(contentRootString);
        assertObjectExists(contentRootUri);

        try (FcrepoResponse response = client.get(contentRootUri)
                .accept(TURTLE_MIMETYPE)
                .perform()) {

            Model crModel = ModelFactory.createDefaultModel();
            crModel.read(response.getBody(), null, Lang.TURTLE.getName());

            Resource crResc = crModel.getResource(contentRootUri.toString());
            assertTrue(crResc.hasProperty(RDF.type, Cdr.ContentRoot));
        }

        URI depositContainerUri = getContainerUri(RepositoryPathConstants.DEPOSIT_RECORD_BASE);
        assertObjectExists(depositContainerUri);
    }

    /**
     * Show that additional initialization calls after the first do not cause
     * objects to be modified or recreated
     *
     * @throws Exception
     */
    @Test
    public void multipleInitializeTest() throws Exception {
        repoInitializer.initializeRepository();

        URI contentContainerUri = getContainerUri(RepositoryPathConstants.CONTENT_BASE);
        String contentContainerEtag = getEtag(contentContainerUri);

        String contentRootString = URIUtil.join(
                contentContainerUri, RepositoryPathConstants.CONTENT_ROOT_ID);
        URI contentRootUri = URI.create(contentRootString);
        String contentRootEtag = getEtag(contentRootUri);

        URI depositContainerUri = getContainerUri(RepositoryPathConstants.DEPOSIT_RECORD_BASE);
        String depositContainerEtag = getEtag(depositContainerUri);

        repoInitializer.initializeRepository();

        assertEquals(contentContainerEtag, getEtag(contentContainerUri),
                "Content Container object changed after second initialization");
        assertEquals(contentRootEtag, getEtag(contentRootUri),
                "Content Root object changed after second initialization");
        assertEquals(depositContainerEtag, getEtag(depositContainerUri),
                "Deposit Container object changed after second initialization");
    }

    private String getEtag(URI uri) throws Exception {
        try (FcrepoResponse response = client.head(uri).perform()) {
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());

            String etag = response.getHeaderValue("ETag");
            return etag.substring(1, etag.length() - 1);
        }
    }

    private URI getContainerUri(String id) {
        String containerString = URIUtil.join(FcrepoPaths.getBaseUri(), id);
        return URI.create(containerString);
    }
}
