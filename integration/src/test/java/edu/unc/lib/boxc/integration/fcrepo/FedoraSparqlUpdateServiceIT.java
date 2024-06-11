package edu.unc.lib.boxc.integration.fcrepo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.unc.lib.boxc.fcrepo.utils.FedoraSparqlUpdateService;
import edu.unc.lib.boxc.model.api.rdf.RDFModelUtil;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;

/**
 *
 * @author bbpennel
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"/spring-test/test-fedora-container.xml", "/spring-test/cdr-client-container.xml"})
public class FedoraSparqlUpdateServiceIT {

    @Autowired
    protected String baseAddress;
    private URI baseUri;

    @Autowired
    protected FcrepoClient client;

    private FedoraSparqlUpdateService updateService;

    @BeforeEach
    public void init_() {
        TestHelper.setContentBase(baseAddress);
        baseUri = URI.create(baseAddress);

        updateService = new FedoraSparqlUpdateService();
        updateService.setFcrepoClient(client);
    }

    @Test
    public void updateTriplesTest() throws Exception {
        URI containerUri;
        try (FcrepoResponse response = client.post(baseUri).perform()) {
            containerUri = response.getLocation();
        }

        String updateString = "INSERT { <> <" + DC.title.getURI() + "> \"title\" . } WHERE {}";
        updateService.executeUpdate(containerUri.toString(), updateString);

        try (FcrepoResponse response = client.get(containerUri).perform()) {
            Model model = RDFModelUtil.createModel(response.getBody());
            Resource resc = model.getResource(containerUri.toString());
            assertTrue(resc.hasProperty(DC.title, "title"));
        }
    }

}
