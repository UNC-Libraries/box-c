package edu.unc.lib.boxc.model.fcrepo.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class OriginalFileVersionServiceTest {

    private static final String FEDORA_BASE = "http://example.com/rest/";
    private static final String FILE_ID = "de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private static final String RESC_URI = FEDORA_BASE + "content/de/75/d8/11/" + FILE_ID;
    private static final String MIMETYPE = "text/plain";
    private static final String VERSION1_DATE = "20250727195502";
    private static final String VERSION2_DATE = "20260727195502";
    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(Model.class);
    private AutoCloseable closeable;
    private Model objModel, version1Model, version2Model;
    private Resource objResc, version1Resource, version2Resource;
    private PID fileObjectPid, originalFilePid;
    private OriginalFileVersionService originalFileVersionService;

    @TempDir
    public Path tmpFolder;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private FcrepoResponse versionsResponse, version1Response, version2Response;
    @Mock
    private GetBuilder getVersionsBuilder, getVersion1Builder, getVersion2Builder;

    @BeforeEach
    public void init() throws FcrepoOperationFailedException, JsonProcessingException {
        closeable = openMocks(this);
        originalFileVersionService = new OriginalFileVersionService();
        getVersionsBuilder = mock(GetBuilder.class, new SelfReturningAnswer());

        fileObjectPid = makePid();
        originalFilePid = DatastreamPids.getOriginalFilePid(fileObjectPid);
        objModel = ModelFactory.createDefaultModel();
        objResc = objModel.getResource(originalFilePid.getRepositoryPath());
        var versionsUriString = URIUtil.join(originalFilePid.getRepositoryUri(), "fcr:metadata", "fcr:versions");
        var objModelString = MAPPER.writeValueAsString(objModel);

        var version1UriString = URIUtil.join(versionsUriString, VERSION1_DATE);
        version1Model = ModelFactory.createDefaultModel();
        version1Resource = version1Model.getResource(version1UriString);
        version1Resource.addProperty(Ebucore.filename, "filename1.txt");
        version1Resource.addProperty(Ebucore.hasMimeType, MIMETYPE);
        var version1ModelString = MAPPER.writeValueAsString(version1Model);

        var version2UriString = URIUtil.join(versionsUriString, VERSION2_DATE);
        version2Model = ModelFactory.createDefaultModel();
        version2Resource = version1Model.getResource(version2UriString);
        version2Resource.addProperty(Ebucore.filename, "filename2.txt");
        version2Resource.addProperty(Ebucore.hasMimeType, MIMETYPE);
        var version2ModelString = MAPPER.writeValueAsString(version2Model);

        objResc.addLiteral(Ldp.contains, version1Resource);
        objResc.addLiteral(Ldp.contains, version2Resource);

        when(fcrepoClient.get(eq(URI.create(versionsUriString)))).thenReturn(getVersionsBuilder);
        when(getVersionsBuilder.perform()).thenReturn(versionsResponse);
        when(versionsResponse.getBody()).thenReturn(new ByteArrayInputStream(objModelString.getBytes(StandardCharsets.UTF_8)));
        when(fcrepoClient.get(eq(URI.create(version1UriString)))).thenReturn(getVersion1Builder);
        when(getVersion1Builder.perform()).thenReturn(version1Response);
        when(version1Response.getBody()).thenReturn(new ByteArrayInputStream(version1ModelString.getBytes(StandardCharsets.UTF_8)));
        when(fcrepoClient.get(eq(URI.create(version2UriString)))).thenReturn(getVersion2Builder);
        when(getVersion2Builder.perform()).thenReturn(version2Response);
        when(version2Response.getBody()).thenReturn(new ByteArrayInputStream(version2ModelString.getBytes(StandardCharsets.UTF_8)));

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


}
