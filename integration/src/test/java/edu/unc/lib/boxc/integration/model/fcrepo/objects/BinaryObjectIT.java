package edu.unc.lib.boxc.integration.model.fcrepo.objects;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author bbpennel
 *
 */
public class BinaryObjectIT extends AbstractFedoraIT {

    private static final String BODY_STRING = "Test text";
    private static final String FILENAME = "test.txt";
    private static final String MIMETYPE = "text/plain";
    private static final String CHECKSUM = "82022e1782b92dce5461ee636a6c5bea8509ffee";

    private URI contentUri;
    private PID filePid;

    @Test
    public void retrieveExternalBinary() throws Exception {
        PID parentPid = pidMinter.mintContentPid();

        PID filePid = PIDs.get(parentPid.getId() + "/my_bin");
        BinaryObject obj = repoObjFactory.createOrUpdateBinary(filePid, makeContentUri(filePid, BODY_STRING),
                FILENAME, MIMETYPE, CHECKSUM, null, null);

        // Verify that the body of the binary is retrieved
        String respString = IOUtils.toString(obj.getBinaryStream());
        assertEquals(BODY_STRING, respString, "Binary content did not match submitted value");

        // Check that metadata is retrieved
        assertEquals(FILENAME, obj.getFilename());
        assertEquals(MIMETYPE, obj.getMimetype());
        assertEquals(BODY_STRING.length(), obj.getFilesize().longValue());
        assertEquals("urn:sha1:" + CHECKSUM, obj.getSha1Checksum());

        assertTrue(obj.getResource().hasProperty(RDF.type, Fcrepo4Repository.Binary));
    }

    @Test
    public void retrieveInternalBinary() throws Exception {
        PID parentPid = pidMinter.mintContentPid();
        try (FcrepoResponse response = client.put(parentPid.getRepositoryUri()).perform()) {
        }

        InputStream contentStream = new ByteArrayInputStream(BODY_STRING.getBytes());

        BinaryObject obj = repoObjFactory.createBinary(parentPid.getRepositoryUri(), "binary_test",
                contentStream, FILENAME, MIMETYPE, CHECKSUM, null, null);

        // Verify that the body of the binary is retrieved
        String respString = IOUtils.toString(obj.getBinaryStream());
        assertEquals(BODY_STRING, respString, "Binary content did not match submitted value");

        // Check that metadata is retrieved
        assertEquals(FILENAME, obj.getFilename());
        assertEquals(MIMETYPE, obj.getMimetype());
        assertEquals(BODY_STRING.length(), obj.getFilesize().longValue());
        assertEquals("urn:sha1:" + CHECKSUM, obj.getSha1Checksum());

        assertTrue(obj.getResource().hasProperty(RDF.type, Fcrepo4Repository.Binary));
    }

    @Test
    public void getParent() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);

        BinaryObject binObj = fileObj.addOriginalFile(makeContentUri(fileObj.getPid(), BODY_STRING), FILENAME, MIMETYPE, null, null);

        treeIndexer.indexAll(baseAddress);

        RepositoryObject parent = binObj.getParent();
        assertEquals(parent.getPid(), fileObj.getPid(),
                "Parent of the binary must match the file object which created it");
    }

    private URI makeContentUri(PID binPid, String bodyString) throws IOException {
        var origUri = storageLocationTestHelper.makeTestStorageUri(binPid);
        FileUtils.writeStringToFile(new File(origUri), bodyString, "UTF-8");
        return origUri;
    }
}
