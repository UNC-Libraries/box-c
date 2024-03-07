package edu.unc.lib.boxc.search.solr.models;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.facets.HierarchicalFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.api.models.GroupedContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.ObjectPath;

/**
 * Record containing aggregated content object records from a solr index
 *
 * @author bbpennel
 *
 */
public class GroupedContentObjectSolrRecord implements GroupedContentObjectRecord {
    private final List<ContentObjectRecord> items;
    private ContentObjectRecord representative;
    private final Long itemCount;
    private final String groupId;

    public GroupedContentObjectSolrRecord(String groupId, List<ContentObjectSolrRecord> items, Long itemCount) {
        this.items = items.stream().map(i -> (ContentObjectRecord) i).collect(Collectors.toList());
        this.itemCount = itemCount;
        this.groupId = groupId;
        if (this.groupId != null) {
            for (ContentObjectRecord item : items) {
                if (this.groupId.equals(item.getRollup())) {
                    this.representative = item;
                    break;
                }
            }
        }

        if (this.representative == null && items.size() > 0) {
            this.representative = items.get(0);
        }
    }

    @Override
    public ContentObjectRecord getRepresentative() {
        return representative;
    }

    public void setRepresentative(ContentObjectRecord representative) {
        this.representative = representative;
    }

    @Override
    public List<ContentObjectRecord> getItems() {
        return items;
    }

    @Override
    public Long getItemCount() {
        return itemCount;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public CutoffFacet getAncestorPathFacet() {
        return this.representative.getAncestorPathFacet();
    }

    @Override
    public CutoffFacet getPath() {
        return this.representative.getPath();
    }

    @Override
    public List<HierarchicalFacet> getContentTypeFacet() {
        return this.representative.getContentTypeFacet();
    }

    @Override
    public List<Datastream> getDatastreamObjects() {
        return this.representative.getDatastreamObjects();
    }

    @Override
    public Datastream getDatastreamObject(String datastreamName) {
        return this.representative.getDatastreamObject(datastreamName);
    }

    @Override
    public Map<String, Collection<String>> getGroupRoleMap() {
        return this.representative.getGroupRoleMap();
    }

    @Override
    public String getParentCollectionName() {
        return this.representative.getParentCollectionName();
    }

    public String getParentCollectionId() {
        return this.representative.getParentCollectionId();
    }

    @Override
    public String getCollectionId() {
        return this.representative.getCollectionId();
    }

    @Override
    public void setCountMap(Map<String,Long> countMap) {
        this.representative.setCountMap(countMap);
    }

    @Override
    public Map<String,Long> getCountMap() {
        return this.representative.getCountMap();
    }

    @Override
    public String getId() {
        return this.representative.getId();
    }

    @Override
    public PID getPid() {
        return this.representative.getPid();
    }

    @Override
    public List<String> getAncestorPath() {
        return this.representative.getAncestorPath();
    }

    @Override
    public String getAncestorNames() {
        return this.representative.getAncestorNames();
    }

    @Override
    public String getParentCollection() {
        return this.representative.getParentCollection();
    }

    @Override
    public String getRollup() {
        return this.representative.getRollup();
    }

    @Override
    public Long get_version_() {
        return this.representative.get_version_();
    }

    @Override
    public List<String> getDatastream() {
        return this.representative.getDatastream();
    }

    @Override
    public Long getFilesizeSort() {
        return this.representative.getFilesizeSort();
    }

    @Override
    public Long getFilesizeTotal() {
        return this.representative.getFilesizeTotal();
    }

    @Override
    public String getResourceType() {
        return this.representative.getResourceType();
    }

    @Override
    public List<String> getPublisher() {
        return this.representative.getPublisher();
    }

    @Override
    public Integer getResourceTypeSort() {
        return this.representative.getResourceTypeSort();
    }

    @Override
    public List<String> getFileFormatCategory() {
        return this.representative.getFileFormatCategory();
    }

    @Override
    public List<String> getFileFormatType() {
        return this.representative.getFileFormatType();
    }

    @Override
    public List<String> getFileFormatDescription() {
        return this.representative.getFileFormatDescription();
    }

    @Override
    public Date getTimestamp() {
        return this.representative.getTimestamp();
    }

    @Override
    public Date getLastIndexed() {
        return this.representative.getLastIndexed();
    }

    @Override
    public List<String> getRoleGroup() {
        return this.representative.getRoleGroup();
    }

    @Override
    public List<String> getReadGroup() {
        return this.representative.getReadGroup();
    }

    @Override
    public List<String> getAdminGroup() {
        return this.representative.getAdminGroup();
    }

    @Override
    public List<String> getStatus() {
        return this.representative.getStatus();
    }

    @Override
    public List<String> getContentStatus() {
        return this.representative.getContentStatus();
    }

    @Override
    public List<String> getIdentifier() {
        return this.representative.getIdentifier();
    }

    @Override
    public String getTitle() {
        return this.representative.getTitle();
    }

    @Override
    public List<String> getOtherTitle() {
        return this.representative.getOtherTitle();
    }

    @Override
    public String getAbstractText() {
        return this.representative.getAbstractText();
    }

    @Override
    public List<String> getKeyword() {
        return this.representative.getKeyword();
    }

    @Override
    public List<String> getExhibit() {
        return this.representative.getExhibit();
    }

    @Override
    public List<String> getSubject() {
        return this.representative.getSubject();
    }

    @Override
    public List<String> getLanguage() {
        return this.representative.getLanguage();
    }

    @Override
    public List<String> getLocation() {
        return this.representative.getLocation();
    }

    @Override
    public List<String> getGenre() {
        return this.representative.getGenre();
    }

    @Override
    public List<String> getCreator() {
        return this.representative.getCreator();
    }

    @Override
    public List<String> getContributor() {
        return this.representative.getContributor();
    }

    @Override
    public List<String> getCreatorContributor() {
        return this.representative.getCreatorContributor();
    }

    @Override
    public Date getDateCreated() {
        return this.representative.getDateCreated();
    }

    @Override
    public String getDateCreatedYear() {
        return this.representative.getDateCreatedYear();
    }

    @Override
    public Date getDateAdded() {
        return this.representative.getDateAdded();
    }

    @Override
    public Date getDateUpdated() {
        return this.representative.getDateUpdated();
    }

    @Override
    public String getCitation() {
        return this.representative.getCitation();
    }

    @Override
    public String getFullText() {
        return this.representative.getFullText();
    }

    @Override
    public Map<String, Object> getDynamicFields() {
        return this.representative.getDynamicFields();
    }

    @Override
    public ObjectPath getObjectPath() {
        return this.representative.getObjectPath();
    }

    @Override
    public void setObjectPath(ObjectPath objectPath) {
        this.representative.setObjectPath(objectPath);
    }

    @Override
    public String getAncestorIds() {
        return representative.getAncestorIds();
    }

    @Override
    public String getIdentifierSort() {
        return representative.getIdentifierSort();
    }

    @Override
    public Integer getMemberOrderId() {
        return representative.getMemberOrderId();
    }

    @Override
    public String getThumbnailId() {
        return representative.getThumbnailId();
    }

    @Override
    public void setThumbnailId(String id) {
        representative.setThumbnailId(id);
    }

    @Override
    public String getViewBehavior() {
        return representative.getViewBehavior();
    }
}
