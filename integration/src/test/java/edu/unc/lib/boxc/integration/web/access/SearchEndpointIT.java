/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.boxc.integration.web.access;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.integration.factories.AdminUnitFactory;
import edu.unc.lib.boxc.integration.factories.CollectionFactory;
import edu.unc.lib.boxc.integration.factories.ContentRootObjectFactory;
import edu.unc.lib.boxc.integration.factories.FolderFactory;
import edu.unc.lib.boxc.integration.factories.WorkFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

/**
 * @author snluong
 */
@ContextHierarchy({
        @ContextConfiguration("/spring-test/test-fedora-container.xml"),
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/acl-service-context.xml"),
        @ContextConfiguration("/spring-test/solr-standalone-context.xml"),
        @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
        @ContextConfiguration("/spring-test/object-factory-context.xml")
})
@RunWith(SpringJUnit4ClassRunner.class)
public class SearchEndpointIT extends EndpointIT {
    @Autowired
    protected String baseAddress;
    @Autowired
    protected RepositoryInitializer repoInitializer;
    @Autowired
    protected SolrClient solrClient;

    protected final static String USERNAME = "test_user";
    protected final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    private CloseableHttpClient httpClient;
    private HttpGet getMethod;

    @Before
    public void setup() throws Exception {
        TestHelper.setContentBase(baseAddress);
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
        // reset solr before every test
        solrClient.deleteByQuery("*:*");
        httpClient = HttpClients.createDefault();
        getMethod = new HttpGet("http://localhost:48080/access/searchJson");
        contentRootObjectFactory.initializeRepository();
    }

    @Test
    public void testBlankSearch() throws Exception {
        createDefaultObjects();
        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            System.out.println(metadata);
            SearchEndpointTestUtility.assertSuccessfulResponse(resp);
        }
    }
}
