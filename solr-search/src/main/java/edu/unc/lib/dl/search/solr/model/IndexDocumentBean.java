/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.Field;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DateTimeUtil;

/**
 *
 * @author bbpennel, harring
 *
 */
public class IndexDocumentBean {
    protected PID pid;
    protected String id;
    protected List<String> ancestorPath;
    protected String ancestorIds;
    protected String parentCollection;
    protected String parentUnit;
    protected String label;

    protected List<String> scope;
    protected String rollup;
    protected Boolean isPart;
    protected Long _version_;

    protected List<String> datastream;
    protected Long filesizeSort;
    protected Long filesizeTotal;

    protected List<String> relations;

    protected List<String> contentModel;
    protected String resourceType;
    protected Integer resourceTypeSort;

    protected String creatorSort;
    protected String defaultSortType;
    protected Long displayOrder;

    protected List<String> contentType;

    protected Date timestamp;
    protected Date lastIndexed;

    protected List<String> roleGroup;
    protected List<String> readGroup;
    protected List<String> adminGroup;
    protected List<String> status;
    protected List<String> contentStatus;
    protected HashSet<String> statusTags;

    protected List<String> identifier;
    protected String identifierSort;

    // Descriptive fields
    protected String title;
    protected List<String> otherTitle;
    protected String abstractText;
    protected List<String> keyword;
    protected List<String> subject;
    protected List<String> language;
    protected List<String> creator;
    protected List<String> contributor;
    protected List<String> department;

    protected Date dateCreated;
    protected Date dateAdded;
    protected Date dateUpdated;

    protected String citation;

    protected String fullText;

    @Field("*_d")
    protected Map<String, Object> dynamicFields;

    public String getId() {
        return id;
    }

    @Field
    public void setId(String id) {
        this.id = id;
        this.pid = new PID(this.id);
    }

    public PID getPid() {
        return pid;
    }

    public List<String> getAncestorPath() {
        return ancestorPath;
    }

    @Field
    public void setAncestorPath(List<String> ancestorPath) {
        this.ancestorPath = ancestorPath;
    }

    public String getAncestorIds() {
        return ancestorIds;
    }

    @Field
    public void setAncestorIds(String ancestorIds) {
        this.ancestorIds = ancestorIds;
    }

    public String getParentCollection() {
        return parentCollection;
    }

    @Field
    public void setParentCollection(String parentCollection) {
        this.parentCollection = parentCollection;
    }

    public String getParentUnit() {
        return parentUnit;
    }

    @Field
    public void setParentUnit(String parentUnit) {
        this.parentUnit = parentUnit;
    }

    public String getLabel() {
        return label;
    }

    @Field
    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getScope() {
        return scope;
    }

    @Field
    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getRollup() {
        return rollup;
    }

    @Field
    public void setRollup(String rollup) {
        this.rollup = rollup;
    }

    public Boolean getIsPart() {
        return isPart;
    }

    @Field
    public void setIsPart(Boolean b) {
        this.isPart = b;
    }

    public Long get_version_() {
        return _version_;
    }

    @Field
    public void set_version_(Long _version_) {
        this._version_ = _version_;
    }

    public List<String> getDatastream() {
        return datastream;
    }

    @Field
    public void setDatastream(List<String> datastream) {
        this.datastream = datastream;
    }

    public Long getFilesizeSort() {
        return filesizeSort;
    }

    @Field
    public void setFilesizeSort(Long filesizeSort) {
        this.filesizeSort = filesizeSort;
    }

    public Long getFilesizeTotal() {
        return filesizeTotal;
    }

    @Field
    public void setFilesizeTotal(Long filesizeTotal) {
        this.filesizeTotal = filesizeTotal;
    }

    public List<String> getRelations() {
        return relations;
    }

    @Field
    public void setRelations(List<String> relations) {
        this.relations = relations;
    }

    public List<String> getContentModel() {
        return contentModel;
    }

    @Field
    public void setContentModel(List<String> contentModel) {
        this.contentModel = contentModel;
    }

    public String getResourceType() {
        return resourceType;
    }

    @Field
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Integer getResourceTypeSort() {
        return resourceTypeSort;
    }

    @Field
    public void setResourceTypeSort(Integer resourceTypeSort) {
        this.resourceTypeSort = resourceTypeSort;
    }

    public String getCreatorSort() {
        return creatorSort;
    }

    @Field
    public void setCreatorSort(String creatorSort) {
        this.creatorSort = creatorSort;
    }

    public String getDefaultSortType() {
        return defaultSortType;
    }

    @Field
    public void setDefaultSortType(String defaultSortType) {
        this.defaultSortType = defaultSortType;
    }

    public Long getDisplayOrder() {
        return displayOrder;
    }

    @Field
    public void setDisplayOrder(Long displayOrder) {
        this.displayOrder = displayOrder;
    }

    public List<String> getContentType() {
        return contentType;
    }

    @Field
    public void setContentType(List<String> contentType) {
        this.contentType = contentType;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Field
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getLastIndexed() {
        return lastIndexed;
    }

    @Field
    public void setLastIndexed(Date lastIndexed) {
        this.lastIndexed = lastIndexed;
    }

    public List<String> getRoleGroup() {
        return roleGroup;
    }

    @Field
    public void setRoleGroup(List<String> roleGroup) {
        this.roleGroup = roleGroup;
    }

    public List<String> getReadGroup() {
        return readGroup;
    }

    @Field
    public void setReadGroup(List<String> readGroup) {
        this.readGroup = readGroup;
    }

    public List<String> getAdminGroup() {
        return adminGroup;
    }

    @Field
    public void setAdminGroup(List<String> adminGroup) {
        this.adminGroup = adminGroup;
    }

    public List<String> getStatus() {
        return status;
    }

    @Field
    public void setStatus(List<String> status) {
        this.status = status;
    }

    public List<String> getContentStatus() {
        return contentStatus;
    }

    @Field
    public void setStatusTags(HashSet<String> statusTags) {
        this.statusTags = statusTags;
    }

    public HashSet<String> getStatusTags() {
        return statusTags;
    }

    @Field
    public void setContentStatus(List<String> contentStatus) {
        this.contentStatus = contentStatus;
    }

    public List<String> getIdentifier() {
        return identifier;
    }

    @Field
    public void setIdentifier(List<String> identifier) {
        this.identifier = identifier;
    }

    public String getIdentifierSort() {
        return identifierSort;
    }

    @Field
    public void setIdentifierSort(String identifierSort) {
        this.identifierSort = identifierSort;
    }

    public String getTitle() {
        return title;
    }

    @Field
    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getOtherTitle() {
        return otherTitle;
    }

    @Field
    public void setOtherTitle(List<String> otherTitle) {
        this.otherTitle = otherTitle;
    }

    public String getAbstractText() {
        return abstractText;
    }

    @Field("abstract")
    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public List<String> getKeyword() {
        return keyword;
    }

    @Field
    public void setKeyword(List<String> keyword) {
        this.keyword = keyword;
    }

    public List<String> getSubject() {
        return subject;
    }

    @Field
    public void setSubject(List<String> subject) {
        this.subject = subject;
    }

    public List<String> getLanguage() {
        return language;
    }

    @Field
    public void setLanguage(List<String> language) {
        this.language = language;
    }

    public List<String> getCreator() {
        return creator;
    }

    @Field
    public void setCreator(List<String> creator) {
        this.creator = creator;
    }

    public List<String> getContributor() {
        return contributor;
    }

    @Field
    public void setContributor(List<String> contributor) {
        this.contributor = contributor;
    }

    public List<String> getDepartment() {
        return department;
    }

    @Field
    public void setDepartment(List<String> department) {
        this.department = department;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    @Field
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    @Field
    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }

    public void setDateAdded(String dateAdded) throws ParseException {
        this.dateAdded = DateTimeUtil.parseUTCToDate(dateAdded);
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }

    @Field
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public void setDateUpdated(String dateUpdated) throws ParseException {
        this.dateUpdated = DateTimeUtil.parseUTCToDate(dateUpdated);
    }

    public String getCitation() {
        return citation;
    }

    @Field
    public void setCitation(String citation) {
        this.citation = citation;
    }

    public String getFullText() {
        return fullText;
    }

    @Field
    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public Map<String, Object> getDynamicFields() {
        return dynamicFields;
    }

    public void setDynamicFields(Map<String, Object> dynamicFields) {
        this.dynamicFields = dynamicFields;
    }
}
