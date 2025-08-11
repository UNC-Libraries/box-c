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
    CutoffFacet getAncestorPathFacet();

    ObjectPath getObjectPath();

    void setObjectPath(ObjectPath objectPath);

    CutoffFacet getPath();

    List<HierarchicalFacet> getContentTypeFacet();

    List<Datastream> getDatastreamObjects();

    Datastream getDatastreamObject(String datastreamName);

    Map<String, Collection<String>> getGroupRoleMap();

    void setCountMap(Map<String,Long> countMap);

    Map<String,Long> getCountMap();

    String getId();

    PID getPid();

    List<String> getAncestorPath();

    String getAncestorIds();

    String getParentCollectionName();

    String getParentCollection();

    String getParentCollectionId();

    String getRollup();

    Long get_version_();

    List<String> getDatastream();

    Long getFilesizeSort();

    Long getFilesizeTotal();

    String getResourceType();

    Integer getResourceTypeSort();

    List<String> getFileFormatCategory();

    List<String> getFileFormatType();

    List<String> getFileFormatDescription();

    Date getTimestamp();

    Date getLastIndexed();

    List<String> getRoleGroup();

    List<String> getReadGroup();

    List<String> getAdminGroup();

    List<String> getStatus();

    List<String> getContentStatus();

    List<String> getIdentifier();

    String getIdentifierSort();

    Integer getMemberOrderId();

    String getTitle();

    String getAncestorNames();

    List<String> getOtherTitle();

    String getAbstractText();

    List<String> getKeyword();

    List<String> getExhibit();

    List<String> getSubject();

    String getStreamingType();

    String getStreamingUrl();

    List<String> getLocation();

    List<String> getGenre();

    List<String> getLanguage();

    List<String> getCreator();

    List<String> getContributor();

    List<String> getCreatorContributor();

    List<String> getPublisher();

    Date getDateCreated();

    String getDateCreatedYear();

    Date getDateAdded();

    Date getDateUpdated();

    String getCitation();

    String getFullText();

    Map<String,Object> getDynamicFields();

    String getCollectionId();

    String getAspaceRefId();
    String getHookId();

    /**
     * @return ID of the object holding the thumbnail for this object, if one is present. Otherwise, null.
     */
    String getThumbnailId();

    void setThumbnailId(String id);

    String getViewBehavior();

    /**
     * @return The alt text for this object, if one is present.
     */
    String getAltText();

    void setAltText(String altText);

   /**
    * @return the checksum on the OriginalFile Datastream, if one is present
    */
   String getChecksum();
}
