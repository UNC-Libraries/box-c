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
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.integration.factories.AdminUnitFactory;
import edu.unc.lib.boxc.integration.factories.CollectionFactory;
import edu.unc.lib.boxc.integration.factories.ContentRootObjectFactory;
import edu.unc.lib.boxc.integration.factories.FileFactory;
import edu.unc.lib.boxc.integration.factories.FolderFactory;
import edu.unc.lib.boxc.integration.factories.WorkFactory;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A parent class for SearchActionController Endpoint tests.
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
public class EndpointIT {
    @Autowired
    protected AdminUnitFactory adminUnitFactory;
    @Autowired
    protected WorkFactory workFactory;
    @Autowired
    protected CollectionFactory collectionFactory;
    @Autowired
    protected FolderFactory folderFactory;
    @Autowired
    protected ContentRootObjectFactory contentRootObjectFactory;
    @Autowired
    protected String baseAddress;
    @Autowired
    protected RepositoryInitializer repoInitializer;
    @Autowired
    protected SolrClient solrClient;

    protected final static String USERNAME = "test_user";
    protected final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    protected final static String ACCESS_URL = "http://localhost:48080/access";

    protected CloseableHttpClient httpClient;
    protected CollectionObject collection;
    protected WorkObject work;
    protected AdminUnit adminUnit1;

    public void createDefaultObjects() throws Exception {
        adminUnit1 = adminUnitFactory.createAdminUnit(Map.of("title", "Admin Object1"));
        var adminUnit2 = adminUnitFactory.createAdminUnit(Map.of("title", "Admin Object2"));
        collection = collectionFactory.createCollection(adminUnit1,
                Map.of("title", "Collection Object", "readGroup", "everyone"));
        work = workFactory.createWork(collection,
                Map.of("title", "Work Object", "readGroup", "everyone"));
        var fileOptions = Map.of(
                "title", "File Object",
                WorkFactory.PRIMARY_OBJECT_KEY, "false",
                FileFactory.FILE_FORMAT_OPTION, FileFactory.AUDIO_FORMAT,
                "readGroup", "everyone");
        workFactory.createFileInWork(work, fileOptions);
        folderFactory.createFolder(collection,
                Map.of("title", "Folder Object", "readGroup", "everyone"));
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

    public List<JsonNode> getNodeFromResponse(CloseableHttpResponse response, String fieldName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        var respJson = mapper.readTree(response.getEntity().getContent());

        return IteratorUtils.toList(respJson.get(fieldName).elements());
    }

    public void assertValuePresent(List<JsonNode> json, int index, String key, String value) {
        var result = json.get(index);
        assertEquals(value, result.get(key).asText());
    }

    public void assertValuePresent(List<JsonNode> json, int index, String key) {
        var result = json.get(index);
        assertNotNull(result.get(key).asText());
    }

    public void assertSuccessfulResponse(CloseableHttpResponse response) {
        assertEquals(200, response.getStatusLine().getStatusCode());
    }

    public void assertIdMatchesAny(List<JsonNode> json, String id) {
        assertTrue(json.stream().anyMatch(entry -> id.equals(entry.get("id").asText())));
    }

    public void assertIdMatchesNone(List<JsonNode> json, String id) {
        assertTrue(json.stream().noneMatch(entry -> id.equals(entry.get("id").asText())));
    }
}
