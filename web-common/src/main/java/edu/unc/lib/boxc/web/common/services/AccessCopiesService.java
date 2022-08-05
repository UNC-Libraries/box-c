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
package edu.unc.lib.boxc.web.common.services;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.ContentCategory;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.filters.QueryFilterFactory;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.web.common.utils.DatastreamUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Service to check for or list resources with access copies
 *
 * @author bbpennel
 */
public class AccessCopiesService {
    private static final Logger log = LoggerFactory.getLogger(AccessCopiesService.class);

    private static final int MAX_FILES = 2000;
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    private PermissionsHelper permissionsHelper;
    private SolrSearchService solrSearchService;
    public static final String AUDIO_MIMETYPE_REGEX = "audio/(x-)?mpeg(-?3)?";
    public static final String PDF_MIMETYPE_REGEX = "application/(x-)?pdf";

    /**
     * List viewable files for the specified object
     * @param pid
     * @param principals
     * @return
     */
    public List<ContentObjectRecord> listViewableFiles(PID pid, AccessGroupSet principals) {
        ContentObjectRecord briefObj = solrSearchService.getObjectById(new SimpleIdRequest(pid, principals));
        String resourceType = briefObj.getResourceType();
        if (ResourceType.File.nameEquals(resourceType)) {
            if (briefObj.getDatastreamObject(DatastreamType.JP2_ACCESS_COPY.getId()) != null) {
                return Collections.singletonList(briefObj);
            } else {
                return Collections.emptyList();
            }
        }
        if (!ResourceType.Work.nameEquals(resourceType)) {
            return Collections.emptyList();
        }

        var resp = performQuery(briefObj, principals, MAX_FILES);
        List<ContentObjectRecord> mdObjs = resp.getResultList();
        mdObjs.add(0, briefObj);
        return mdObjs;
    }

    /**
     * Returns true if the object contains files that may be displayed in the viewer
     *
     * @param briefObj object to check, should be either a Work or File object
     * @param principals
     * @return
     */
    public boolean hasViewableFiles(ContentObjectRecord briefObj, AccessGroupSet principals) {
        String resourceType = briefObj.getResourceType();
        if (ResourceType.File.nameEquals(resourceType)) {
            Datastream datastream = briefObj.getDatastreamObject(DatastreamType.JP2_ACCESS_COPY.getId());
            return datastream != null;
        }
        if (!ResourceType.Work.nameEquals(resourceType)) {
            return false;
        }

        var resp = performQuery(briefObj, principals, 0);
        return resp.getResultCount() > 0;
    }

    /**
     * Retrieves the ID of the owner of the original file for the provided object, if the mimetype of the
     * file matches the provided regular expression pattern. If there is no matching original file, null is returned.
     * @param briefObj
     * @param principals
     * @param regxPattern
     * @return String
     */
    public String getDatastreamPid(ContentObjectRecord briefObj, AccessGroupSet principals, String regxPattern) {
        if (permissionsHelper.hasOriginalAccess(principals, briefObj) &&
                DatastreamUtil.originalFileMimetypeMatches(briefObj, regxPattern)) {
            var ds = briefObj.getDatastreamObject(DatastreamType.ORIGINAL_FILE.getId());
            return StringUtils.isEmpty(ds.getOwner()) ? briefObj.getId() : ds.getOwner();
        }
        return null;
    }

    /**
     * Get the path of the original_file datastream within contentObjectRecord that can be downloaded,
     * or an empty string if no appropriate original_file is present
     * @param contentObjectRecord
     * @param principals
     * @return
     */
    public String getDownloadUrl(ContentObjectRecord contentObjectRecord, AccessGroupSet principals) {
        if (contentObjectRecord == null || !permissionsHelper.hasOriginalAccess(principals, contentObjectRecord)) {
            return "";
        }
        return DatastreamUtil.getOriginalFileUrl(contentObjectRecord);
    }

    private static final String IMAGE_CONTENT_TYPE = ContentCategory.image.getDisplayName();

    /**
     * @param contentObjectRecord
     * @param principals
     * @param checkChildren if true, then in cases where it is ambiguous if the provided record has a thumbnail,
     *             then additional queries will be performed to check.
     * @return ID of the object the thumbnail for the provided object belongs to, or null if there is no thumbnail
     */
    public String getThumbnailId(ContentObjectRecord contentObjectRecord, AccessGroupSet principals,
                                 boolean checkChildren) {
        // Find thumbnail datastream recorded directly on the object, if present
        var thumbId = DatastreamUtil.getThumbnailOwnerId(contentObjectRecord);
        if (thumbId != null) {
            log.debug("Found thumbnail object directly assigned to object {}", thumbId);
            return thumbId;
        }
        // Don't need to check any further if object isn't a work or doesn't contain files with thumbnails
        if (!ResourceType.Work.name().equals(contentObjectRecord.getResourceType())
                || contentObjectRecord.getFileFormatCategory() == null
                || !contentObjectRecord.getFileFormatCategory().contains(IMAGE_CONTENT_TYPE)) {
            log.debug("Record {} is not applicable for a thumbnail", contentObjectRecord.getId());
            return null;
        }
        if (!checkChildren) {
            log.debug("Not checking children for work {}, so using self as thumbnail id", contentObjectRecord.getId());
            return contentObjectRecord.getId();
        }

        var request = buildFirstChildQuery(contentObjectRecord, principals);
        // Limit query to just children which have a thumbnail datastream
        var searchState = request.getSearchState();
        searchState.getFilters().add(
                QueryFilterFactory.createFilter(SearchFieldKey.DATASTREAM, DatastreamType.THUMBNAIL_LARGE));

        var resp = solrSearchService.getSearchResults(request);
        if (resp.getResultCount() > 0) {
            var id = resp.getResultList().get(0).getId();
            log.debug("Found thumbnail object {} for work {}", id, contentObjectRecord.getId());
            return id;
        } else {
            log.debug("No thumbnail objects for work {}", contentObjectRecord.getId());
            return null;
        }
    }

    public void populateThumbnailId(ContentObjectRecord record, AccessGroupSet principals,
                                    boolean checkChildren) {
        if (record == null) {
            return;
        }
        record.setThumbnailId(getThumbnailId(record, principals, checkChildren));
    }

    public void populateThumbnailIds(List<ContentObjectRecord> records, AccessGroupSet principals,
                                     boolean checkChildren) {
        for (var record : records) {
            record.setThumbnailId(getThumbnailId(record, principals, checkChildren));
        }
    }

    private SearchRequest buildFirstChildQuery(ContentObjectRecord briefObj, AccessGroupSet principals) {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(null);
        searchState.setRowsPerPage(1);
        searchState.setSortType("default");
        CutoffFacet selectedPath = briefObj.getPath();
        searchState.addFacet(selectedPath);
        SearchRequest searchRequest = new SearchRequest(searchState, principals);
        searchRequest.setApplyCutoffs(true);
        return searchRequest;
    }

    private SearchResultResponse performQuery(ContentObjectRecord briefObj, AccessGroupSet principals, int rows) {
        // Search for child objects with jp2 datastreams with user can access
        SearchState searchState = new SearchState();
        if (!globalPermissionEvaluator.hasGlobalPrincipal(principals)) {
            searchState.setPermissionLimits(Arrays.asList(Permission.viewAccessCopies));
        }
        searchState.setIgnoreMaxRows(true);
        searchState.setRowsPerPage(rows);
        CutoffFacet selectedPath = briefObj.getPath();
        searchState.addFacet(selectedPath);
        searchState.setSortType("default");
        searchState.getFilters().add(
                QueryFilterFactory.createFilter(SearchFieldKey.DATASTREAM, DatastreamType.JP2_ACCESS_COPY));

        var searchRequest = new SearchRequest(searchState, principals);
        return solrSearchService.getSearchResults(searchRequest);
    }

    public void setGlobalPermissionEvaluator(GlobalPermissionEvaluator globalPermissionEvaluator) {
        this.globalPermissionEvaluator = globalPermissionEvaluator;
    }

    public void setPermissionsHelper(PermissionsHelper permissionsHelper) {
        this.permissionsHelper = permissionsHelper;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }
}
