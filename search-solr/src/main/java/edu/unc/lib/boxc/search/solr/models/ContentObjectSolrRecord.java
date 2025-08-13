package edu.unc.lib.boxc.search.solr.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.solr.facets.FilterableDisplayValueFacet;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.beans.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.HierarchicalFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.api.models.ObjectPath;
import edu.unc.lib.boxc.search.api.models.ObjectPathEntry;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
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
    protected String ancestorNames;
    protected String parentName;
    protected String parentId;
    protected List<HierarchicalFacet> contentTypeFacet;
    protected List<Datastream> datastreamObjects;
    // Inverted map of the roleGroup, clustering roles into buckets by group
    Map<String, Collection<String>> groupRoleMap;
    protected Map<String, Long> countMap;
    protected String thumbnailId;
    protected String checksum;

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
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("id: " + getId() + "\n");
        sb.append("ancestorPath: " + getAncestorPath() + "\n");
        sb.append("resourceType: " + getResourceType() + "\n");
        sb.append("exhibit: " + getExhibit() + "\n");
        sb.append("fileFormatCategory: " + getFileFormatCategory() + "\n");
        sb.append("fileFormatType: " + getFileFormatType() + "\n");
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
        if (StringUtils.isBlank(parentCollection)) {
            return null;
        }
        var facetVal = new FilterableDisplayValueFacet(SearchFieldKey.PARENT_COLLECTION.name(), parentCollection);
        parentName = facetVal.getDisplayValue();
        return parentName;
    }

    public String getParentCollectionId() {
        if (parentId != null) {
            return parentId;
        }
        String parentCollection = getParentCollection();
        if (StringUtils.isBlank(parentCollection)) {
            return null;
        }
        // split the parent collection string at | and get the second part
        parentId = parentCollection.split("[|]")[1];
        return parentId;
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

    @Override
    public String getThumbnailId() {
        return thumbnailId;
    }

    @Override
    public void setThumbnailId(String thumbnailId) {
        this.thumbnailId = thumbnailId;
    }

    @Override
    public String getChecksum() {
        if (checksum != null) {
            return checksum;
        }
        var datastream = getDatastreamObject(DatastreamType.ORIGINAL_FILE.getId());
        var checksum = datastream.getChecksum();
        this.checksum = checksum;
        return checksum;
    }
}
