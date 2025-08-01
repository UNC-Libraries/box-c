package edu.unc.lib.boxc.operations.impl.sitemap;

import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class CreateSitemapServiceTest {
    private static final String UUID1 = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String UUID2 = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String COLLECTION_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private static final String ADMIN_UNIT_UUID = "5158b962-9e59-4ed8-b920-fc948213efd3";

    private AutoCloseable closeable;
    @Mock
    private SolrSearchService solrSearchService;

    public Path temporaryFolderPath;

    private CreateSitemapService createSitemapService;

    @BeforeEach
    public void setup() throws IOException {
        closeable = openMocks(this);
        temporaryFolderPath = Files.createTempDirectory("createSitemapServiceTest");
        createSitemapService = new CreateSitemapService();
        createSitemapService.setSolrSearchService(solrSearchService);
        createSitemapService.setAccessGroups(new AccessGroupSetImpl("agroup"));
        createSitemapService.setSitemapBasePath(temporaryFolderPath.toString() + "/");
        createSitemapService.setSitemapBaseUrl("https://sitemaps.example.com/");
        createSitemapService.setBaseUrl("https://sitemaps.example.com/");
    }

    @AfterEach
    void closeService() throws Exception {
        FileUtils.deleteDirectory(temporaryFolderPath.toFile());
        closeable.close();
    }

    @Test
    public void createSitemapTest() throws Exception {
        createSitemap();

        Assertions.assertTrue(Files.exists(temporaryFolderPath.resolve("sitemap.xml")));
        Assertions.assertTrue(Files.exists(temporaryFolderPath.resolve("page_1.xml")));
    }

    @Test
    public void sitemapIndexTest() throws Exception {
        createSitemap();

        File xmlFile = new File(String.valueOf(temporaryFolderPath.resolve("sitemap.xml")));
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        NodeList nodeList = doc.getElementsByTagName("loc");

        Assertions.assertEquals(1, nodeList.getLength());
        Assertions.assertEquals("https://sitemaps.example.com/sitemap/page_1.xml", nodeList.item(0).getTextContent());
    }

    @Test
    public void sitemapPageTest() throws Exception {
        createSitemap();

        File xmlFile = new File(String.valueOf(temporaryFolderPath.resolve("page_1.xml")));
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        NodeList nodeList = doc.getElementsByTagName("loc");

        Assertions.assertEquals(2, nodeList.getLength());
        Assertions.assertEquals("https://sitemaps.example.com/f277bb38-272c-471c-a28a-9887a1328a1f", nodeList
                .item(0).getTextContent());
        Assertions.assertEquals("https://sitemaps.example.com/ba70a1ee-fa7c-437f-a979-cc8b16599652", nodeList
                .item(1).getTextContent());
    }

    private void createSitemap() {
        makeRecord(COLLECTION_UUID, ADMIN_UNIT_UUID, ResourceType.Collection, "Collection", new Date());
        var workRecord1 = makeWorkRecord(UUID1, "Work 1");
        var workRecord2 = makeWorkRecord(UUID2, "Work 2");
        mockResults(workRecord1, workRecord2);

        createSitemapService.generateSitemap();
    }

    private SearchResultResponse makeResultResponse(ContentObjectRecord... results) {
        var resp = new SearchResultResponse();
        resp.setResultList(Arrays.asList(results));
        resp.setResultCount(results.length);
        return resp;
    }

    private ContentObjectRecord makeWorkRecord(String uuid, String title) {
        Date dateUpdated = new Date();
        return makeRecord(uuid, COLLECTION_UUID, ResourceType.Work, title, dateUpdated);
    }

    private void mockResults(ContentObjectRecord... results) {
        when(solrSearchService.getSearchResults(any())).thenReturn(makeResultResponse(results));
    }

    private ContentObjectRecord makeRecord(String uuid, String parentUuid, ResourceType resourceType, String title,
                                           Date dateUpdated) {
        var rec = new ContentObjectSolrRecord();
        rec.setId(uuid);
        rec.setAncestorPath(makeAncestorPath(parentUuid));
        rec.setResourceType(resourceType.name());
        rec.setTitle(title);
        rec.setRoleGroup(Arrays.asList("patron|public", "canViewOriginals|everyone"));
        rec.setDateUpdated(dateUpdated);
        return rec;
    }

    private List<String> makeAncestorPath(String parentUuid) {
        return Arrays.asList("1,collections", "2," + ADMIN_UNIT_UUID, "3," + COLLECTION_UUID, "4," + parentUuid);
    }
}