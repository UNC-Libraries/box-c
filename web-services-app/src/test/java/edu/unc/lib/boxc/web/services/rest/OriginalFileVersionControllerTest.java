package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.OriginalFileVersionService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OriginalFileVersionControllerTest {
    private static final String MIMETYPE = "text/plain";
    private static final String VERSION1_DATE = "20250727195502";
    private static final String VERSION2_DATE = "20260727195530";
    private static final String OBJECT_ID = "e2847b41-e0ee-45bb-bdb3-a97a6241bee5";
    private static final PID OBJECT_PID = PIDs.get(OBJECT_ID);
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    private OriginalFileVersionService service;
    private MockMvc mockMvc;
    private AutoCloseable closeable;
    private GetBuilder getVersionsBuilder, getVersion1Builder, getVersion2Builder;
    @InjectMocks
    private OriginalFileVersionsController versionsController;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private FcrepoResponse versionsResponse;
    @Mock
    private FcrepoResponse version1Response;
    @Mock
    private FcrepoResponse version2Response;

    private Resource version1Resource;

    private Resource version2Resource;

    @BeforeEach
    public void setup() throws FcrepoOperationFailedException {
        closeable = openMocks(this);
        service = new OriginalFileVersionService();
        service.setFcrepoClient(fcrepoClient);
        versionsController.setAccessControlService(accessControlService);
        versionsController.setService(service);
        getVersionsBuilder = mock(GetBuilder.class, new SelfReturningAnswer());
        getVersion1Builder = mock(GetBuilder.class, new SelfReturningAnswer());
        getVersion2Builder = mock(GetBuilder.class, new SelfReturningAnswer());

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
        when(versionsResponse.getBody()).thenAnswer(inv -> new java.io.FileInputStream("src/test/resources/rdf/file-object-versions.rdf"));

        when(getVersion1Builder.perform()).thenReturn(version1Response);
        when(version1Response.getBody()).thenAnswer(inv -> new java.io.FileInputStream("src/test/resources/rdf/version1.rdf"));

        when(getVersion2Builder.perform()).thenReturn(version2Response);
        when(version2Response.getBody()).thenAnswer(inv -> new java.io.FileInputStream("src/test/resources/rdf/version2.rdf"));

        mockMvc = MockMvcBuilders.standaloneSetup(versionsController)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void noPermissionTest() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(any(), eq(OBJECT_PID), any(), eq(Permission.viewMetadata));

        mockMvc.perform(get("/version/" + OBJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void successTest() throws Exception {
        PID originalFilePid = DatastreamPids.getOriginalFilePid(OBJECT_PID);

        String versionsUriString = URIUtil.join(originalFilePid.getRepositoryUri(), "fcr:metadata", "fcr:versions");
        String version1UriString = URIUtil.join(originalFilePid.getRepositoryUri(), "fcr:versions", VERSION1_DATE);
        String version2UriString = URIUtil.join(originalFilePid.getRepositoryUri(),"fcr:versions", VERSION2_DATE);

        Model objModel = ModelFactory.createDefaultModel();
        version1Resource = objModel.getResource(version1UriString);
        version1Resource.addProperty(Ebucore.filename, "00276_op0178_0001.tif");
        version1Resource.addProperty(Ebucore.hasMimeType, MIMETYPE);
        version2Resource = objModel.getResource(version2UriString);
        version2Resource.addProperty(Ebucore.filename, "00276_op0178_0001_2.tif");
        version2Resource.addProperty(Ebucore.hasMimeType, MIMETYPE);


        var result = mockMvc.perform(get("/version/" + OBJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }
}
