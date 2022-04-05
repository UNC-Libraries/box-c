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
package edu.unc.lib.boxc.search.api;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public enum SearchFieldKey {
    ABSTRACT("abstract", "abstract", "Abstract"),
    ADMIN_GROUP("adminGroup", "adminGroup", "Admin Group"),
    ANCESTOR_IDS("ancestorIds", "ancestorIds", "Ancestor Ids"),
    ANCESTOR_PATH("ancestorPath", "path", "Folders"),
    CITATION("citation", "citation", "Citation"),
    COLLECTION_ID("collectionId", "collectionId", "Archival Collection ID"),
    CONTENT_STATUS("contentStatus", "contentStatus", "Content Status"),
    CONTENT_TYPE("contentType", "format", "Format"),
    CONTRIBUTOR("contributor", "contributor", "Contributor"),
    CONTRIBUTOR_INDEX("contributorIndex", "contributorIndex", "Creator/Contributor"),
    CREATOR("creator", "creator", "Creator"),
    CREATOR_CONTRIBUTOR("creatorContributor", "creatorContributor", "Creator/Contributor"),
    CREATOR_SORT("creatorSort", "creatorSort", "Creator Sort"),
    DATASTREAM("datastream", "datastream", "Datastream"),
    DATE_ADDED("dateAdded", "added", "Date Deposited"),
    DATE_CREATED("dateCreated", "created", "Date Created"),
    DATE_CREATED_YEAR("dateCreatedYear", "createdYear", "Date Created Year"),
    DATE_UPDATED("dateUpdated", "updated", "Last Updated"),
    DEFAULT_INDEX("text", "anywhere", "Anywhere"),
    FILESIZE("filesizeSort", "filesize", "Filesize"),
    FILESIZE_TOTAL("filesizeTotal", "filesizeTotal", "Filesize Total"),
    FULL_TEXT("fullText", "fullText", "Full Text"),
    GENRE("genre", "genre", "Genre"),
    ID("id", "id", "ID"),
    IDENTIFIER("identifier", "identifier", "Identifier"),
    IDENTIFIER_SORT("identifierSort", "identifierSort", "Identifier Sort"),
    KEYWORD("keyword", "keyword", "Keyword"),
    LANGUAGE("language", "language", "Language"),
    LAST_INDEXED("lastIndexed", "lastIndexed", "lastIndexed"),
    LOCATION("location", "location", "Location"),
    OTHER_TITLES("otherTitle", "otherTitle", "otherTitle"),
    PARENT_COLLECTION("parentCollection", "collection", "Collection"),
    PARENT_UNIT("parentUnit", "unit", "Admin Unit"),
    PUBLISHER("publisher", "publisher", "Publisher"),
    READ_GROUP("readGroup", "readGroup", "Read Group"),
    RESOURCE_TYPE("resourceType", "type", "Resource Type"),
    RESOURCE_TYPE_SORT("resourceTypeSort", "typeSort", "Resource Type Sort"),
    RIGHTS("rights", "rights", "Rights"),
    RIGHTS_OAI_PMH("rightsOaiPmh", "rightsOaiPmh", "Rights OAI-PMH"),
    RIGHTS_URI("rightsUri", "rightsUri", "Rights URI"),
    RLA_SITE_CODE("rla_site_code_d", "rla.siteCode", ""),
    RLA_CATALOG_NUMBER("rla_catalog_number_d", "rla.catalogNumber", ""),
    RLA_CONTEXT_1("rla_context_1_d", "rla.context1", ""),
    ROLE_GROUP("roleGroup", "role", "Roles"),
    ROLLUP_ID("rollup", "rollupId", "Rollup ID"),
    SCORE("score", "score", "Score"),
    STATUS("status", "status", "Access Status"),
    SUBJECT("subject", "subject", "Subject"),
    SUBJECT_INDEX("subjectIndex", "subjectIndex", "Subject Index"),
    TIMESTAMP("timestamp", "timestamp", "Timestamp"),
    TITLE("title", "title", "Title"),
    TITLE_INDEX("titleIndex", "titleIndex", "Title Index"),
    TITLE_LC("title_lc", "title_lc", "Title"),
    VERSION("_version_", "version", "Version");

    private final String solrField;
    private final String displayLabel;
    private final String urlParam;

    private static Map<String, SearchFieldKey> nameToKey = Arrays.stream(SearchFieldKey.values())
            .collect(Collectors.toMap(SearchFieldKey::getSolrField, Function.identity()));

    private SearchFieldKey(String solrField, String urlParam, String displayLabel) {
        this.solrField = solrField;
        this.urlParam = urlParam;
        this.displayLabel = displayLabel;
    }

    /**
     *
     * @return the solr field
     */
    public String getSolrField() {
        return solrField;
    }

    /**
     * @return Display label for this field
     */
    public String getDisplayLabel() {
        return displayLabel;
    }

    /**
     * @return parameter name of this field in search URLs
     */
    public String getUrlParam() {
        return urlParam;
    }

    /**
     * @param name
     * @return the SearchFieldKey which has a solr field name matching the provided value
     */
    public static SearchFieldKey getByName(String name) {
        return nameToKey.get(name);
    }
}
