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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.exceptions.SolrRuntimeException;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.web.common.utils.DatastreamUtil;

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;

/**
 * Service to check for or list resources with access copies
 *
 * @author bbpennel
 */
public class AccessCopiesService extends SolrSearchService {
    private static final int MAX_FILES = 2000;
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    /**
     * List viewable files for the specified object
     * @param pid
     * @param principals
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<ContentObjectRecord> listViewableFiles(PID pid, AccessGroupSet principals) {
        ContentObjectRecord briefObj = getObjectById(new SimpleIdRequest(pid, principals));
        String resourceType = briefObj.getResourceType();
        if (searchSettings.resourceTypeFile.equals(resourceType)) {
            if (briefObj.getDatastreamObject(DatastreamType.JP2_ACCESS_COPY.getId()) != null) {
                return Collections.singletonList(briefObj);
            } else {
                return Collections.emptyList();
            }
        }
        if (!searchSettings.resourceTypeAggregate.equals(resourceType)) {
            return Collections.emptyList();
        }

        QueryResponse resp = performQuery(briefObj, principals, MAX_FILES);
        List<?> results = resp.getBeans(ContentObjectSolrRecord.class);
        List<ContentObjectRecord> mdObjs = (List<ContentObjectRecord>) results;
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
        if (searchSettings.resourceTypeFile.equals(resourceType)) {
            Datastream datastream = briefObj.getDatastreamObject(DatastreamType.JP2_ACCESS_COPY.getId());
            return datastream != null;
        }
        if (!searchSettings.resourceTypeAggregate.equals(resourceType)) {
            return false;
        }

        QueryResponse resp = performQuery(briefObj, principals, 0);
        return resp.getResults().getNumFound() > 0;
    }

    /**
     * Returns true if the file is a pdf and is the primary object or the object has no primary object
     * @param workObjs
     * @return
     */
    public boolean pdfViewerNeeded(List<ContentObjectRecord> workObjs) {
        if (workObjs == null) {
            return false;
        }

        // Check first object
        ContentObjectRecord workObj = workObjs.get(0);
        if (hasPdf(workObj) && workObjs.size() == 1) {
            return true;
        }

        workObjs.remove(0);

        // Check any other objects
        boolean hasPdfPrimaryObj = false;
        for (ContentObjectRecord childObj: workObjs) {
            if (!hasPdf(childObj)) {
                continue;
            }

            List<String> primaryObj = childObj.getRelation(Cdr.primaryObject.getURI());
            if (primaryObj == null) {
                continue;
            }

            // True if PDF is primary object
            String primaryObjUUID = primaryObj.get(0);
            String origFileIdentifier = childObj.getDatastreamObject(ORIGINAL_FILE.getId()).getDatastreamIdentifier();
            if (origFileIdentifier.startsWith(primaryObjUUID)) {
                hasPdfPrimaryObj = true;
                break;
            }
        }

        return hasPdfPrimaryObj;
    }

    private boolean hasPdf(ContentObjectRecord contentObj) {
        return DatastreamUtil.originalFileMimetypeMatches(contentObj, "application/(x-)?pdf");
    }

    private QueryResponse performQuery(ContentObjectRecord briefObj, AccessGroupSet principals, int rows) {
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

        SearchRequest searchRequest = new SearchRequest(searchState, principals);
        SolrQuery query = generateSearch(searchRequest);
        query.addFilterQuery(solrSettings.getFieldName(SearchFieldKey.DATASTREAM.name()) + ":"
                + DatastreamType.JP2_ACCESS_COPY.getId() + "|*");

        try {
            QueryResponse resp = executeQuery(query);
            return resp;
        } catch (SolrServerException e) {
            throw new SolrRuntimeException("Error listing viewable files: " + query, e);
        }
    }

    public void setGlobalPermissionEvaluator(GlobalPermissionEvaluator globalPermissionEvaluator) {
        this.globalPermissionEvaluator = globalPermissionEvaluator;
    }
}
