package edu.unc.lib.dl.search.solr.model;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface BriefObjectMetadata {
	public String getIdWithoutPrefix() ;

	public CutoffFacet getAncestorPathFacet();
	
	public CutoffFacet getPath();

	public List<MultivaluedHierarchicalFacet> getContentTypeFacet();

	public List<Datastream> getDatastreamObjects();

	public Map<String, Collection<String>> getGroupRoleMap();

	public CutoffFacetNode getParentCollectionObject();

	public void setChildCount(Long childCount);
	
	public long getChildCount();

	public String getId();
	
	public List<String> getAncestorPath();

	public String getAncestorNames();

	public String getParentCollection();

	public List<String> getScope();

	public String getRollup();

	public Long get_version_();

	public List<String> getDatastream();

	public Long getFilesizeSort();

	public Long getFilesizeTotal();

	public List<String> getRelations();

	public List<String> getContentModel();

	public String getResourceType();

	public Integer getResourceTypeSort();

	public String getCreatorSort();

	public String getDefaultSortType();

	public Long getDisplayOrder();

	public List<String> getContentType();

	public Date getTimestamp();

	public Date getLastIndexed();

	public List<String> getRoleGroup();

	public List<String> getReadGroup();

	public List<String> getAdminGroup();

	public List<String> getStatus();

	public List<String> getIdentifier();

	public String getTitle();

	public List<String> getOtherTitle();

	public String getAbstractText();

	public List<String> getKeyword();

	public List<String> getSubject();

	public List<String> getLanguage();

	public List<String> getCreator();

	public List<String> getContributor();

	public List<String> getDepartment();

	public Date getDateCreated();

	public Date getDateAdded();

	public Date getDateUpdated();

	public String getCitation();

	public String getFullText();
}
