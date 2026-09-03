package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.services.AccessCopiesService;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.services.processing.DownloadImageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DatastreamControllerTest {
    private static final String USERNAME = "test_user";
    private static final AccessGroupSetImpl GROUPS = new AccessGroupSetImpl("adminGroup");

    @InjectMocks
    private DatastreamController controller;

    @Mock
    private SolrQueryLayerService solrQueryLayerService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private AccessCopiesService accessCopiesService;
    @Mock
    private DownloadImageService downloadImageService;

    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
        TestHelper.setContentBase("http://localhost:48087/rest");
    }

    @AfterEach
    void closeService() throws Exception {
        GroupsThreadStore.clearStore();
        closeable.close();
    }

    @Test
    public void testGetThumbnailForFileReusesInitialSolrRecord() throws Exception {
        var pid = TestHelper.makePid();
        var fileRecord = new ContentObjectSolrRecord();
        fileRecord.setId(pid.getId());
        fileRecord.setResourceType(ResourceType.File.name());
        ResponseEntity<Resource> response = ResponseEntity.ok(new ByteArrayResource(new byte[0]));

        when(solrQueryLayerService.getObjectById(any(SimpleIdRequest.class))).thenReturn(fileRecord);
        when(downloadImageService.streamThumbnail(fileRecord, "160")).thenReturn(response);

        var result = controller.getThumbnail(pid.getId(), "small");

        assertSame(response, result);
        verify(solrQueryLayerService).getObjectById(any(SimpleIdRequest.class));
        verify(accessCopiesService, never()).getThumbnailRecord(any(), any(), anyBoolean());
        verify(downloadImageService).streamThumbnail(fileRecord, "160");
    }

    @Test
    public void testGetThumbnailForWorkReusesThumbnailRecord() throws Exception {
        var workPid = TestHelper.makePid();
        var thumbPid = TestHelper.makePid();
        var workRecord = new ContentObjectSolrRecord();
        workRecord.setId(workPid.getId());
        workRecord.setResourceType(ResourceType.Work.name());
        var thumbRecord = new ContentObjectSolrRecord();
        thumbRecord.setId(thumbPid.getId());
        thumbRecord.setResourceType(ResourceType.File.name());
        ResponseEntity<Resource> response = ResponseEntity.ok(new ByteArrayResource(new byte[0]));

        when(solrQueryLayerService.getObjectById(any(SimpleIdRequest.class))).thenReturn(workRecord);
        when(accessCopiesService.getThumbnailRecord(eq(workRecord), any(), eq(true))).thenReturn(thumbRecord);
        when(downloadImageService.streamThumbnail(thumbRecord, "250")).thenReturn(response);

        var result = controller.getThumbnail(workPid.getId(), "large");

        assertSame(response, result);
        verify(solrQueryLayerService).getObjectById(any(SimpleIdRequest.class));
        verify(accessCopiesService).getThumbnailRecord(eq(workRecord), any(), eq(true));
        verify(downloadImageService).streamThumbnail(thumbRecord, "250");
    }
}
