package edu.unc.lib.boxc.model.fcrepo.services;

import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

public class OriginalFileVersionServiceTest {
    private static final String MIMETYPE = "text/plain";
    private static final String VERSION1_DATE = "20250727195502";
    private static final String VERSION2_DATE = "20260727195502";
    private OriginalFileVersionService originalFileVersionService;
    private AutoCloseable closeable;
    private PID fileObjectPid, version1Pid, version2Pid;
    private File objModelFile, version1ModelFile, version2ModelFile;
    private GetBuilder getVersionsBuilder, getVersion1Builder, getVersion2Builder;

    @TempDir
    public Path tmpFolder;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private FcrepoResponse versionsResponse;
    @Mock
    private FcrepoResponse version1Response;
    @Mock
    private FcrepoResponse version2Response;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        originalFileVersionService = new OriginalFileVersionService();
        originalFileVersionService.setFcrepoClient(fcrepoClient);

        getVersionsBuilder = mock(GetBuilder.class, new SelfReturningAnswer());
        getVersion1Builder = mock(GetBuilder.class, new SelfReturningAnswer());
        getVersion2Builder = mock(GetBuilder.class, new SelfReturningAnswer());

        fileObjectPid = makePid();
        PID originalFilePid = DatastreamPids.getOriginalFilePid(fileObjectPid);

        String versionsUriString = URIUtil.join(originalFilePid.getRepositoryUri(), "fcr:metadata", "fcr:versions");
        String version1UriString = URIUtil.join(versionsUriString, VERSION1_DATE);
        String version2UriString = URIUtil.join(versionsUriString, VERSION2_DATE);

        version1Pid = PIDs.get(version1UriString);
        version2Pid = PIDs.get(version2UriString);

        Model objModel = ModelFactory.createDefaultModel();
        Resource objResc = objModel.getResource(originalFilePid.getRepositoryPath());
        objResc.addProperty(RDF.type, Ldp.RdfSource);
        objResc.addProperty(Ldp.contains, objModel.createResource(version1UriString));
        objResc.addProperty(Ldp.contains, objModel.createResource(version2UriString));

        Model version1Model = ModelFactory.createDefaultModel();
        Resource version1Resource = version1Model.getResource(version1UriString);
        version1Resource.addProperty(Ebucore.filename, "filename1.txt");
        version1Resource.addProperty(Ebucore.hasMimeType, MIMETYPE);

        Model version2Model = ModelFactory.createDefaultModel();
        Resource version2Resource = version2Model.getResource(version2UriString);
        version2Resource.addProperty(Ebucore.filename, "filename2.txt");
        version2Resource.addProperty(Ebucore.hasMimeType, MIMETYPE);

        objModelFile = tmpFolder.resolve("objModel.rdf").toFile();
        version1ModelFile = tmpFolder.resolve("version1Model.rdf").toFile();
        version2ModelFile = tmpFolder.resolve("version2Model.rdf").toFile();

        writeModelToFile(objModel, objModelFile);
        writeModelToFile(version1Model, version1ModelFile);
        writeModelToFile(version2Model, version2ModelFile);

        when(fcrepoClient.get(any(URI.class))).thenAnswer(inv -> {
            URI uri = inv.getArgument(0);
            String uriString = uri.toString();
            System.out.println("fcrepoClient.get called with: " + uriString);

            if (uriString.endsWith("/fcr:versions")) {
                return getVersionsBuilder;
            }
            if (uriString.contains(VERSION1_DATE)) {
                return getVersion1Builder;
            }
            if (uriString.contains(VERSION2_DATE)) {
                return getVersion2Builder;
            }

            throw new AssertionError("Unexpected URI passed to fcrepoClient.get(): " + uriString);
        });

        when(getVersionsBuilder.perform()).thenReturn(versionsResponse);
        when(versionsResponse.getBody()).thenAnswer(inv -> new java.io.FileInputStream(objModelFile));

        when(getVersion1Builder.perform()).thenReturn(version1Response);
        when(version1Response.getBody()).thenAnswer(inv -> new java.io.FileInputStream(version1ModelFile));

        when(getVersion2Builder.perform()).thenReturn(version2Response);
        when(version2Response.getBody()).thenAnswer(inv -> new java.io.FileInputStream(version2ModelFile));
    }

    @AfterEach
    void closeService() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    public void successTest() {
        Map<PID, Map<String, String>> metadataMap = originalFileVersionService.getVersionMetadata(fileObjectPid);

        assertFalse(metadataMap.isEmpty());
        assertEquals(2, metadataMap.size());

        assertVersionMetadata(metadataMap, version1Pid, "filename1.txt");
        assertVersionMetadata(metadataMap, version2Pid, "filename2.txt");
    }

    private void assertVersionMetadata(Map<PID, Map<String, String>> metadataMap, PID pid, String expectedFilename) {
        assertTrue(metadataMap.containsKey(pid));

        Map<String, String> metadata = metadataMap.get(pid);
        assertEquals(expectedFilename, metadata.get("filename"));
        assertEquals(MIMETYPE, metadata.get("mimetype"));
    }

    private void writeModelToFile(Model model, File file) throws Exception {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            RDFDataMgr.write(fileOutputStream, model, RDFFormat.RDFXML);
        }
    }
}