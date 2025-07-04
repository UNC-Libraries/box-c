package edu.unc.lib.boxc.search.solr.models;

import static edu.unc.lib.boxc.search.api.SearchFieldKey.ABSTRACT;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.ADMIN_GROUP;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.ANCESTOR_IDS;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.ANCESTOR_PATH;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.ASPACE_REF_ID;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.CITATION;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.COLLECTION_ID;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.CONTENT_STATUS;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.CONTRIBUTOR;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.CREATOR;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.CREATOR_CONTRIBUTOR;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.DATASTREAM;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.DATE_ADDED;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.DATE_CREATED;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.DATE_CREATED_YEAR;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.DATE_UPDATED;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.EXHIBIT;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.FILESIZE;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.FILESIZE_TOTAL;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.FULL_TEXT;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.HOOK_ID;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.ID;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.IDENTIFIER;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.IDENTIFIER_SORT;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.KEYWORD;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.LANGUAGE;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.LAST_INDEXED;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.LOCATION;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.OTHER_SUBJECT;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.OTHER_TITLE;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.PARENT_COLLECTION;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.PARENT_UNIT;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.PUBLISHER;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.READ_GROUP;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.RESOURCE_TYPE;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.RESOURCE_TYPE_SORT;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.RIGHTS;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.RIGHTS_OAI_PMH;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.RIGHTS_URI;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.ROLE_GROUP;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.ROLLUP_ID;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.STATUS;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.STREAMING_TYPE;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.STREAMING_URL;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.SUBJECT;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.TIMESTAMP;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.TITLE;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.VERSION;


import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import org.apache.solr.client.solrj.beans.Field;

import edu.unc.lib.boxc.common.util.DateTimeUtil;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
@SuppressWarnings("unchecked")
public class IndexDocumentBean {
    protected PID pid;

    private Map<String, Object> fields;

    public IndexDocumentBean() {
        fields = new HashMap<>();
    }

    @Field("*_d")
    protected Map<String, Object> dynamicFields;

    public String getId() {
        return (String) fields.get(ID.getSolrField());
    }

    @Field
    public void setId(String id) {
        fields.put(ID.getSolrField(), id);
        this.pid = PIDs.get(id);
    }

    public PID getPid() {
        return pid;
    }

    public List<String> getAncestorPath() {
        return (List<String>) fields.get(ANCESTOR_PATH.getSolrField());
    }

    @Field
    public void setAncestorPath(List<String> ancestorPath) {
        fields.put(ANCESTOR_PATH.getSolrField(), ancestorPath);
    }

    public String getAncestorIds() {
        return (String) fields.get(ANCESTOR_IDS.getSolrField());
    }

    @Field
    public void setAncestorIds(String ancestorIds) {
        fields.put(ANCESTOR_IDS.getSolrField(), ancestorIds);
    }

    public String getParentCollection() {
        return (String) fields.get(PARENT_COLLECTION.getSolrField());
    }

    @Field
    public void setParentCollection(String parentCollection) {
        fields.put(PARENT_COLLECTION.getSolrField(), parentCollection);
    }

    public String getParentUnit() {
        return (String) fields.get(PARENT_UNIT.getSolrField());
    }

    @Field
    public void setParentUnit(String parentUnit) {
        fields.put(PARENT_UNIT.getSolrField(), parentUnit);
    }

    public String getRollup() {
        return (String) fields.get(ROLLUP_ID.getSolrField());
    }

    @Field
    public void setRollup(String rollup) {
        fields.put(ROLLUP_ID.getSolrField(), rollup);
    }

    public Long get_version_() {
        return (Long) fields.get(VERSION.getSolrField());
    }

    @Field
    public void set_version_(Long _version_) {
        fields.put(VERSION.getSolrField(), _version_);
    }

    public List<String> getDatastream() {
        return (List<String>) fields.get(DATASTREAM.getSolrField());
    }

    @Field
    public void setDatastream(List<String> datastream) {
        fields.put(DATASTREAM.getSolrField(), datastream);
    }

    public Long getFilesizeSort() {
        return (Long) fields.get(FILESIZE.getSolrField());
    }

    @Field
    public void setFilesizeSort(Long filesizeSort) {
        fields.put(FILESIZE.getSolrField(), filesizeSort);
    }

    public Long getFilesizeTotal() {
        return (Long) fields.get(FILESIZE_TOTAL.getSolrField());
    }

    @Field
    public void setFilesizeTotal(Long filesizeTotal) {
        fields.put(FILESIZE_TOTAL.getSolrField(), filesizeTotal);
    }

    public String getResourceType() {
        return (String) fields.get(RESOURCE_TYPE.getSolrField());
    }

    @Field
    public void setResourceType(String resourceType) {
        fields.put(RESOURCE_TYPE.getSolrField(), resourceType);
    }

    public Integer getResourceTypeSort() {
        return (Integer) fields.get(RESOURCE_TYPE_SORT.getSolrField());
    }

    @Field
    public void setResourceTypeSort(Integer resourceTypeSort) {
        fields.put(RESOURCE_TYPE_SORT.getSolrField(), resourceTypeSort);
    }

    public List<String> getFileFormatCategory() {
        return (List<String>) fields.get(SearchFieldKey.FILE_FORMAT_CATEGORY.getSolrField());
    }

    @Field
    public void setFileFormatCategory(List<String> categories) {
        fields.put(SearchFieldKey.FILE_FORMAT_CATEGORY.getSolrField(), categories);
    }

    public List<String> getFileFormatType() {
        return (List<String>) fields.get(SearchFieldKey.FILE_FORMAT_TYPE.getSolrField());
    }

    @Field
    public void setFileFormatType(List<String> types) {
        fields.put(SearchFieldKey.FILE_FORMAT_TYPE.getSolrField(), types);
    }

    public List<String> getFileFormatDescription() {
        return (List<String>) fields.get(SearchFieldKey.FILE_FORMAT_DESCRIPTION.getSolrField());
    }

    @Field
    public void setFileFormatDescription(List<String> types) {
        fields.put(SearchFieldKey.FILE_FORMAT_DESCRIPTION.getSolrField(), types);
    }

    public Date getTimestamp() {
        return (Date) fields.get(TIMESTAMP.getSolrField());
    }

    @Field
    public void setTimestamp(Date timestamp) {
        fields.put(TIMESTAMP.getSolrField(), timestamp);
    }

    public Date getLastIndexed() {
        return (Date) fields.get(LAST_INDEXED.getSolrField());
    }

    @Field
    public void setLastIndexed(Date lastIndexed) {
        fields.put(LAST_INDEXED.getSolrField(), lastIndexed);
    }

    public List<String> getRoleGroup() {
        return (List<String>) fields.get(ROLE_GROUP.getSolrField());
    }

    @Field
    public void setRoleGroup(List<String> roleGroup) {
        fields.put(ROLE_GROUP.getSolrField(), roleGroup);
    }

    public List<String> getReadGroup() {
        return (List<String>) fields.get(READ_GROUP.getSolrField());
    }

    @Field
    public void setReadGroup(List<String> readGroup) {
        fields.put(READ_GROUP.getSolrField(), readGroup);
    }

    public List<String> getAdminGroup() {
        return (List<String>) fields.get(ADMIN_GROUP.getSolrField());
    }

    @Field
    public void setAdminGroup(List<String> adminGroup) {
        fields.put(ADMIN_GROUP.getSolrField(), adminGroup);
    }

    public List<String> getStatus() {
        return (List<String>) fields.get(STATUS.getSolrField());
    }

    @Field
    public void setStatus(List<String> status) {
        fields.put(STATUS.getSolrField(), status);
    }

    public List<String> getContentStatus() {
        return (List<String>) fields.get(CONTENT_STATUS.getSolrField());
    }

    @Field
    public void setContentStatus(List<String> contentStatus) {
        fields.put(CONTENT_STATUS.getSolrField(), contentStatus);
    }

    public List<String> getIdentifier() {
        return (List<String>) fields.get(IDENTIFIER.getSolrField());
    }

    @Field
    public void setIdentifier(List<String> identifier) {
        fields.put(IDENTIFIER.getSolrField(), identifier);
    }

    public String getIdentifierSort() {
        return (String) fields.get(IDENTIFIER_SORT.getSolrField());
    }

    @Field
    public void setIdentifierSort(String identifierSort) {
        fields.put(IDENTIFIER_SORT.getSolrField(), identifierSort);
    }

    public Integer getMemberOrderId() {
        return (Integer) fields.get(SearchFieldKey.MEMBER_ORDER_ID.getSolrField());
    }

    @Field
    public void setMemberOrderId(Integer value) {
        fields.put(SearchFieldKey.MEMBER_ORDER_ID.getSolrField(), value);
    }

    public String getViewBehavior() {
        return (String) fields.get(SearchFieldKey.VIEW_BEHAVIOR.getSolrField());
    }
    @Field
    public void setViewBehavior(String behavior) {
        fields.put(SearchFieldKey.VIEW_BEHAVIOR.getSolrField(), behavior);
    }

    public String getTitle() {
        return (String) fields.get(TITLE.getSolrField());
    }

    @Field
    public void setTitle(String title) {
        fields.put(TITLE.getSolrField(), title);
    }

    public List<String> getOtherTitle() {
        return (List<String>) fields.get(OTHER_TITLE.getSolrField());
    }

    @Field
    public void setOtherTitle(List<String> otherTitle) {
        fields.put(OTHER_TITLE.getSolrField(), otherTitle);
    }

    public String getAbstractText() {
        return (String) fields.get(ABSTRACT.getSolrField());
    }

    @Field("abstract")
    public void setAbstractText(String abstractText) {
        fields.put(ABSTRACT.getSolrField(), abstractText);
    }

    public List<String> getKeyword() {
        return (List<String>) fields.get(KEYWORD.getSolrField());
    }

    @Field
    public void setCollectionId(String collectionId) {
        fields.put(COLLECTION_ID.getSolrField(), collectionId);
    }

    public String getCollectionId() {
        return (String) fields.get(COLLECTION_ID.getSolrField());
    }

    @Field
    public void setKeyword(List<String> keyword) {
        fields.put(KEYWORD.getSolrField(), keyword);
    }

    public List<String> getSubject() {
        return (List<String>) fields.get(SUBJECT.getSolrField());
    }

    @Field
    public void setSubject(List<String> subject) {
        fields.put(SUBJECT.getSolrField(), subject);
    }

    public List<String> getOtherSubject() {
        return (List<String>) fields.get(OTHER_SUBJECT.getSolrField());
    }

    @Field
    public void setOtherSubject(List<String> otherSubject) {
        fields.put(OTHER_SUBJECT.getSolrField(), otherSubject);
    }

    @Field
    public void setGenre(List<String> genre) {
        fields.put(SearchFieldKey.GENRE.getSolrField(), genre);
    }

    public List<String> getGenre() {
        return (List<String>) fields.get(SearchFieldKey.GENRE.getSolrField());
    }

    public String getDateCreatedYear() {
        return (String) fields.get(DATE_CREATED_YEAR.getSolrField());
    }

    @Field
    public void setDateCreatedYear(String dateCreatedYear) {
        fields.put(DATE_CREATED_YEAR.getSolrField(), dateCreatedYear);
    }

    public List<String> getLanguage() {
        return (List<String>) fields.get(LANGUAGE.getSolrField());
    }

    @Field
    public void setLanguage(List<String> language) {
        fields.put(LANGUAGE.getSolrField(), language);
    }

    public List<String> getLocation() {
        return (List<String>) fields.get(LOCATION.getSolrField());
    }

    @Field
    public void setLocation(List<String> location) {
        fields.put(LOCATION.getSolrField(), location);
    }

    public List<String> getCreator() {
        return (List<String>) fields.get(CREATOR.getSolrField());
    }

    @Field
    public void setCreator(List<String> creator) {
        fields.put(CREATOR.getSolrField(), creator);
    }

    public List<String> getContributor() {
        return (List<String>) fields.get(CONTRIBUTOR.getSolrField());
    }

    @Field
    public void setContributor(List<String> contributor) {
        fields.put(CONTRIBUTOR.getSolrField(), contributor);
    }

    public List<String> getCreatorContributor() {
        return (List<String>) fields.get(CREATOR_CONTRIBUTOR.getSolrField());
    }

    @Field
    public void setCreatorContributor(List<String> creatorsContributors) {
        fields.put(CREATOR_CONTRIBUTOR.getSolrField(), creatorsContributors);
    }

    public Date getDateCreated() {
        return (Date) fields.get(DATE_CREATED.getSolrField());
    }

    @Field
    public void setDateCreated(Date dateCreated) {
        fields.put(DATE_CREATED.getSolrField(), dateCreated);
    }

    public Date getDateAdded() {
        return (Date) fields.get(DATE_ADDED.getSolrField());
    }

    @Field
    public void setDateAdded(Date dateAdded) {
        fields.put(DATE_ADDED.getSolrField(), dateAdded);
    }

    public void setDateAdded(String dateAdded) throws ParseException {
        fields.put(DATE_ADDED.getSolrField(), DateTimeUtil.parseUTCToDate(dateAdded));
    }

    public Date getDateUpdated() {
        return (Date) fields.get(DATE_UPDATED.getSolrField());
    }

    @Field
    public void setDateUpdated(Date dateUpdated) {
        fields.put(DATE_UPDATED.getSolrField(), dateUpdated);
    }

    public void setDateUpdated(String dateUpdated) throws ParseException {
        fields.put(DATE_UPDATED.getSolrField(), DateTimeUtil.parseUTCToDate(dateUpdated));
    }

    public String getCitation() {
        return (String) fields.get(CITATION.getSolrField());
    }

    @Field
    public void setExhibit(List<String> exhibits) {
        fields.put(EXHIBIT.getSolrField(), exhibits);
    }

    public List<String> getExhibit() {
        return (List<String>) fields.get(EXHIBIT.getSolrField());
    }

    @Field
    public void setCitation(String citation) {
        fields.put(CITATION.getSolrField(), citation);
    }

    public List<String> getPublisher() {
        return (List<String>) fields.get(PUBLISHER.getSolrField());
    }

    @Field
    public void setPublisher(List<String> publishers) {
        fields.put(PUBLISHER.getSolrField(), publishers);
    }

    public List<String> getRights() {
        return (List<String>) fields.get(RIGHTS.getSolrField());
    }

    @Field
    public void setRights(List<String> rights) {
        fields.put(RIGHTS.getSolrField(), rights);
    }

    @Field
    public void setRightsOaiPmh(List<String> rights) {
        fields.put(RIGHTS_OAI_PMH.getSolrField(), rights);
    }

    public List<String> getRightsOaiPmh() {
        return (List<String>) fields.get(RIGHTS_OAI_PMH.getSolrField());
    }

    public List<String> getRightsUri() {
        return (List<String>) fields.get(RIGHTS_URI.getSolrField());
    }

    @Field
    public void setRightsUri(List<String> rightsUri) {
        fields.put(RIGHTS_URI.getSolrField(), rightsUri);
    }

    public String getFullText() {
        return (String) fields.get(FULL_TEXT.getSolrField());
    }

    @Field
    public void setFullText(String fullText) {
        fields.put(FULL_TEXT.getSolrField(), fullText);
    }

    public Map<String, Object> getDynamicFields() {
        return dynamicFields;
    }

    public void setDynamicFields(Map<String, Object> dynamicFields) {
        this.dynamicFields = dynamicFields;
    }

    public String getStreamingType() {
        return (String) fields.get(STREAMING_TYPE.getSolrField());
    }

    @Field
    public void setStreamingType(String streamingType) {
        fields.put(STREAMING_TYPE.getSolrField(), streamingType);
    }

    public String getStreamingUrl() {
        return (String) fields.get(STREAMING_URL.getSolrField());
    }

    @Field
    public void setStreamingUrl(String streamingUrl) {
        fields.put(STREAMING_URL.getSolrField(), streamingUrl);
    }

    public String getAspaceRefId() {
        return (String) fields.get(ASPACE_REF_ID.getSolrField());
    }

    @Field
    public void setAspaceRefId(String aspaceRefId) {
        fields.put(ASPACE_REF_ID.getSolrField(), aspaceRefId);
    }

    public String getHookId() {
        return (String) fields.get(HOOK_ID.getSolrField());
    }

    @Field
    public void setHookId(String hookId) {
        fields.put(HOOK_ID.getSolrField(), hookId);
    }

    /**
     * Map containing all non-dynamic fields
     *
     */
    public Map<String, Object> getFields() {
        return fields;
    }

    public String getAltText() {
        return (String) fields.get(SearchFieldKey.ALT_TEXT.getSolrField());
    }

    @Field
    public void setAltText(String altText) {
        fields.put(SearchFieldKey.ALT_TEXT.getSolrField(), altText);
    }
}
