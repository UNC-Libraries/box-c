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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.fcrepo3.ObjectAccessControlsBeanImpl;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.search.solr.service.ObjectPathFactory;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.DateTimeUtil;

/**
 * Stores a single Solr tuple representing an object from a search result. Can be populated directly by Solrj's
 * queryResponse.getBeans.
 *
 * @author bbpennel
 */
public class BriefObjectMetadataBean extends IndexDocumentBean implements BriefObjectMetadata {
    private static final Logger LOG = LoggerFactory.getLogger(BriefObjectMetadataBean.class);

    protected static ObjectPathFactory pathFactory;

    protected CutoffFacet ancestorPathFacet;
    protected CutoffFacet path;
    protected ObjectPath objectPath;
    protected String ancestorNames;
    protected String parentName;
    protected List<MultivaluedHierarchicalFacet> contentTypeFacet;
    protected List<Datastream> datastreamObjects;
    // Inverted map of the roleGroup, clustering roles into buckets by group
    Map<String, Collection<String>> groupRoleMap;
    protected Map<String, Long> countMap;
    protected ObjectAccessControlsBean accessControlBean;
    protected Map<String, List<String>> relationsMap;
    private List<Tag> tags;

    public BriefObjectMetadataBean() {
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
    public CutoffFacet getAncestorPathFacet() {
        return ancestorPathFacet;
    }

    public void setAncestorPathFacet(CutoffFacet ancestorPathFacet) {
        this.ancestorPathFacet = ancestorPathFacet;
    }

    @Override
    @Field
    public void setAncestorPath(List<String> ancestorPaths) {
        super.setAncestorPath(ancestorPaths);
        this.ancestorPathFacet = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), ancestorPaths, 0);
    }

    /**
     * Returns a HierarchicalFacet of the full path for this object, including the ancestor path and itself.
     *
     * @return
     */
    @Override
    public CutoffFacet getPath() {
        if (path == null) {
            if (getAncestorPath() == null) {
                this.path = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), "1," + getId() + "," + getTitle(),
                        0L);
            } else {
                path = new CutoffFacet(ancestorPathFacet);
                path.addNode(getId());
            }
        }
        return path;
    }

    @Override
    public List<MultivaluedHierarchicalFacet> getContentTypeFacet() {
        return contentTypeFacet;
    }

    @Field
    public void setContentType(ArrayList<String> contentTypes) {
        super.setContentType(contentTypes);
        this.contentTypeFacet = MultivaluedHierarchicalFacet.createMultivaluedHierarchicalFacets(
                SearchFieldKeys.CONTENT_TYPE.name(), contentTypes);
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
            datastreamObjects.add(new Datastream(value));
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

    public void setAccessControlBean(ObjectAccessControlsBean aclBean) {
        this.accessControlBean = aclBean;
    }

    @Override
    public ObjectAccessControlsBean getAccessControlBean() {
        List<String> roleGroup = getRoleGroup();
        if (this.accessControlBean == null && roleGroup != null) {
            this.accessControlBean = new ObjectAccessControlsBeanImpl(pid, roleGroup);
        }
        return this.accessControlBean;
    }

    @Override
    @Field
    public void setRelations(List<String> relations) {
        super.setRelations(relations);

        this.relationsMap = new HashMap<>(relations.size());
        for (String relation: relations) {
            if (relation == null) {
                continue;
            }
            String[] rdfParts = relation.split("\\|", 2);

            List<String> values = this.relationsMap.get(rdfParts[0]);
            if (values == null) {
                values = new ArrayList<>();
                this.relationsMap.put(rdfParts[0], values);
            }
            values.add(rdfParts[1]);
        }
    }

    @Override
    public List<String> getRelation(String relationName) {
        if (relationsMap == null) {
            return null;
        }
        return this.relationsMap.get(relationName);
    }

    @Override
    public Datastream getDefaultWebData() {
        if (this.relationsMap == null) {
            return null;
        }
        List<String> defaultWebDataValues = this.relationsMap.get(
                ContentModelHelper.CDRProperty.defaultWebData.getPredicate());
        if (defaultWebDataValues == null) {
            return null;
        }
        String defaultWebData = defaultWebDataValues.get(0);
        if (defaultWebData == null) {
            return null;
        }
        return this.getDatastreamObject(defaultWebData);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("id: " + getId() + "\n");
        sb.append("ancestorPath: " + getAncestorPath() + "\n");
        sb.append("ancestorNames: " + ancestorNames + "\n");
        sb.append("resourceType: " + getResourceType() + "\n");
        sb.append("displayOrder: " + getDisplayOrder() + "\n");
        sb.append("contentType: " + getContentType() + "\n");
        sb.append("datastream: " + getDatastream() + "\n");
        sb.append("title: " + getTitle() + "\n");
        sb.append("abstractText: " + getAbstractText() + "\n");
        sb.append("keyword: " + getKeyword() + "\n");
        sb.append("subject: " + getSubject() + "\n");
        sb.append("language: " + getLanguage() + "\n");
        sb.append("creator: " + getCreator() + "\n");
        sb.append("department: " + getDepartment() + "\n");
        sb.append("dateCreated: " + getDateCreated() + "\n");
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
    public List<Tag> getTags() {
        if (this.tags == null) {
            return null;
        }
        return Collections.unmodifiableList(this.tags);
    }

    @Override
    public void addTag(Tag t) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(t);
    }

    @Override
    public Date getActiveEmbargo() {
        List<String> embargoUntil = getRelation(CDRProperty.embargoUntil.getPredicate());
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
                } catch (ParseException e) {
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
        BriefObjectMetadataBean.pathFactory = pathFactory;
    }

    @Override
    public String getAncestorNames() {
        if (ancestorNames == null) {
            if (objectPath == null && pathFactory != null) {
                objectPath = pathFactory.getPath(this);
            }

            if (objectPath != null) {
                StringBuilder ancestorNames = new StringBuilder();
                for (ObjectPathEntry entry : objectPath.getEntries()) {
                    ancestorNames.append('/').append(entry.getName().replaceAll("\\/", "\\\\/"));
                }

                this.ancestorNames = ancestorNames.toString();
            }
        }

        return ancestorNames;
    }
}
