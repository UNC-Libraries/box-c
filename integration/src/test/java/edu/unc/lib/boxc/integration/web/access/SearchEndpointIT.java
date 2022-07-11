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

import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for searchJson endpoints
 * @author snluong
 */

@RunWith(SpringJUnit4ClassRunner.class)
public class SearchEndpointIT extends EndpointIT {
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
    public void testBlankSearchReturnsRightNumberOfObjects() throws Exception {
        createDefaultObjects();

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            assertSuccessfulResponse(resp);
            // two admin units, 1 collection (nested in the admin unit), 1 work (with nested file), and 1 folder
            assertEquals(5, metadata.size());
        }
    }
}
