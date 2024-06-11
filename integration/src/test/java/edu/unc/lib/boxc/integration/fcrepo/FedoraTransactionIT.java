package edu.unc.lib.boxc.integration.fcrepo;

import edu.unc.lib.boxc.fcrepo.exceptions.TransactionCancelledException;
import edu.unc.lib.boxc.fcrepo.utils.FcrepoClientFactory;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author harring
 *
 */
public class FedoraTransactionIT extends AbstractFedoraIT {
    private Model model;

    @BeforeEach
    public void init() {
        model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource("");
        resc.addProperty(DcElements.title, "Folder Title");
    }

    @Test
    public void createTxTest() throws Exception {
        FedoraTransaction tx = txManager.startTransaction();

        FolderObject obj = null;
        try {
            obj = repoObjFactory.createFolderObject(model);

            assertTrue(FedoraTransaction.hasTxId());
            assertTrue(obj.getTypes().contains(Cdr.Folder.getURI()));
            assertTrue(obj.getTypes().contains(PcdmModels.Object.getURI()));
            assertEquals("Folder Title", obj.getResource()
                    .getProperty(DcElements.title).getString());
        } finally {
            tx.close();
        }

        assertFalse(FedoraTransaction.hasTxId());
        assertNull(tx.getTxUri());
        verifyNonTxStatusCode(obj.getPid(), 200);
    }

    @Test
    public void createRollbackTxTest() {
        Assertions.assertThrows(TransactionCancelledException.class, () -> {
            FedoraTransaction tx = txManager.startTransaction();
            FolderObject obj = repoObjFactory.createFolderObject(model);
            tx.cancel();
            assertNull(repoObjLoader.getFolderObject(obj.getPid()));
            assertNull(obj.getUri());
            assertFalse(FedoraTransaction.hasTxId());
            assertFalse(FedoraTransaction.isStillAlive());
        });
    }

    @Test
    public void nestedTxTest() throws Exception {
        FedoraTransaction parentTx = txManager.startTransaction();

        FedoraTransaction subTx = txManager.startTransaction();
        WorkObject workObj = repoObjFactory.createWorkObject(null);
        subTx.close();

        assertNull(subTx.getTxUri());
        verifyNonTxStatusCode(workObj.getPid(), 404);
        assertNotNull((parentTx.getTxUri()));
        assertTrue(FedoraTransaction.isStillAlive());

        parentTx.close();
        assertNull(parentTx.getTxUri());
        assertFalse(FedoraTransaction.isStillAlive());
        verifyNonTxStatusCode(workObj.getPid(), 200);
    }

    @Test
    public void cannotAccessObjectOutsideTxTest() throws Exception {
        FedoraTransaction tx = txManager.startTransaction();
        FolderObject folder = repoObjFactory.createFolderObject(null);

        verifyNonTxStatusCode(folder.getPid(), 404);
        tx.close();
    }

    private void verifyNonTxStatusCode(PID pid, int statusCode) {
        FcrepoClient nonTxClient = FcrepoClient.client()
                .credentials("fedoraAdmin", "fedoraAdmin")
                .authScope("localhost")
                .build();
        try (FcrepoResponse response = nonTxClient.get(pid.getRepositoryUri()).perform()) {
            assertEquals(statusCode, response.getStatusCode());
        } catch (FcrepoOperationFailedException | IOException e) {
            fail();
        }
    }

}
