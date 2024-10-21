package edu.unc.lib.boxc.web.common.services;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.ContentCategory;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author lfarrell
 */
public class WorkFilesizeServiceTest {
    private AccessGroupSet principals;

    private WorkFilesizeService workFilesizeService;

    private AutoCloseable closeable;

    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private SearchResultResponse searchResultResponse;
    @Captor
    private ArgumentCaptor<SearchRequest> searchRequestCaptor;

    @BeforeEach
    public void init() throws IOException, SolrServerException {
        closeable = openMocks(this);

        principals = new AccessGroupSetImpl("group");

        workFilesizeService = new WorkFilesizeService();
        workFilesizeService.setSolrSearchService(solrSearchService);
        workFilesizeService.setGlobalPermissionEvaluator(globalPermissionEvaluator);

        when(solrSearchService.getSearchResults(searchRequestCaptor.capture())).thenReturn(searchResultResponse);
        when(searchResultResponse.getResultCount()).thenReturn(1L);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void workObjectWithBulkDownload() {
        var fileSize = 8194L;
        var mdObject = createObject(ResourceType.Work, fileSize);
        when(solrSearchService.getSearchResults(searchRequestCaptor.capture()).getResultList())
                .thenReturn(List.of(mdObject));
        assertEquals(FileUtils.byteCountToDisplaySize(fileSize),
                workFilesizeService.getTotalFilesize(mdObject, principals));
    }

    @Test
    public void workObjectWithBulkDownloadGreaterThan1GB() {
        var TWO_GIGABYTE_FILE = 2147483648L;
        var mdObject = createObject(ResourceType.Work, TWO_GIGABYTE_FILE);
        when(solrSearchService.getSearchResults(searchRequestCaptor.capture())
                .getResultList()).thenReturn(List.of(mdObject));
        assertEquals("-1", workFilesizeService.getTotalFilesize(mdObject, principals));
    }

    private ContentObjectSolrRecord createObject(ResourceType resourceType, Long filesize) {
        var mdObjectImg = new ContentObjectSolrRecord();
        mdObjectImg.setResourceType(resourceType.name());
        var id = UUID.randomUUID().toString();
        mdObjectImg.setId(id);
        mdObjectImg.setFilesizeTotal(filesize);
        List<String> imgDatastreams = List.of(
                ORIGINAL_FILE.getId() + "|image/png|file.png|png|" + filesize + "|urn:sha1:checksum|",
                JP2_ACCESS_COPY.getId() + "|image/jp2|bunny.jp2|jp2|||" + id + "|1200x1200");
        mdObjectImg.setFileFormatCategory(Collections.singletonList(ContentCategory.image.getDisplayName()));
        mdObjectImg.setFileFormatType(Collections.singletonList("image/png"));
        mdObjectImg.setDatastream(imgDatastreams);
        return mdObjectImg;
    }
}
