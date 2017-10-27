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
package edu.unc.lib.dl.search.solr.util;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public enum SearchFieldKeys {
    ABSTRACT("abstract"),
    ADMIN_GROUP("adminGroup"),
    ANCESTOR_IDS("ancestorIds"),
    ANCESTOR_PATH("ancestorPath"),
    CITATION("citation"),
    CONTENT_MODEL("contentModel"),
    CONTENT_STATUS("contentStatus"),
    CONTENT_TYPE("contentType"),
    CONTRIBUTOR("contributor"),
    CONTRIBUTOR_INDEX("contributorIndex"),
    CREATOR("creator"),
    CREATOR_SORT("creatorSort"),
    DATASTREAM("datastream"),
    DATE_ADDED("dateAdded"),
    DATE_CREATED("dateCreated"),
    DATE_UPDATED("dateUpdated"),
    DEFAULT_INDEX("text"),
    DEPARTMENT("department"),
    DEPARTMENT_LC("department_lc"),
    DISPLAY_ORDER("displayOrder"),
    FILESIZE("filesizeSort"),
    FILESIZE_TOTAL("filesizeTotal"),
    FULL_TEXT("fullText"),
    ID("id"),
    IDENTIFIER("identifier"),
    IDENTIFIER_SORT("identifierSort"),
    IS_PART("isPart"),
    KEYWORD("keyword"),
    LABEL("label"),
    LANGUAGE("language"),
    LAST_INDEXED("lastIndexed"),
    OTHER_TITLES("otherTitle"),
    PARENT_COLLECTION("parentCollection"),
    PARENT_UNIT("parentUnit"),
    READ_GROUP("readGroup"),
    RELATIONS("relations"),
    RESOURCE_TYPE("resourceType"),
    RESOURCE_TYPE_SORT("resourceTypeSort"),
    ROLE_GROUP("roleGroup"),
    ROLLUP_ID("rollup"),
    STATUS("status"),
    STATUS_TAGS("statustags"),
    SUBJECT("subject"),
    SUBJECT_INDEX("subjectIndex"),
    TIMESTAMP("timestamp"),
    TITLE("title"),
    TITLE_INDEX("titleIndex"),
    TITLE_LC("title_lc"),
    VERSION("_version_");

    private final String solrField;

    private SearchFieldKeys(String solrField) {
        this.solrField = solrField;
    }

    /**
     *
     * @return the solr field
     */
    public String getSolrField() {
        return solrField;
    }
}
