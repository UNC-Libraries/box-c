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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static edu.unc.lib.boxc.integration.factories.FileFactory.FILE_FORMAT_OPTION;
import static edu.unc.lib.boxc.integration.factories.FileFactory.IMAGE_FORMAT;
import static org.junit.Assert.*;

/**
 * Integration tests for searchJson endpoints
 * @author snluong, krwong, bbpennel
 */

@RunWith(SpringJUnit4ClassRunner.class)
public class SearchEndpointIT extends EndpointIT {
    protected final static String SEARCH_URL = ACCESS_URL + "/searchJson";
    protected final static List<String> DEFAULT_FACETS = Arrays.asList("PARENT_UNIT", "PARENT_COLLECTION",
            "FILE_FORMAT_CATEGORY", "GENRE", "SUBJECT", "LANGUAGE", "LOCATION", "PUBLISHER",
            "DATE_CREATED_YEAR", "CREATOR_CONTRIBUTOR");

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
                FILE_FORMAT_OPTION, FileFactory.TEXT_FORMAT,
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
                FILE_FORMAT_OPTION, FileFactory.TEXT_FORMAT,
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
            assertIdMatchesNone(metadata, collectionId);
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
            assertIdMatchesAny(metadata, collectionId);
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

    @Test
    public void testDateDepositedInclusiveSearch() throws Exception {
        createDefaultObjects();
        var currentYear = Year.now().getValue();
        assertResultCountEquals(SEARCH_URL + "/?added=" + currentYear + ",", 5);
        assertResultCountEquals(SEARCH_URL + "/?added=" + currentYear + "," + currentYear, 5);
        assertResultCountEquals(SEARCH_URL + "/?added=," + currentYear, 5);
        assertResultCountEquals(SEARCH_URL + "/?added=" + (currentYear + 1) + ",", 0);
    }

    @Test
    public void testSearchWithCreatedDateRangeSpecified() throws Exception {
        createDefaultObjects();
        var ids = createDatedObjects();
        var datedCollectionId = ids.get(1);

        var getMethod = new HttpGet(SEARCH_URL + "/?createdYear=2022,2022");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            // should only find the collection, not the admin unit that has a created date
            assertEquals(1, metadata.size());
            assertValuePresent(metadata, 0, "title", "A dated collection");
            assertIdMatchesAny(metadata, datedCollectionId);
        }
    }

    @Test
    public void testSearchWithCreatedDateRangeSpecifiedAsUnknown() throws Exception {
        createDefaultObjects();
        var ids = createDatedObjects();
        var datedAdminUnitId = ids.get(0);
        var datedCollectionId = ids.get(1);

        var getMethod = new HttpGet(SEARCH_URL + "/?createdYear=unknown");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            // should only find the five default objects, not the dated admin unit or dated collection
            assertEquals(5, metadata.size());
            assertIdMatchesNone(metadata, datedAdminUnitId);
            assertIdMatchesNone(metadata, datedCollectionId);
        }
    }

    @Test
    public void testMultiImageWorkHasFirstImageAsThumbnail() throws Exception {
        createDefaultObjects();
        var mWork = workFactory.createWork(collection, Map.of("title", "Multifile work"));
        workFactory.createFileInWork(mWork, Map.of("title", "File C", FILE_FORMAT_OPTION, IMAGE_FORMAT));
        workFactory.createFileInWork(mWork, Map.of("title", "File B", FILE_FORMAT_OPTION, IMAGE_FORMAT));
        var firstFile = workFactory.createFileInWork(mWork, Map.of("title", "File A", FILE_FORMAT_OPTION, IMAGE_FORMAT));
        workFactory.createFileInWork(mWork, Map.of("title", "File D", FILE_FORMAT_OPTION, IMAGE_FORMAT));

        var getMethod = new HttpGet(SEARCH_URL + "/?id=" + mWork.getPid().getId());

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            assertSuccessfulResponse(resp);
            assertEquals(1, metadata.size());

            var workRecord = metadata.get(0);
            var thumbnailUrl = workRecord.get("thumbnail_url").asText();
            assertEquals("http://localhost:48080/services/api/thumb/" + firstFile.getPid().getId() + "/large",
                    thumbnailUrl);
        }
    }

    @Test
    public void testSearchWithOneFacetFilter() throws Exception {
        createDefaultObjects();
        createLanguageSubjectObjects();

        var getMethod = new HttpGet(SEARCH_URL + "/?facetSelect=language&language=English&getFacets=true");

        try (var resp = httpClient.execute(getMethod)) {
            var facetFields = getFacetsFromResponse(resp);

            assertSuccessfulResponse(resp);
            // should only find the language facet
            assertEquals(1, facetFields.size());
        }
    }

    @Test
    public void testSearchWithLanguageFilterSelectedDisplaysAllAvailableLanguages() throws Exception {
        createDefaultObjects();
        createLanguageSubjectObjects();

        var getMethod = new HttpGet(SEARCH_URL + "/?facetSelect=language&language=English&getFacets=true");

        try (var resp = httpClient.execute(getMethod)) {
            var facetFields = getFacetsFromResponse(resp);
            var languageFields = new ArrayList<JsonNode>();
            facetFields.get(0).get("values").elements().forEachRemaining(languageFields::add);

            assertSuccessfulResponse(resp);
            // should find all available languages: English, Cherokee
            assertValuePresent(languageFields, 0, "value", "English");
            assertValuePresent(languageFields, 1, "value", "Cherokee");
        }
    }

    @Test
    public void testSearchWithSubjectFilterSelectedDisplaysAvailableLanguages() throws Exception {
        createDefaultObjects();
        createLanguageSubjectObjects();

        var getMethod = new HttpGet(SEARCH_URL + "/?facetSelect=language%2Csubject&subject=North%2520Carolina&getFacets=true");

        try (var resp = httpClient.execute(getMethod)) {
            var facetFields = getFacetsFromResponse(resp);
            var languageFields = new ArrayList<JsonNode>();
            facetFields.get(0).get("values").elements().forEachRemaining(languageFields::add);
            var subjectFields = new ArrayList<JsonNode>();
            facetFields.get(1).get("values").elements().forEachRemaining(subjectFields::add);

            assertSuccessfulResponse(resp);
            // should find all available languages (English) and subjects (North Carolina)
            assertEquals(2,facetFields.size());
            assertEquals(1,languageFields.size());
            assertValuePresent(languageFields, 0, "value", "English");
            assertValuePresent(subjectFields, 0, "value", "North Carolina");
        }
    }

    @Test
    public void testGetFacetsFalseSearchReturnsNoFacets() throws Exception {
        createDefaultObjects();
        createLanguageSubjectObjects();

        var getMethod = new HttpGet(SEARCH_URL + "/?getFacets=false");

        try (var resp = httpClient.execute(getMethod)) {
            var respJson = getResponseAsJson(resp);

            assertSuccessfulResponse(resp);
            assertFalse(respJson.has("facetFields"));
        }
    }

    @Test
    public void testGetFacetsTrueSearchReturnsDefaultFacets() throws Exception {
        createDefaultObjects();
        var getMethod = new HttpGet(SEARCH_URL + "/?getFacets=true");

        try (var resp = httpClient.execute(getMethod)) {
            var facetFields = getFacetsFromResponse(resp);
            var facetNames = new ArrayList<String>();

            for (JsonNode facet : facetFields) {
                facetNames.add(facet.get("name").asText());
            }

            assertSuccessfulResponse(resp);
            assertEquals(10, facetFields.size());
            assertEquals(DEFAULT_FACETS, facetNames);
        }
    }

    @Test
    public void testSearchWithFacetSelectOnlyShowsThoseFacets() throws Exception {
        createDefaultObjects();
        var getMethod = new HttpGet(SEARCH_URL + "/?facetSelect=language%2Csubject&getFacets=true");

        try (var resp = httpClient.execute(getMethod)) {
            var facetFields = getFacetsFromResponse(resp);

            assertSuccessfulResponse(resp);
            assertEquals(2, facetFields.size());
            assertEquals("LANGUAGE", facetFields.get(0).get("name").asText());
            assertEquals("SUBJECT", facetFields.get(1).get("name").asText());
        }
    }

    @Test
    public void testSearchWithFacetSelectCannotShowDisallowedFacets() throws Exception {
        createDefaultObjects();
        var getMethod = new HttpGet(SEARCH_URL + "/?facetSelect=language,subject,contentStatus&getFacets=true");

        try (var resp = httpClient.execute(getMethod)) {
            var facetFields = getFacetsFromResponse(resp);

            assertSuccessfulResponse(resp);
            assertEquals(2, facetFields.size());
            assertEquals("LANGUAGE", facetFields.get(0).get("name").asText());
            assertEquals("SUBJECT", facetFields.get(1).get("name").asText());
        }
    }

    @Test
    public void testFacetCountsReflectUserPermissions() throws Exception {
        createDefaultObjects();
        createLanguageSubjectObjects();
        // create admin-restricted collections in different languages
        collectionFactory.createCollection(adminUnit1,
                Map.of("title", "Collection Object", "adminGroup", "adminGroup", "languageTerm", "kor"));
        collectionFactory.createCollection(adminUnit1,
                Map.of("title", "Collection Object", "adminGroup", "adminGroup", "languageTerm", "por"));

        var getMethod = new HttpGet(SEARCH_URL + "/?facetSelect=language&getFacets=true");

        try (var resp = httpClient.execute(getMethod)) {
            var facetFields = getFacetsFromResponse(resp);
            var languageFields = new ArrayList<JsonNode>();
            facetFields.get(0).get("values").elements().forEachRemaining(languageFields::add);

            assertSuccessfulResponse(resp);
            assertEquals(2, languageFields.size());
            // should display only languages associated with public objects: English, Cherokee
            assertValuePresent(languageFields, 0, "value", "English");
            assertValuePresent(languageFields, 1, "value", "Cherokee");
        }
    }

    @Test
    public void testSearchAllExpectedFieldsForRecordReturned() throws Exception {
        createWorkAndFileObjects();

        var getMethod = new HttpGet(SEARCH_URL + "/?types=Work,File&rollup=false");

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            assertSuccessfulResponse(resp);

            assertStringValuePresent(metadata, 1, "thumbnail_url");
            assertStringValuePresent(metadata, 1, "id");
            assertStringValuePresent(metadata, 1, "title");
            assertStringValuePresent(metadata, 1, "_version_");
            assertArrayValuePresent(metadata, 1, "status");
            assertArrayValuePresent(metadata, 1, "contentStatus");
            assertArrayValuePresent(metadata, 1, "subject");
            assertStringValuePresent(metadata, 1, "type");
            assertArrayValuePresent(metadata, 0, "creator");
            assertArrayValuePresent(metadata, 1, "datastream");
            assertArrayValuePresent(metadata, 1, "format");
            assertArrayValuePresent(metadata, 1, "fileDesc");
            assertArrayValuePresent(metadata, 1, "fileType");
            assertArrayValuePresent(metadata, 1, "identifier");
            assertArrayValuePresent(metadata, 1, "ancestorPath");
            assertArrayValuePresent(metadata, 1, "objectPath");
            assertStringValuePresent(metadata, 1, "rollup");
            assertArrayValuePresent(metadata, 0, "counts"); //counts in work and not file
            assertStringValuePresent(metadata, 1, "added");
            assertStringValuePresent(metadata, 1, "updated");
            assertStringValuePresent(metadata, 1, "created");
            assertStringValuePresent(metadata, 1, "timestamp");
            assertArrayValuePresent(metadata, 1, "permissions");
        }
    }

    public List<String> createDatedObjects() throws Exception {
        List<String> ids = new ArrayList<>();
        var datedAdminUnit = adminUnitFactory.createAdminUnit(
                Map.of("title", "Dated Admin Object", "dateCreated", "2018-07-01"));
        var datedCollection = collectionFactory.createCollection(datedAdminUnit,
                Map.of("title", "A dated collection",
                        "dateCreated", "2022-07-01",
                        "readGroup", "everyone"));
        ids.add(datedAdminUnit.getPid().getId());
        ids.add(datedCollection.getPid().getId());
        return ids;
    }

    public void createLanguageSubjectObjects() throws Exception {
        var languageAdminUnit = adminUnitFactory.createAdminUnit(
                Map.of("title", "Admin Object", "languageTerm", "eng"));
        collectionFactory.createCollection(languageAdminUnit,
                Map.of("title", "A language collection",
                        "languageTerm", "eng",
                        "readGroup", "everyone"));
        folderFactory.createFolder(collection,
                Map.of("title","A language folder","languageTerm", "eng",
                        "topic", "North Carolina","readGroup", "everyone"));
        adminUnitFactory.createAdminUnit(Map.of(
                "title", "English Language Admin", "languageTerm", "eng",
                "topic", "North Carolina", "readGroup", "everyone"));
        adminUnitFactory.createAdminUnit(Map.of(
                "title", "Cherokee Language Admin", "languageTerm", "chr",
                "topic", "UNC", "readGroup", "everyone"));
    }

    public void createWorkAndFileObjects() throws Exception {
        adminUnit1 = adminUnitFactory.createAdminUnit(Map.of("title", "Admin Object1"));
        collection = collectionFactory.createCollection(adminUnit1,
                Map.of("title", "Collection Object", "readGroup", "everyone"));
        work = workFactory.createWork(collection,
                Map.of("title", "Work Record", "dateCreated", "2022-07-01",
                        "topic", "North Carolina", "identifier", "abc123",
                        "creator", "Creator?", "readGroup", "everyone"));
        var fileOptions = Map.of(
                "title", "File Record", "dateCreated", "2022-07-01",
                "topic", "North Carolina", "identifier", "abc123",
                WorkFactory.PRIMARY_OBJECT_KEY, "false",
                FileFactory.FILE_FORMAT_OPTION, IMAGE_FORMAT,
                "readGroup", "everyone");
        workFactory.createFileInWork(work, fileOptions);
    }
}
