package edu.unc.lib.boxc.search.api.models;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.facets.HierarchicalFacet;

/**
 * An object containing the metadata for a repository object
 *
 * @author bbpennel
 *
 */
public interface ContentObjectRecord {
    public CutoffFacet getAncestorPathFacet();

    public ObjectPath getObjectPath();

    public void setObjectPath(ObjectPath objectPath);

    public CutoffFacet getPath();

    public List<HierarchicalFacet> getContentTypeFacet();

    public List<Datastream> getDatastreamObjects();

    public Datastream getDatastreamObject(String datastreamName);

    public Map<String, Collection<String>> getGroupRoleMap();

    public void setCountMap(Map<String,Long> countMap);

    public Map<String,Long> getCountMap();

    public String getId();

    public PID getPid();

    public List<String> getAncestorPath();

    public String getAncestorIds();

    public String getParentCollectionName();

    public String getParentCollection();

    public String getParentCollectionId();

    public String getRollup();

    public Long get_version_();

    public List<String> getDatastream();

    public Long getFilesizeSort();

    public Long getFilesizeTotal();

    public String getResourceType();

    public Integer getResourceTypeSort();

    public List<String> getFileFormatCategory();

    public List<String> getFileFormatType();

    public List<String> getFileFormatDescription();

    public Date getTimestamp();

    public Date getLastIndexed();

    public List<String> getRoleGroup();

    public List<String> getReadGroup();

    public List<String> getAdminGroup();

    public List<String> getStatus();

    public List<String> getContentStatus();

    public List<String> getIdentifier();

    public String getIdentifierSort();

    public Integer getMemberOrderId();

    public String getTitle();

    public String getAncestorNames();

    public List<String> getOtherTitle();

    public String getAbstractText();

    public List<String> getKeyword();

    public List<String> getExhibit();

    public List<String> getSubject();

    public String getStreamingUrl();

    public List<String> getLocation();

    public List<String> getGenre();

    public List<String> getLanguage();

    public List<String> getCreator();

    public List<String> getContributor();

    public List<String> getCreatorContributor();

    public List<String> getPublisher();

    public Date getDateCreated();

    public String getDateCreatedYear();

    public Date getDateAdded();

    public Date getDateUpdated();

    public String getCitation();

    public String getFullText();

    public Map<String,Object> getDynamicFields();

    String getCollectionId();

    /**
     * @return ID of the object holding the thumbnail for this object, if one is present. Otherwise, null.
     */
    String getThumbnailId();

    void setThumbnailId(String id);

    String getViewBehavior();
}
