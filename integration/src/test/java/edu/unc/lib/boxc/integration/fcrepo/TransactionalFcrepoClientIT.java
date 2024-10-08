package edu.unc.lib.boxc.integration.fcrepo;

import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.createModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.client.FedoraTypes.LDP_NON_RDF_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.commons.io.IOUtils;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.fcrepo.exceptions.TransactionCancelledException;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.fcrepo.utils.TransactionalFcrepoClient;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.RDFModelUtil;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;

/**
 * @author bbpennel
 */
public class TransactionalFcrepoClientIT extends AbstractFedoraIT {
    @Autowired
    private TransactionalFcrepoClient fcrepoClient;
    @Autowired
    private String baseAddress;
    private URI baseUri;
    @Autowired
    private TransactionManager txManager;

    @BeforeEach
    public void setup() {
        baseUri = URI.create(baseAddress);
        TestHelper.setContentBase(baseAddress);
    }

    @Test
    public void testRequestWithTx() throws Exception {
        txManager.setClient(fcrepoClient);

        // Create object in transaction, should return transaction location
        FedoraTransaction tx = txManager.startTransaction();
        URI objUri;
        try (FcrepoResponse response = fcrepoClient.post(baseUri).perform()) {
            objUri = response.getLocation();
        }

        assertTrue(objUri.toString().startsWith(tx.getTxUri().toString()), "Location must be within transaction");

        // Verify that the response triples have non-tx uris
        try (FcrepoResponse response = fcrepoClient.get(objUri).perform()) {
            // Remove tx from obj uri
            String nonTxObjUri = objUri.toString().replaceFirst("/tx:[^/]+", "");

            Model model = createModel(response.getBody());

            Resource resc = model.getResource(nonTxObjUri);
            assertTrue(resc.hasProperty(RDF.type), "Subject must be non-tx uri");
        } finally {
            tx.close();
        }
    }

    @Test
    public void testPutTriplesWithTx() throws Exception {
        txManager.setClient(fcrepoClient);

        PID pid = PIDs.get(UUID.randomUUID().toString());

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(pid.getRepositoryPath());
        resc.addLiteral(DC.title, "My Title");

        FedoraTransaction tx = txManager.startTransaction();
        createAndVerifyObjectWithBody(pid.getRepositoryUri(), model);
        try {
            tx.cancel();
        } catch (TransactionCancelledException e) {
            // Expected
        }
        // Verify that the created resource went away with transaction
        try (FcrepoResponse response = fcrepoClient.get(pid.getRepositoryUri()).perform()) {
        } catch (FcrepoOperationFailedException e) {
            assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
        }

        FedoraTransaction tx2 = txManager.startTransaction();
        createAndVerifyObjectWithBody(pid.getRepositoryUri(), model);
        tx2.close();

        try (FcrepoResponse response = fcrepoClient.get(pid.getRepositoryUri()).perform()) {
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        }
    }

    @Test
    public void testBinaryRequestWithTx() throws Exception {
        txManager.setClient(fcrepoClient);

        PID bodyPid = PIDs.get(UUID.randomUUID().toString());
        String requestBody = "<" + bodyPid.getRepositoryPath() + "> <" + RDF.type.getURI() + "> \"Test\"";
        InputStream requestStream = new ByteArrayInputStream(requestBody.getBytes());

        // Create object in transaction, should return transaction location
        FedoraTransaction tx = txManager.startTransaction();
        URI objUri;
        try (FcrepoResponse response = fcrepoClient.post(baseUri)
                .body(requestStream, "application/n-triples")
                .addInteractionModel(LDP_NON_RDF_SOURCE)
                .perform()) {
            objUri = response.getLocation();
        }

        assertTrue(objUri.toString().startsWith(tx.getTxUri().toString()), "Location must be within transaction");

        tx.close();

        String nonTxObjPath = objUri.toString().replaceFirst("/tx:[^/]+", "");
        URI nonTxObjUri = URI.create(nonTxObjPath);
        // Verify that the binary content was not rewritten with tx uris
        try (FcrepoResponse response = fcrepoClient.get(nonTxObjUri).perform()) {
            String respBody = IOUtils.toString(response.getBody());
            assertTrue(respBody.contains(bodyPid.getRepositoryPath()));
        }
    }

    private URI createAndVerifyObjectWithBody(URI path, Model model) throws Exception {
        InputStream rdfStream = RDFModelUtil.streamModel(model);

        URI objUri;
        try (FcrepoResponse response = fcrepoClient.put(path).body(rdfStream).perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            objUri = response.getLocation();
        }

        try (FcrepoResponse response = fcrepoClient.get(objUri).perform()) {
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            Model respModel = RDFModelUtil.createModel(response.getBody());
            respModel.contains(createResource(path.toString()), DC.title, "My Title");
        }

        return objUri;
    }
}
