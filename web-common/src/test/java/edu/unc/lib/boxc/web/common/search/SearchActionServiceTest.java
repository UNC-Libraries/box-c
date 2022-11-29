package edu.unc.lib.boxc.web.common.search;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.utils.SearchStateUtil;
import edu.unc.lib.boxc.web.common.search.SearchActionService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/solr-search-context-unit.xml" })
public class SearchActionServiceTest extends Assert {
    @Autowired
    private SearchSettings searchSettings;
    @Autowired
    private SearchActionService searchActionService;
    private String emptyStateString = null;

    @Before
    public void setUp() throws Exception {
        SearchState searchState = new SearchState();
        this.emptyStateString = SearchStateUtil.generateStateParameterString(searchState);
    }

    @Test
    public void executeActions() {
        SearchState searchState = new SearchState();

        //Action on null search state
        searchActionService.executeActions(null, new LinkedHashMap<String,String[]>());

        //No actions
        searchActionService.executeActions(searchState, new LinkedHashMap<String,String[]>());
        assertEquals(emptyStateString, SearchStateUtil.generateStateParameterString(searchState));
        searchActionService.executeActions(searchState, null);
        assertEquals(emptyStateString, SearchStateUtil.generateStateParameterString(searchState));
    }

    @Test
    public void pagingActions() {
        Map<String,String[]> params = new LinkedHashMap<String,String[]>();
        SearchState searchState = new SearchState();

        //Rows per page not set, no change
        params.put("a." + searchSettings.actionName("NEXT_PAGE"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertNull(searchState.getStartRow());

        //Can't have negative rows per page
        params.clear();
        searchState.setStartRow(0);
        searchState.setRowsPerPage(searchSettings.getDefaultPerPage());
        params.put("a." + searchSettings.actionName("NEXT_PAGE"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchSettings.getDefaultPerPage(), searchState.getStartRow().intValue());

        //Action that should have a parameter
        params.clear();
        params.put("a." + searchSettings.actionName("SET_START_ROW"), new String[]{Integer.toString(searchSettings.getDefaultPerPage())});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getStartRow().intValue(), searchSettings.getDefaultPerPage());

        params.put("a." + searchSettings.actionName("SET_START_ROW"), new String[]{Integer.toString(0)});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getStartRow().intValue(), 0);

        //Test action that takes no parameters, with a param
        params.clear();
        params.put("a." + searchSettings.actionName("NEXT_PAGE"), new String[]{"anywere"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getStartRow().intValue(), searchSettings.getDefaultPerPage());

        params.clear();
        params.put("a." + searchSettings.actionName("PREVIOUS_PAGE"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getStartRow().intValue(), 0);
        //Verify that can't go to a negative page
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getStartRow().intValue(), 0);
    }
}
