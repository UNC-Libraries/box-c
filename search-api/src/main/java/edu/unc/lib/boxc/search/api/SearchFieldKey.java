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
    ALT_TEXT("altText", "altText", "Alt Text"),
    ANCESTOR_IDS("ancestorIds", "ancestorIds", "Ancestor Ids"),
    ANCESTOR_PATH("ancestorPath", "path", "Folders"),
    CITATION("citation", "citation", "Citation"),
    COLLECTION_ID("collectionId", "collectionId", "Archival Collection ID"),
    CONTENT_STATUS("contentStatus", "contentStatus", "Content Status"),
    CONTRIBUTOR("contributor", "contributor", "Contributor"),
    CONTRIBUTOR_INDEX("contributorIndex", "contributorIndex", "Creator/Contributor"),
    CREATOR("creator", "creator", "Creator"),
    CREATOR_CONTRIBUTOR("creatorContributor", "creatorContributor", "Creator/Contributor"),
    DATASTREAM("datastream", "datastream", "Datastream"),
    DATE_ADDED("dateAdded", "added", "Date Deposited"),
    DATE_CREATED("dateCreated", "created", "Date Created"),
    DATE_CREATED_YEAR("dateCreatedYear", "createdYear", "Date Created Year"),
    DATE_UPDATED("dateUpdated", "updated", "Last Updated"),
    DEFAULT_INDEX("text", "anywhere", "Anywhere"),
    FILE_FORMAT_CATEGORY("fileFormatCategory", "format", "Format"),
    FILE_FORMAT_TYPE("fileFormatType", "fileType", "File Type"),
    FILE_FORMAT_DESCRIPTION("fileFormatDescription", "fileDesc", "File Type Description"),
    FILESIZE("filesizeSort", "filesize", "Filesize"),
    FILESIZE_TOTAL("filesizeTotal", "filesizeTotal", "Filesize Total"),
    FULL_TEXT("fullText", "fullText", "Full Text"),
    EXHIBIT("exhibit", "exhibit", "Exhibit"),
    GENRE("genre", "genre", "Genre"),
    ID("id", "id", "ID"),
    IDENTIFIER("identifier", "identifier", "Identifier"),
    IDENTIFIER_SORT("identifierSort", "identifierSort", "Identifier Sort"),
    KEYWORD("keyword", "keyword", "Keyword"),
    LANGUAGE("language", "language", "Language"),
    LAST_INDEXED("lastIndexed", "lastIndexed", "lastIndexed"),
    LOCATION("location", "location", "Location"),
    MEMBER_ORDER_ID("memberOrderId", "memberOrderId", "Member Order ID"),
    OTHER_SUBJECT("otherSubject", "otherSubject", "Other Subject"),
    OTHER_TITLE("otherTitle", "otherTitle", "otherTitle"),
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
    STREAMING_TYPE("streamingType", "streamingType", "Streaming Type"),
    STREAMING_URL("streamingUrl", "streamingUrl", "Streaming URL"),
    SUBJECT("subject", "subject", "Subject"),
    SUBJECT_INDEX("subjectIndex", "subjectIndex", "Subject Index"),
    TIMESTAMP("timestamp", "timestamp", "Timestamp"),
    TITLE("title", "title", "Title"),
    TITLE_INDEX("titleIndex", "titleIndex", "Title Index"),
    VERSION("_version_", "version", "Version"),
    VIEW_BEHAVIOR("viewBehavior", "viewBehavior", "View Behavior");

    private final String solrField;
    private final String displayLabel;
    private final String urlParam;

    private static Map<String, SearchFieldKey> nameToKey = Arrays.stream(SearchFieldKey.values())
            .collect(Collectors.toMap(SearchFieldKey::getSolrField, Function.identity()));

    private static Map<String, SearchFieldKey> urlParamToKey = Arrays.stream(SearchFieldKey.values())
            .collect(Collectors.toMap(SearchFieldKey::getUrlParam, Function.identity()));

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

    /**
     * @param urlParam
     * @return the SearchFieldKey which has a urlParam matching the provided value
     */
    public static SearchFieldKey getByUrlParam(String urlParam) {
        return urlParamToKey.get(urlParam);
    }
}
