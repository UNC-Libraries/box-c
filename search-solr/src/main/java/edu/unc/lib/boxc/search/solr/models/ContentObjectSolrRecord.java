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
package edu.unc.lib.boxc.search.solr.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.common.util.DateTimeUtil;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.HierarchicalFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.api.models.ObjectPath;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.facets.MultivaluedHierarchicalFacet;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;

/**
 * Stores a single Solr tuple representing an object from a search result. Can be populated directly by Solrj's
 * queryResponse.getBeans.
 *
 * @author bbpennel
 */
public class ContentObjectSolrRecord extends IndexDocumentBean implements ContentObjectRecord {
    private static final Logger LOG = LoggerFactory.getLogger(ContentObjectSolrRecord.class);

    protected static ObjectPathFactory pathFactory;

    protected CutoffFacetImpl ancestorPathFacet;
    protected CutoffFacetImpl path;
    protected ObjectPath objectPath;
    protected String parentName;
    protected List<HierarchicalFacet> contentTypeFacet;
    protected List<Datastream> datastreamObjects;
    // Inverted map of the roleGroup, clustering roles into buckets by group
    Map<String, Collection<String>> groupRoleMap;
    protected Map<String, Long> countMap;
    protected Map<String, List<String>> relationsMap;
    protected String thumbnailId;

    public ContentObjectSolrRecord() {
        countMap = new HashMap<>(2);
    }

    // TODO getDefaultWebData getDefaultWebObject getFilesizeByDatastream

    public String getIdWithoutPrefix() {
        String id = getId();
        int index = id.indexOf(":");
        if (index != -1) {
            return id.substring(index + 1);
        }
        return id;
    }

    @Override
    public CutoffFacetImpl getAncestorPathFacet() {
        return ancestorPathFacet;
    }

    public void setAncestorPathFacet(CutoffFacetImpl ancestorPathFacet) {
        this.ancestorPathFacet = ancestorPathFacet;
    }

    @Override
    @Field
    public void setAncestorPath(List<String> ancestorPaths) {
        super.setAncestorPath(ancestorPaths);
        this.ancestorPathFacet = new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), ancestorPaths, 0);
    }

    /**
     * Returns a HierarchicalFacet of the full path for this object, including the ancestor path and itself.
     *
     * @return
     */
    @Override
    public CutoffFacetImpl getPath() {
        if (path == null) {
            if (getAncestorPath() == null) {
                this.path = new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), "1," + getId() + "," + getTitle(),
                        0L);
            } else {
                path = new CutoffFacetImpl(ancestorPathFacet);
                path.addNode(getId());
            }
        }
        return path;
    }

    @Override
    public List<HierarchicalFacet> getContentTypeFacet() {
        return contentTypeFacet;
    }

    @Field
    public void setContentType(List<String> contentTypes) {
        super.setContentType(contentTypes);
        this.contentTypeFacet = MultivaluedHierarchicalFacet.createMultivaluedHierarchicalFacets(
                SearchFieldKey.CONTENT_TYPE.name(), contentTypes);
    }

    @Override
    public List<Datastream> getDatastreamObjects() {
        return datastreamObjects;
    }

    @Override
    public Datastream getDatastreamObject(String datastreamName) {
        if (datastreamName == null || this.datastreamObjects == null) {
            return null;
        }

        String[] datastreamParts = datastreamName.split("/", 2);
        String pid;
        if (datastreamParts.length > 1) {
            pid = datastreamParts[0];
            if (pid.equals(getId())) {
                pid = null;
            }
            datastreamName = datastreamParts[1];
        } else {
            pid = null;
        }

        for (Datastream datastream: this.datastreamObjects) {
            if (datastream.equals(datastreamName) && (pid == null || pid.equals(datastream.getOwner()))) {
                return datastream;
            }
        }
        return null;
    }

    @Override
    @Field
    public void setDatastream(List<String> datastream) {
        super.setDatastream(datastream);

        datastreamObjects = new ArrayList<>();
        for (String value : datastream) {
            datastreamObjects.add(new DatastreamImpl(value));
        }
    }

    @Override
    @Field
    public void setRoleGroup(List<String> roleGroup) {
        super.setRoleGroup(roleGroup);

        groupRoleMap = new HashMap<>();
        if (roleGroup != null) {
            for (String roleGroupPair : roleGroup) {
                String[] roleGroupData = roleGroupPair.split("\\|");
                if (roleGroupData.length == 2) {
                    Collection<String> roles = groupRoleMap.get(roleGroupData[1]);
                    if (roles == null) {
                        roles = new ArrayList<>();
                        groupRoleMap.put(roleGroupData[1], roles);
                    }
                    roles.add(roleGroupData[0]);
                }
            }
        }
    }

    @Override
    public Map<String, Collection<String>> getGroupRoleMap() {
        return groupRoleMap;
    }

    @Override
    public List<String> getRelation(String relationName) {
        if (relationsMap == null) {
            return null;
        }
        return this.relationsMap.get(relationName);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("id: " + getId() + "\n");
        sb.append("ancestorPath: " + getAncestorPath() + "\n");
        sb.append("resourceType: " + getResourceType() + "\n");
        sb.append("contentType: " + getContentType() + "\n");
        sb.append("datastream: " + getDatastream() + "\n");
        sb.append("title: " + getTitle() + "\n");
        sb.append("abstractText: " + getAbstractText() + "\n");
        sb.append("keyword: " + getKeyword() + "\n");
        sb.append("subject: " + getSubject() + "\n");
        sb.append("language: " + getLanguage() + "\n");
        sb.append("publisher: " + getPublisher() + "\n");
        sb.append("creator: " + getCreator() + "\n");
        sb.append("contributor: " + getContributor() + "\n");
        sb.append("creatorContributor: " + getCreatorContributor() + "\n");
        sb.append("dateCreated: " + getDateCreated() + "\n");
        sb.append("dateCreatedYear: " + getDateCreatedYear() + "\n");
        sb.append("dateAdded: " + getDateAdded() + "\n");
        sb.append("dateUpdated: " + getDateUpdated() + "\n");
        sb.append("timestamp: " + getTimestamp() + "\n");
        sb.append("contentStatus: " + getContentStatus() + "\n");
        return sb.toString();
    }

    @Override
    public String getParentCollectionName() {

        if (parentName != null) {
            return parentName;
        }

        String parentCollection = getParentCollection();
        if (objectPath == null) {
            if (pathFactory != null && parentCollection != null) {
                parentName = pathFactory.getName(parentCollection);
            }
        } else {
            parentName = objectPath.getName(parentCollection);
        }

        return parentName;
    }

    @Override
    public Map<String, Long> getCountMap() {
        return this.countMap;
    }

    @Override
    public void setCountMap(Map<String, Long> countMap) {
        this.countMap = countMap;
    }

    @Override
    public Date getActiveEmbargo() {
        List<String> embargoUntil = getRelation(CdrAcl.embargoUntil.getURI());
        if (embargoUntil != null) {
            Date result = null;
            Date dateNow = new Date();
            for (String embargo : embargoUntil) {
                Date embargoDate;
                try {
                    embargoDate = DateTimeUtil.parseUTCToDate(embargo);
                    if (embargoDate.after(dateNow)) {
                        if (result == null || embargoDate.after(result)) {
                            result = embargoDate;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    LOG.error("Failed to parse embargo", e);
                }
            }
            return result;
        }
        return null;
    }

    @Override
    public ObjectPath getObjectPath() {
        // Retrieve the ancestor path on demand if it is not already set
        if (objectPath == null && pathFactory != null) {
            this.objectPath = pathFactory.getPath(this);
        }

        return objectPath;
    }

    @Override
    public void setObjectPath(ObjectPath objectPath) {
        this.objectPath = objectPath;
    }

    public static void setPathFactory(ObjectPathFactory pathFactory) {
        ContentObjectSolrRecord.pathFactory = pathFactory;
    }

    @Override
    public String getThumbnailId() {
        return thumbnailId;
    }

    @Override
    public void setThumbnailId(String thumbnailId) {
        this.thumbnailId = thumbnailId;
    }
}
