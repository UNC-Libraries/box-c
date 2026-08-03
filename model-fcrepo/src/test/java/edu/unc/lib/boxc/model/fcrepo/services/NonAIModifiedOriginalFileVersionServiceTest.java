package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.file.Path;

import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class NonAIModifiedOriginalFileVersionServiceTest {

    private static final String FEDORA_BASE = "http://example.com/rest/";
    private static final String FILE_ID = "de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private static final String RESC_URI = FEDORA_BASE + "content/de/75/d8/11/" + FILE_ID;
    private static final String MIMETYPE = "text/plain";
    private static final String VERSION1_DATE = "20250727195502";
    private static final String VERSION2_DATE = "20260727195502";
    private AutoCloseable closeable;
    private Model objModel, version1Model, version2Model;
    private Resource objResc, version1Resource, version2Resource;
    private PID fileObjectPid, originalFilePid;
    private OriginalFileVersionService originalFileVersionService;
    private File objModelFile, version1ModelFile, version2ModelFile;
    @TempDir
    public Path tmpFolder;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private FcrepoResponse versionsResponse, version1Response, version2Response;
    @Mock
    private GetBuilder getVersionsBuilder, getVersion1Builder, getVersion2Builder;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
        originalFileVersionService = new OriginalFileVersionService();
        originalFileVersionService.setFcrepoClient(fcrepoClient);
        getVersionsBuilder = mock(GetBuilder.class, new SelfReturningAnswer());

        fileObjectPid = makePid();
        originalFilePid = DatastreamPids.getOriginalFilePid(fileObjectPid);
        objModel = ModelFactory.createDefaultModel();
        objResc = objModel.getResource(originalFilePid.getRepositoryPath());
        objResc.addProperty(RDF.type, Cdr.FileObject);
        objModelFile = new File(tmpFolder.toFile(), fileObjectPid.getUUID() + ".txt");
        var versionsUriString = URIUtil.join(originalFilePid.getRepositoryUri(), "fcr:metadata", "fcr:versions");

        var version1UriString = URIUtil.join(versionsUriString, VERSION1_DATE);
        version1Model = ModelFactory.createDefaultModel();
        version1Resource = version1Model.getResource(version1UriString);
        version1Resource.addProperty(Ebucore.filename, "filename1.txt");
        version1Resource.addProperty(Ebucore.hasMimeType, MIMETYPE);
        version1ModelFile = tmpFolder.resolve("version1ModelFile").toFile();
        writeModelToFile(version1Model, version1ModelFile);

        var version2UriString = URIUtil.join(versionsUriString, VERSION2_DATE);
        version2Model = ModelFactory.createDefaultModel();
        version2Resource = version2Model.getResource(version2UriString);
        version2Resource.addProperty(Ebucore.filename, "filename2.txt");
        version2Resource.addProperty(Ebucore.hasMimeType, MIMETYPE);
        version2ModelFile = tmpFolder.resolve("version2ModelFile").toFile();
        writeModelToFile( version2Model, version2ModelFile);

        objResc.addLiteral(Ldp.contains, version1Resource);
        objResc.addLiteral(Ldp.contains, version2Resource);
        writeModelToFile(objModel, objModelFile);

        when(fcrepoClient.get(eq(URI.create(versionsUriString)))).thenReturn(getVersionsBuilder);
        when(getVersionsBuilder.perform()).thenReturn(versionsResponse);
        when(versionsResponse.getBody()).thenReturn(new FileInputStream(objModelFile));
        when(fcrepoClient.get(eq(URI.create(version1UriString)))).thenReturn(getVersion1Builder);
        when(getVersion1Builder.perform()).thenReturn(version1Response);
        when(version1Response.getBody()).thenReturn(new FileInputStream(version1ModelFile));
        when(fcrepoClient.get(eq(URI.create(version2UriString)))).thenReturn(getVersion2Builder);
        when(getVersion2Builder.perform()).thenReturn(version2Response);
        when(version2Response.getBody()).thenReturn(new FileInputStream(version2ModelFile));

    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }


    @Test
    public void successTest() {
        var metadataMap = originalFileVersionService.getVersionMetadata(fileObjectPid);
        assertFalse(metadataMap.isEmpty());
    }


    private void writeModelToFile(Model model, File file) throws Exception {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            RDFDataMgr.write(fileOutputStream, model, RDFFormat.RDFXML);
        }
    }
}
