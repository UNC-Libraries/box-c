package edu.unc.lib.boxc.operations.impl.sitemap;

import com.redfin.sitemapgenerator.SitemapIndexGenerator;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.filters.QueryFilterFactory;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.model.api.ResourceType.Work;

@Component
@EnableScheduling
public class CreateSitemapService {
    private static final Logger LOG = LoggerFactory.getLogger(CreateSitemapService.class);
    private static final int DEFAULT_PAGE_SIZE = 10000;
    private static final List<String> METADATA_FIELDS = List.of(SearchFieldKey.ID.name(), SearchFieldKey.DATE_UPDATED.name());
    public static final String AGENT_NAME = "SitemapGenerationAgent";
    private SolrSearchService solrSearchService;
    private AgentPrincipals agentPrincipals;
    private String baseUrl;
    private String sitemapBaseUrl;
    private String sitemapBasePath;

    @Scheduled(cron = "${sitemap.cron.schedule}")
    @Scheduled(cron = "0 */15 * * * *")
    public void generateSitemap() {
        try {
            int pagePrefix = 1;
            ArrayList<String> pages = new ArrayList<>();
            var works = getRecords(0);
            pages.add(buildSitemapPage(works.getResultList(), pagePrefix));

            if (works.getResultCount() > DEFAULT_PAGE_SIZE) {
                for (var i = DEFAULT_PAGE_SIZE; i < works.getResultCount(); i+= DEFAULT_PAGE_SIZE) {
                    pagePrefix += 1;
                    works = getRecords(i);
                    pages.add(buildSitemapPage(works.getResultList(), pagePrefix));
                }
            }

            buildSitemapIndex(pages);
        } catch (MalformedURLException e) {
            LOG.warn("Unable to generate DCR sitemap {}", e.getMessage());
        }
    }

    private void buildSitemapIndex(ArrayList<String> pages) throws MalformedURLException {
        SitemapIndexGenerator sitemapIndex = new SitemapIndexGenerator(sitemapBaseUrl,
                new File(sitemapBasePath + "sitemap.xml"));
        for (String page : pages) {
            sitemapIndex.addUrl(page);
        }
        sitemapIndex.write();
    }

    /**
     * Creates a sitemap page and returns the URL of the page
     * @param works
     * @param prefix
     * @return String
     * @throws MalformedURLException
     */
    private String buildSitemapPage(List<ContentObjectRecord> works, int prefix) throws MalformedURLException {
        var page_name = "page_" + prefix;

        var wsg = WebSitemapGenerator.builder(sitemapBaseUrl, new File(sitemapBasePath))
                .fileNamePrefix(page_name).build();
        for (ContentObjectRecord work: works) {
            var url = new WebSitemapUrl.Options(baseUrl + work.getId()).lastMod(work.getDateUpdated()).build();
            wsg.addUrl(url);
        }
        wsg.write();

        return sitemapBaseUrl + "sitemap/" + page_name + ".xml";
    }

    private SearchResultResponse getRecords(int startRow) {
        SearchState searchState = new SearchState();
        searchState.setStartRow(startRow);
        searchState.setIgnoreMaxRows(true);
        searchState.setRowsPerPage(DEFAULT_PAGE_SIZE);
        searchState.setResourceTypes(List.of(Work.name()));
        searchState.addFilter(QueryFilterFactory.createHasValuesFilter(SearchFieldKey.READ_GROUP, List.of(PUBLIC_PRINC)));
        searchState.setSortType("default");
        searchState.setResultFields(METADATA_FIELDS);

        var searchRequest = new SearchRequest(searchState, agentPrincipals.getPrincipals());
        return solrSearchService.getSearchResults(searchRequest);
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    public void setAccessGroups(AccessGroupSet accessGroups) {
        this.agentPrincipals = new AgentPrincipalsImpl(AGENT_NAME, accessGroups);
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setSitemapBaseUrl(String sitemapBaseUrl) {
        this.sitemapBaseUrl = sitemapBaseUrl;
    }

    public void setSitemapBasePath(String sitemapBasePath) {
        this.sitemapBasePath = sitemapBasePath;
    }
}