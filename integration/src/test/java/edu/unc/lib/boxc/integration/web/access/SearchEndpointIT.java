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

import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.integration.factories.FileFactory;
import edu.unc.lib.boxc.integration.factories.WorkFactory;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for searchJson endpoints
 * @author snluong
 */

@RunWith(SpringJUnit4ClassRunner.class)
public class SearchEndpointIT extends EndpointIT {
    protected final static String SEARCH_URL = ACCESS_URL + "/searchJson";

    @Before
    public void setup() throws Exception {
        TestHelper.setContentBase(baseAddress);
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
        // reset solr before every test
        solrClient.deleteByQuery("*:*");
        httpClient = HttpClients.createDefault();
        contentRootObjectFactory.initializeRepository();
    }

    @Test
    public void testBlankSearchReturnsRightNumberOfObjects() throws Exception {
        createDefaultObjects();
        var getMethod = new HttpGet(SEARCH_URL);

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            assertSuccessfulResponse(resp);
            // two admin units, 1 collection (nested in the admin unit), 1 work (with nested file), and 1 folder
            assertEquals(5, metadata.size());
        }
    }

    @Test
    public void testSearchTermInTitle() throws Exception {
        createDefaultObjects();
        collectionFactory.createCollection(adminUnit1,
                Map.of("title", "Text Collection", "readGroup", "everyone"));
        // make another file in the default work that is a text file with "text"
        var fileOptions = Map.of(
                "title", "Object" + System.nanoTime(),
                WorkFactory.PRIMARY_OBJECT_KEY, "false",
                FileFactory.FILE_FORMAT_OPTION, FileFactory.TEXT_FORMAT,
                "readGroup", "everyone");
        workFactory.createFileInWork(work, fileOptions);

        var getMethod = new HttpGet(SEARCH_URL + "/?titleIndex=text");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            assertSuccessfulResponse(resp);
            // find the one collection with "Text" in the title, but not the work with the text file
            assertEquals(1, metadata.size());
            assertValuePresent(metadata, 0, "type", "Collection");
            assertValuePresent(metadata, 0, "title", "Text Collection");
        }
    }

    @Test
    public void testSearchTermInAnyField() throws Exception {
        createDefaultObjects();
        collectionFactory.createCollection(adminUnit1,
                Map.of("title", "Through the lens", "readGroup", "everyone"));
        var textWork = workFactory.createWork(collection,
                Map.of("title", "Work with text file", "readGroup", "everyone"));
        // make another file in the default work that is a text file with the word "Through"
        var fileOptions = Map.of(
                "title", "Object" + System.nanoTime(),
                WorkFactory.PRIMARY_OBJECT_KEY, "false",
                FileFactory.FILE_FORMAT_OPTION, FileFactory.TEXT_FORMAT,
                "readGroup", "everyone");
        workFactory.createFileInWork(textWork, fileOptions);

        var getMethod = new HttpGet(SEARCH_URL + "/?anywhere=through");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            // find the two items
            assertEquals(2, metadata.size());
            // check for the collection
            assertValuePresent(metadata, 0, "type", "Collection");
            assertValuePresent(metadata, 0, "title", "Through the lens");
            // check for the work that has the file
            assertValuePresent(metadata, 1, "type", "Work");
            assertValuePresent(metadata, 1, "title", "Work with text file");
        }
    }

    @Test
    public void testSearchTypeParamWithOneType() throws Exception {
        createDefaultObjects();

        var getMethod = new HttpGet(SEARCH_URL + "/?types=Work");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            // find the one work
            assertEquals(1, metadata.size());
            // check for the work
            assertValuePresent(metadata, 0, "type", "Work");
        }
    }
    
    @Test
    public void testSearchTypeParamWithFileSpecified() throws Exception {
        createDefaultObjects();

        // rollup=false disables the FileObjects getting grouped into their associated Works
        var getMethod = new HttpGet(SEARCH_URL + "/?types=Work,File&rollup=false");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            // find the two items
            assertEquals(2, metadata.size());
            // check for the work
            assertValuePresent(metadata, 0, "type", "Work");
            // check for the file
            assertValuePresent(metadata, 1, "type", "File");
        }
    }

    @Test
    public void testSearchWithNormalSorting() throws Exception {
        createDefaultObjects();
        collectionFactory.createCollection(adminUnit1,
                Map.of("title", "A first collection", "readGroup", "everyone"));
        
        var getMethod = new HttpGet(SEARCH_URL + "/?sort=title,normal");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            // make sure all items return
            assertEquals(6, metadata.size());
            // objects should be in A-Z order by title
            assertValuePresent(metadata, 0, "title", "A first collection");
            assertValuePresent(metadata, 1, "title", "Admin Object1");
            assertValuePresent(metadata, 2, "title", "Admin Object2");
            assertValuePresent(metadata, 3, "title", "Collection Object");
            assertValuePresent(metadata, 4, "title", "Folder Object");
            assertValuePresent(metadata, 5, "title", "Work Object");
        }
    }

    @Test
    public void testSearchWithReversedSorting() throws Exception {
        createDefaultObjects();
        collectionFactory.createCollection(adminUnit1,
                Map.of("title", "A first collection", "readGroup", "everyone"));
        
        var getMethod = new HttpGet(SEARCH_URL + "/?sort=title,reverse");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            // make sure all items return
            assertEquals(6, metadata.size());
            // objects should be in Z-A order by title
            assertValuePresent(metadata, 5, "title", "A first collection");
            assertValuePresent(metadata, 4, "title", "Admin Object1");
            assertValuePresent(metadata, 3, "title", "Admin Object2");
            assertValuePresent(metadata, 2, "title", "Collection Object");
            assertValuePresent(metadata, 1, "title", "Folder Object");
            assertValuePresent(metadata, 0, "title", "Work Object");
        }
    }

    @Test
    public void testSearchWithALowerPageSize() throws Exception {
        createDefaultObjects();
        collectionFactory.createCollection(adminUnit1,
                Map.of("title", "A first collection", "readGroup", "everyone"));
        folderFactory.createFolder(collection,
                Map.of("title", "Folder Object 2", "readGroup", "everyone"));

        var getMethod = new HttpGet(SEARCH_URL + "/?start=0&rows=5");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            // there are seven objects but this search should only return 5 because of page size
            assertEquals(5, metadata.size());
        }
    }

    @Test
    public void testSearchWithADifferentStartIndex() throws Exception {
        createDefaultObjects();
        collectionFactory.createCollection(adminUnit1,
                Map.of("title", "A first collection", "readGroup", "everyone"));

        var getMethod = new HttpGet(SEARCH_URL + "/?start=3");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            // there are 6 total items, but since the index starts at 3 we should have only 3 results
            assertEquals(3, metadata.size());
        }
    }

    @Test
    public void testSearchPublicUserCannotSeeStaffOnlyObjects() throws Exception {
        createDefaultObjects();
        var staffOnlyCollection = collectionFactory.createCollection(adminUnit1,
                Map.of("title", "A first collection", "adminGroup", "adminGroup"));

        var getMethod = new HttpGet(SEARCH_URL);

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            var collectionId = staffOnlyCollection.getPid().getId();
            assertSuccessfulResponse(resp);
            // two admin units, 1 collection (nested in the admin unit), 1 work (with nested file), and 1 folder
            assertEquals(5, metadata.size());
            assertTrue(metadata.stream().noneMatch(entry -> collectionId.equals(entry.get("id"))));
        }
    }

    @Test
    public void testSearchStaffUserCanSeeStaffOnlyObjects() throws Exception {
        createDefaultObjects();
        var staffOnlyCollection = collectionFactory.createCollection(adminUnit1,
                Map.of("title", "A first collection", "adminGroup", "adminGroup"));

        var getMethod = new HttpGet(SEARCH_URL);
        getMethod.setHeader("isMemberof","adminGroup");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            var collectionId = staffOnlyCollection.getPid().getId();
            assertSuccessfulResponse(resp);
            assertEquals(6, metadata.size());
            System.out.println(metadata);
            assertTrue(metadata.stream().anyMatch(entry -> collectionId.equals(entry.get("id"))));
        }
    }

    @Test
    public void testSearchPublicUserWithMetadataOnlyPermissionCanSeeMetadata() throws Exception {
        createDefaultObjects();
        collectionFactory.createCollection(adminUnit1,
                Map.of("title", "A first collection", "metadataGroup", "everyone"));

        var getMethod = new HttpGet(SEARCH_URL);

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            var collectionMetadata = metadata.get(2);
            assertSuccessfulResponse(resp);
            // check "permissions" field in affected result only lists "viewMetadata"
            assertEquals("viewMetadata", collectionMetadata.get("permissions").get(0).asText());
            assertEquals(6, metadata.size());
        }
    }
}
