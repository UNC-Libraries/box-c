package edu.unc.lib.dl.search.solr.model;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class GroupedMetadataBean implements BriefObjectMetadata {
	private List<BriefObjectMetadataBean> items;
	private BriefObjectMetadataBean representative;
	private Long itemCount;
	private String groupId;

	public GroupedMetadataBean(String groupId, List<BriefObjectMetadataBean> items, Long itemCount) {
		this.items = items;
		this.itemCount = itemCount;
		this.groupId = groupId;
		for (BriefObjectMetadataBean item : items) {
			if (this.groupId.equals(item.getRollup())) {
				this.representative = item;
				break;
			}
		}
	}

	public BriefObjectMetadataBean getRepresentative() {
		return representative;
	}

	public void setRepresentative(BriefObjectMetadataBean representative) {
		this.representative = representative;
	}

	public List<BriefObjectMetadataBean> getItems() {
		return items;
	}

	public Long getItemCount() {
		return itemCount;
	}

	public String getGroupId() {
		return groupId;
	}

	@Override
	public String getIdWithoutPrefix() {
		return this.representative.getIdWithoutPrefix();
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
	public List<MultivaluedHierarchicalFacet> getContentTypeFacet() {
		return this.representative.getContentTypeFacet();
	}

	@Override
	public List<Datastream> getDatastreamObjects() {
		return this.representative.getDatastreamObjects();
	}

	@Override
	public Map<String, Collection<String>> getGroupRoleMap() {
		return this.representative.getGroupRoleMap();
	}

	@Override
	public CutoffFacetNode getParentCollectionObject() {
		return this.representative.getParentCollectionObject();
	}

	public void setChildCount(Long childCount) {
		this.representative.setChildCount(childCount);
	}
	
	@Override
	public long getChildCount() {
		return this.representative.getChildCount();
	}

	@Override
	public String getId() {
		return this.representative.getId();
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
	public List<String> getScope() {
		return this.representative.getScope();
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
	public List<String> getRelations() {
		return this.representative.getRelations();
	}

	@Override
	public List<String> getContentModel() {
		return this.representative.getContentModel();
	}

	@Override
	public String getResourceType() {
		return this.representative.getResourceType();
	}

	@Override
	public Integer getResourceTypeSort() {
		return this.representative.getResourceTypeSort();
	}

	@Override
	public String getCreatorSort() {
		return this.representative.getCreatorSort();
	}

	@Override
	public String getDefaultSortType() {
		return this.representative.getDefaultSortType();
	}

	@Override
	public Long getDisplayOrder() {
		return this.representative.getDisplayOrder();
	}

	@Override
	public List<String> getContentType() {
		return this.representative.getContentType();
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
	public List<String> getSubject() {
		return this.representative.getSubject();
	}

	@Override
	public List<String> getLanguage() {
		return this.representative.getLanguage();
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
	public List<String> getDepartment() {
		return this.representative.getDepartment();
	}

	@Override
	public Date getDateCreated() {
		return this.representative.getDateCreated();
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
}
