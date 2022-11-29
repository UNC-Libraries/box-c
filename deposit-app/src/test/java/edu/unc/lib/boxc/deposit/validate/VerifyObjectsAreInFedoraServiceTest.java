package edu.unc.lib.boxc.deposit.validate;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.HeadBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.deposit.validate.VerifyObjectsAreInFedoraService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 *
 * @author harring
 *
 */
public class VerifyObjectsAreInFedoraServiceTest {

    private VerifyObjectsAreInFedoraService verificationService;
    private PID workPid;
    private PID filePid;
    private Collection<String> pids;
    @Mock
    private FcrepoClient client;
    @Mock
    private Resource resc;
    @Mock
    private NodeIterator iterator;
    @Mock
    private FcrepoResponse response;
    @Mock
    private HeadBuilder builder;


    @Before
    public void init() throws Exception {
        initMocks(this);
        verificationService = new VerifyObjectsAreInFedoraService();
        setField(verificationService, "fcrepoClient", client);

        workPid = makePid();
        filePid = makePid();
        pids = new HashSet<>();
    }

    @Test
    public void allObjectsInFedoraTest() throws FcrepoOperationFailedException {
        when(client.head(any(URI.class))).thenReturn(builder);
        when(builder.perform()).thenReturn(response);

        List<PID> objPids = verificationService.listObjectsNotInFedora(pids);

        assertEquals(0, objPids.size());
    }

    @Test
    public void listObjectsNotInFedoraTest() throws FcrepoOperationFailedException {
        when(client.head(any(URI.class))).thenReturn(builder);
        when(builder.perform())
                .thenThrow(new FcrepoOperationFailedException(workPid.getRepositoryUri(), 404, "Not Found"));

        Collection<String> pids = new HashSet<>();
        pids.add(workPid.toString());
        pids.add(filePid.toString());
        List<PID> objPids = verificationService.listObjectsNotInFedora(pids);

        assertEquals(2, objPids.size());
    }

    @Test
    public void listObjectPIDsTest() {
        PID depositPid = makePid();
        PID obj1 = makePid();
        PID obj2 = makePid();
        List<PID> objPids = new ArrayList<>();
        objPids.add(obj1);
        objPids.add(obj2);

        assertEquals(
                "The following objects from deposit " + depositPid.getQualifiedId() + " did not make it to Fedora:\n"
                        + obj1.toString() + "\n" + obj2.toString() + "\n",
                verificationService.listObjectPIDs(depositPid.getQualifiedId(), objPids));
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

}
