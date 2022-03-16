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

    public String getAncestorNames();

    public String getAncestorIds();

    public String getParentCollectionName();

    public String getParentCollection();

    public String getRollup();

    public Long get_version_();

    public List<String> getDatastream();

    public Long getFilesizeSort();

    public Long getFilesizeTotal();

    public List<String> getRelations();

    public List<String> getRelation(String relationName);

    public Boolean getIsPart();

    public List<String> getContentModel();

    public String getResourceType();

    public Integer getResourceTypeSort();

    public String getCreatorSort();

    public Long getDisplayOrder();

    public List<String> getContentType();

    public Date getTimestamp();

    public Date getLastIndexed();

    public List<String> getRoleGroup();

    public List<String> getReadGroup();

    public List<String> getAdminGroup();

    public List<String> getStatus();

    public List<String> getContentStatus();

    public List<String> getIdentifier();

    public String getIdentifierSort();

    public String getLabel();

    public String getTitle();

    public List<String> getOtherTitle();

    public String getAbstractText();

    public List<String> getKeyword();

    public List<String> getSubject();

    public List<String> getLocation();

    public List<String> getGenre();

    public List<String> getLanguage();

    public List<String> getCreator();

    public List<String> getContributor();

    public List<String> getPublisher();

    public List<String> getDepartment();

    public Date getDateCreated();

    public String getDateCreatedYear();

    public Date getDateAdded();

    public Date getDateUpdated();

    public String getCitation();

    public String getFullText();

    public Map<String,Object> getDynamicFields();

    public Date getActiveEmbargo();

    String getCollectionId();

    /**
     * @return ID of the object holding the thumbnail for this object, if one is present. Otherwise, null.
     */
    String getThumbnailId();

    void setThumbnailId(String id);
}
