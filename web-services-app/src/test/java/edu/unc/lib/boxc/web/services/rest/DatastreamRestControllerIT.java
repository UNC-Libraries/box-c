package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.auth.api.Permission.viewMetadata;
import static edu.unc.lib.boxc.model.api.DatastreamType.MD_EVENTS;
import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.DatastreamType.THUMBNAIL_SMALL;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.createModel;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getTechnicalMetadataPid;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static edu.unc.lib.boxc.web.common.services.FedoraContentService.CONTENT_DISPOSITION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.unc.lib.boxc.indexing.solr.test.TestCorpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.web.common.services.DerivativeContentService;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;

/**
 *
 * @author bbpennel
 *
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextHierarchy({
        @ContextConfiguration("/spring-test/test-fedora-container.xml"),
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
        @ContextConfiguration("/datastream-content-it-servlet.xml")
})
public class DatastreamRestControllerIT extends AbstractAPIIT {

    private static final String BINARY_CONTENT = "binary content";

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private DerivativeContentService derivativeContentService;

    @Autowired
    private PremisLoggerFactory premisLoggerFactory;

    @Autowired
    private EmbeddedSolrServer embeddedSolrServer;

    @TempDir
    public Path derivDir;
    private String derivDirPath;

    @BeforeEach
    public void setup() {
        derivDirPath = derivDir.toString();

        DerivativeService derivService = new DerivativeService();
        derivService.setDerivativeDir(derivDirPath);

        derivativeContentService.setDerivativeService(derivService);
    }

    @Test
    public void testGetFile() throws Exception {
        PID filePid = makePid();

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        Path contentPath = Files.createTempFile("file", ".txt");
        FileUtils.writeStringToFile(contentPath.toFile(), BINARY_CONTENT, "UTF-8");
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
        Path originalPath = Files.createTempFile("file", ".txt");
        FileUtils.writeStringToFile(originalPath.toFile(), BINARY_CONTENT, "UTF-8");
        fileObj.addOriginalFile(originalPath.toUri(), null, "text/plain", null, null);

        PID fitsPid = getTechnicalMetadataPid(filePid);
        Path techmdPath = Files.createTempFile("fits", ".xml");
        FileUtils.writeStringToFile(techmdPath.toFile(), content, "UTF-8");
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
    public void testGetThumbnail() throws Exception {
        var corpus = populateCorpus();

        PID filePid = corpus.pid6File;
        String id = filePid.getId();
        createDerivative(id, THUMBNAIL_SMALL, BINARY_CONTENT.getBytes());

        MvcResult result = mvc.perform(get("/thumb/" + filePid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(BINARY_CONTENT, response.getContentAsString());
        assertEquals(BINARY_CONTENT.length(), response.getContentLength());
        assertEquals("image/png", response.getContentType());
        assertEquals("inline; filename=\"" + id + "." + THUMBNAIL_SMALL.getExtension() + "\"",
                response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetThumbnailForWork() throws Exception {
        var corpus = populateCorpus();

        PID filePid = corpus.pid6File;
        String id = filePid.getId();
        createDerivative(id, THUMBNAIL_SMALL, BINARY_CONTENT.getBytes());
        PID workPid = corpus.pid6;

        MvcResult result = mvc.perform(get("/thumb/" + workPid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(BINARY_CONTENT, response.getContentAsString());
        assertEquals(BINARY_CONTENT.length(), response.getContentLength());
        assertEquals("image/png", response.getContentType());
        assertEquals("inline; filename=\"" + id + "." + THUMBNAIL_SMALL.getExtension() + "\"",
                response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetInvalidThumbnailSize() throws Exception {
        var corpus = populateCorpus();

        PID filePid = corpus.pid6File;
        String id = filePid.getId();
        createDerivative(id, THUMBNAIL_SMALL, BINARY_CONTENT.getBytes());

        mvc.perform(get("/thumb/" + filePid.getId() + "/megasize"))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void testGetThumbnailNoPermission() throws Exception {
        var corpus = populateCorpus();

        PID filePid = corpus.pid6File;
        String id = filePid.getId();
        createDerivative(id, THUMBNAIL_SMALL, BINARY_CONTENT.getBytes());

        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSetImpl.class), eq(viewMetadata));

        MvcResult result = mvc.perform(get("/thumb/" + filePid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("", response.getContentAsString(), "Content must not be returned");
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
        createDerivative(id, THUMBNAIL_SMALL, BINARY_CONTENT.getBytes());

        MvcResult result = mvc.perform(get("/file/" + filePid.getId() + "/" + THUMBNAIL_SMALL.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(BINARY_CONTENT, response.getContentAsString());
        assertEquals(BINARY_CONTENT.length(), response.getContentLength());
        assertEquals("image/png", response.getContentType());
        assertEquals("inline; filename=\"" + id + "." + THUMBNAIL_SMALL.getExtension() + "\"",
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

    private File createDerivative(String id, DatastreamType dsType, byte[] content) throws Exception {
        String hashedPath = idToPath(id, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        Path derivPath = Paths.get(derivDirPath, dsType.getId(), hashedPath,
                id + "." + dsType.getExtension());

        File derivFile = derivPath.toFile();
        derivFile.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(derivFile, content);

        return derivFile;
    }
}