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
package edu.unc.lib.dl.search.solr.model;

import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ABSTRACT;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ADMIN_GROUP;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ANCESTOR_IDS;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ANCESTOR_PATH;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.CITATION;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.CONTENT_MODEL;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.CONTENT_STATUS;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.CONTENT_TYPE;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.CONTRIBUTOR;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.CREATOR;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.CREATOR_SORT;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.DATASTREAM;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.DATE_ADDED;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.DATE_CREATED;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.DATE_UPDATED;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.DEPARTMENT;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.DISPLAY_ORDER;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.FILESIZE;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.FILESIZE_TOTAL;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.FULL_TEXT;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ID;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.IDENTIFIER;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.IDENTIFIER_SORT;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.IS_PART;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.KEYWORD;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.LABEL;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.LANGUAGE;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.LAST_INDEXED;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.OTHER_TITLES;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.PARENT_COLLECTION;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.PARENT_UNIT;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.READ_GROUP;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.RELATIONS;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.RESOURCE_TYPE;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.RESOURCE_TYPE_SORT;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ROLE_GROUP;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ROLLUP_ID;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.STATUS;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.STATUS_TAGS;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.SUBJECT;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.TIMESTAMP;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.TITLE;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.VERSION;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.Field;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DateTimeUtil;


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

    public String getLabel() {
        return (String) fields.get(LABEL.getSolrField());
    }

    @Field
    public void setLabel(String label) {
        fields.put(LABEL.getSolrField(), label);
    }

    public String getRollup() {
        return (String) fields.get(ROLLUP_ID.getSolrField());
    }

    @Field
    public void setRollup(String rollup) {
        fields.put(ROLLUP_ID.getSolrField(), rollup);
    }

    public Boolean getIsPart() {
        return (Boolean) fields.get(IS_PART.getSolrField());
    }

    @Field
    public void setIsPart(Boolean isPart) {
        fields.put(IS_PART.getSolrField(), isPart);
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

    public List<String> getRelations() {
        return (List<String>) fields.get(RELATIONS.getSolrField());
    }

    @Field
    public void setRelations(List<String> relations) {
        fields.put(RELATIONS.getSolrField(), relations);
    }

    public List<String> getContentModel() {
        return (List<String>) fields.get(CONTENT_MODEL.getSolrField());
    }

    @Field
    public void setContentModel(List<String> contentModel) {
        fields.put(CONTENT_MODEL.getSolrField(), contentModel);
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

    public String getCreatorSort() {
        return (String) fields.get(CREATOR_SORT.getSolrField());
    }

    @Field
    public void setCreatorSort(String creatorSort) {
        fields.put(CREATOR_SORT.getSolrField(), creatorSort);
    }

    public Long getDisplayOrder() {
        return (Long) fields.get(DISPLAY_ORDER.getSolrField());
    }

    @Field
    public void setDisplayOrder(Long displayOrder) {
        fields.put(DISPLAY_ORDER.getSolrField(), displayOrder);
    }

    public List<String> getContentType() {
        return (List<String>) fields.get(CONTENT_TYPE.getSolrField());
    }

    @Field
    public void setContentType(List<String> contentType) {
        fields.put(CONTENT_TYPE.getSolrField(), contentType);
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
    public void setStatusTags(List<String> statusTags) {
        fields.put(STATUS_TAGS.getSolrField(), statusTags);
    }

    public List<String> getStatusTags() {
        return (List<String>) fields.get(STATUS_TAGS.getSolrField());
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

    public String getTitle() {
        return (String) fields.get(TITLE.getSolrField());
    }

    @Field
    public void setTitle(String title) {
        fields.put(TITLE.getSolrField(), title);
    }

    public List<String> getOtherTitle() {
        return (List<String>) fields.get(OTHER_TITLES.getSolrField());
    }

    @Field
    public void setOtherTitle(List<String> otherTitle) {
        fields.put(OTHER_TITLES.getSolrField(), otherTitle);
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

    public List<String> getLanguage() {
        return (List<String>) fields.get(LANGUAGE.getSolrField());
    }

    @Field
    public void setLanguage(List<String> language) {
        fields.put(LANGUAGE.getSolrField(), language);
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

    public List<String> getDepartment() {
        return (List<String>) fields.get(DEPARTMENT.getSolrField());
    }

    @Field
    public void setDepartment(List<String> department) {
        fields.put(DEPARTMENT.getSolrField(), department);
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
    public void setCitation(String citation) {
        fields.put(CITATION.getSolrField(), citation);
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

    /**
     * Map containing all non-dynamic fields
     *
     */
    public Map<String, Object> getFields() {
        return fields;
    }
}
