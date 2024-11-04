package edu.unc.lib.boxc.web.services.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.indexing.solr.test.TestCorpus;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.api.images.ImageServerUtil;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import edu.unc.lib.boxc.web.common.services.DerivativeContentService;
import edu.unc.lib.boxc.web.common.services.FedoraContentService;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.common.utils.AnalyticsTrackerUtil;
import edu.unc.lib.boxc.web.services.processing.DownloadImageService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.fcrepo.client.FcrepoClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static edu.unc.lib.boxc.auth.api.Permission.viewAccessCopies;
import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.MD_EVENTS;
import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.createModel;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getTechnicalMetadataPid;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static edu.unc.lib.boxc.web.common.services.FedoraContentService.CONTENT_DISPOSITION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpHeaders.RANGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author bbpennel
 *
 */
@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
        @ContextConfiguration("/datastream-content-it-servlet.xml")
})

@WireMockTest(httpPort = 46887)
public class DatastreamRestControllerIT extends AbstractAPIIT {

    private static final String BINARY_CONTENT = "binary content";

    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private SolrQueryLayerService solrQueryLayerService;
    @Autowired
    private FedoraContentService fedoraContentService;
    @Autowired
    private DerivativeContentService derivativeContentService;
    @Autowired
    private AccessCopiesService accessCopiesService;
    @Autowired
    private PremisLoggerFactory premisLoggerFactory;
    @Autowired
    private AnalyticsTrackerUtil analyticsTrackerUtil;
    @Autowired
    private FcrepoClient fcrepoClient;
    @Autowired
    private EmbeddedSolrServer embeddedSolrServer;
    @Autowired
    private DownloadImageService downloadImageService;
    private DatastreamController controller;

    @TempDir
    public Path derivDir;
    private String derivDirPath;
    private AutoCloseable closeable;

    @BeforeEach
    public void initLocal() {
        closeable = openMocks(this);
        controller = new DatastreamController();
        controller.setAnalyticsTracker(analyticsTrackerUtil);
        controller.setSolrQueryLayerService(solrQueryLayerService);
        controller.setAccessControlService(accessControlService);
        controller.setFedoraContentService(fedoraContentService);
        controller.setDerivativeContentService(derivativeContentService);
        controller.setAccessCopiesService(accessCopiesService);
        controller.setDownloadImageService(downloadImageService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        derivDirPath = derivDir.toString();

        DerivativeService derivService = new DerivativeService();
        derivService.setDerivativeDir(derivDirPath);

        derivativeContentService.setDerivativeService(derivService);
        fedoraContentService.setClient(fcrepoClient);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetFile() throws Exception {
        PID filePid = makePid();

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        Path contentPath = createBinaryContent(BINARY_CONTENT);
        fileObj.addOriginalFile(contentPath.toUri(), "file.txt", "text/plain", null, null);

        MvcResult result = mvc.perform(get("/file/" + filePid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(BINARY_CONTENT, response.getContentAsString());

        assertEquals(BINARY_CONTENT.length(), response.getContentLength());
        assertEquals("text/plain", response.getContentType());
        assertEquals("inline; filename=\"file.txt\"", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetMultipleDatastreams() throws Exception {
        PID filePid = makePid();

        String content = "<fits>content</fits>";

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        Path originalPath = createBinaryContent(BINARY_CONTENT);
        fileObj.addOriginalFile(originalPath.toUri(), null, "text/plain", null, null);

        PID fitsPid = getTechnicalMetadataPid(filePid);
        Path techmdPath = createBinaryContent(content,"fits", ".xml");
        fileObj.addBinary(fitsPid, techmdPath.toUri(),
                "fits.xml", "application/xml", null, null, null);

        // Verify original file content retrievable
        MvcResult result1 = mvc.perform(get("/file/" + filePid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(BINARY_CONTENT, result1.getResponse().getContentAsString());

        // Verify administrative datastream retrievable
        MvcResult result2 = mvc.perform(get("/file/" + filePid.getId() + "/" + TECHNICAL_METADATA.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result2.getResponse();
        assertEquals(content, response.getContentAsString());

        assertEquals(content.length(), response.getContentLength());
        assertEquals("application/xml", response.getContentType());
        assertEquals("inline; filename=\"fits.xml\"", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetThumbnailWithFileObject() throws Exception {
        var corpus = populateCorpus();
        PID filePid = corpus.pid6File;
        String id = filePid.getId();

        var filename = "bunny.jpg";
        var formattedBasePath = "/iiif/v3/" + ImageServerUtil.getImageServerEncodedId(id);
        stubFor(WireMock.get(urlMatching(formattedBasePath + "/full/!64,64/0/default.jpg"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(filename)
                        .withHeader("Content-Type", "image/jpeg")));

        MvcResult result = mvc.perform(get("/thumb/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        // TO DO assert correct image returned
        assertEquals("image/jpeg", response.getContentType());
        assertEquals("inline;", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetThumbnailForWork() throws Exception {
        var corpus = populateCorpus();
        PID filePid = corpus.pid6File;
        PID workPid = corpus.pid6;

        var filename = "bunny.jpg";
        var formattedBasePath = "/iiif/v3/" + ImageServerUtil.getImageServerEncodedId(filePid.getId());
        stubFor(WireMock.get(urlMatching(formattedBasePath + "/full/!128,128/0/default.jpg"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(filename)
                        .withHeader("Content-Type", MediaType.IMAGE_JPEG_VALUE)));

        MvcResult result = mvc.perform(get("/thumb/" + workPid.getId() + "/large"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        // TO DO assert correct image returned
        assertEquals(MediaType.IMAGE_JPEG_VALUE, response.getContentType());
        assertEquals("inline;", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetThumbnailForCollection() throws Exception {
        var corpus = populateCorpus();
        var collectionPid = corpus.pid2;
        var id = collectionPid.getId();
        createDerivative(id, JP2_ACCESS_COPY, BINARY_CONTENT.getBytes());

        var filename = "bunny.jpg";
        var formattedBasePath = "/iiif/v3/" + ImageServerUtil.getImageServerEncodedId(collectionPid.getId());
        stubFor(WireMock.get(urlMatching(formattedBasePath + "/full/!128,128/0/default.jpg"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(filename)
                        .withHeader("Content-Type", MediaType.IMAGE_JPEG_VALUE)));

        MvcResult result = mvc.perform(get("/thumb/" + id + "/large"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(filename, response.getContentAsString());
        assertEquals(MediaType.IMAGE_JPEG_VALUE, response.getContentType());
        assertEquals("inline;", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetInvalidThumbnailSize() throws Exception {
        var corpus = populateCorpus();
        PID filePid = corpus.pid6File;

        mvc.perform(get("/thumb/" + filePid.getId() + "/megasize"))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void testGetThumbnailNoPermission() throws Exception {
        var corpus = populateCorpus();
        PID filePid = corpus.pid6File;

        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSetImpl.class), eq(viewAccessCopies));

        MvcResult result = mvc.perform(get("/thumb/" + filePid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("", response.getContentAsString(), "Content must not be returned");
    }

    @Test
    public void testGetThumbnailReturnPlaceholder() throws Exception {
        var corpus = populateCorpus();
        var collectionPid = corpus.pid3;
        var placeholder = "placeholder.png";
        var id = URLEncoder.encode("/default_images/placeholder.png", UTF_8);

        stubFor(WireMock.get(urlMatching("/iiif/v3/" + id + "/full/!128,128/0/default.jpg"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBodyFile(placeholder)
                        .withHeader("Content-Type", "image/png")));

        MvcResult result = mvc.perform(get("/thumb/" + collectionPid.getId() + "/large"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        // TO DO assert correct image returned
        assertEquals(MediaType.IMAGE_PNG_VALUE, response.getContentType());
        assertEquals("inline;", response.getHeader(CONTENT_DISPOSITION));

    }

    private TestCorpus populateCorpus() throws Exception {
        var corpus = new TestCorpus();
        embeddedSolrServer.add(corpus.populate());
        embeddedSolrServer.commit();
        return corpus;
    }

    @Test
    public void testGetFileDerivative() throws Exception {
        PID filePid = makePid();
        String id = filePid.getId();
        createDerivative(id, JP2_ACCESS_COPY, BINARY_CONTENT.getBytes());

        MvcResult result = mvc.perform(get("/file/" + filePid.getId() + "/" + JP2_ACCESS_COPY.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(BINARY_CONTENT, response.getContentAsString());
        assertEquals(BINARY_CONTENT.length(), response.getContentLength());
        assertEquals("image/jp2", response.getContentType());
        assertEquals("inline; filename=\"" + id + "." + JP2_ACCESS_COPY.getExtension() + "\"",
                response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetThumbnailNotPresent() throws Exception {
        PID filePid = makePid();

        mvc.perform(get("/thumb/" + filePid.getId()))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void testGetEventLog() throws Exception {
        PID folderPid = makePid();
        String id = folderPid.getId();

        FolderObject folderObj = repositoryObjectFactory.createFolderObject(folderPid, null);
        premisLoggerFactory.createPremisLogger(folderObj)
                .buildEvent(Premis.Creation)
                .addAuthorizingAgent(AgentPids.forPerson("some_user"))
                .writeAndClose();

        MvcResult result = mvc.perform(get("/file/" + id + "/" + MD_EVENTS.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals("text/turtle", response.getContentType());
        assertEquals("inline; filename=\"" + id + "_event_log.ttl\"", response.getHeader(CONTENT_DISPOSITION));

        Model premisModel = createModel(IOUtils.toInputStream(response.getContentAsString(), UTF_8), "TURTLE");
        assertEquals(1, premisModel.listResourcesWithProperty(RDF.type, Premis.Creation).toList().size(),
                "Response did not contain expected premis event");

        assertTrue(response.getContentLength() > 0, "Expected content length to be set");
    }

    @Test
    public void testGetEventLogNotPresent() throws Exception {
        PID folderPid = makePid();
        String id = folderPid.getId();

        repositoryObjectFactory.createFolderObject(folderPid, null);

        MvcResult result = mvc.perform(get("/file/" + id + "/" + MD_EVENTS.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();

        assertEquals("text/turtle", response.getContentType());
        assertEquals(0, response.getContentLength(), "Expected empty response");
    }

    @Test
    public void testGetEventLogNoPermissions() throws Exception {
        PID folderPid = makePid();
        String id = folderPid.getId();

        FolderObject folderObj = repositoryObjectFactory.createFolderObject(folderPid, null);
        premisLoggerFactory.createPremisLogger(folderObj)
                .buildEvent(Premis.Creation)
                .addAuthorizingAgent(AgentPids.forPerson("some_user"))
                .writeAndClose();

        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(folderPid), any(AccessGroupSetImpl.class), eq(viewHidden));

        MvcResult result = mvc.perform(get("/file/" + id + "/" + MD_EVENTS.getId()))
                .andExpect(status().isForbidden())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("", response.getContentAsString(), "Content must not be returned");
    }

    @Test
    public void testRangeExceedsFileLength() throws Exception {
        PID filePid = makePid();

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        Path contentPath = createBinaryContent(BINARY_CONTENT);
        fileObj.addOriginalFile(contentPath.toUri(), "file.txt", "text/plain", null, null);

        mvc.perform(get("/file/" + filePid.getId())
                        .header(RANGE,"bytes=900000-900001"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andReturn();
    }

    private void createDerivative(String id, DatastreamType dsType, byte[] content) throws Exception {
        String hashedPath = idToPath(id, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        Path derivPath = Paths.get(derivDirPath, dsType.getId(), hashedPath,
                id + "." + dsType.getExtension());

        File derivFile = derivPath.toFile();
        derivFile.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(derivFile, content);
    }
}