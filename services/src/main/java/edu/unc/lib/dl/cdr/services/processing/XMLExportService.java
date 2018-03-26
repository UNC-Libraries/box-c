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
package edu.unc.lib.dl.cdr.services.processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.cdr.services.rest.modify.ExportXMLController.XMLExportRequest;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;
import edu.unc.lib.persist.services.EmailHandler;

/**
 * A service that starts a job for exporting the XML metadata of specified objects
 *
 * @author harring
 *
 */
public class XMLExportService {
    private SearchStateFactory searchStateFactory;
    private SolrQueryLayerService queryLayer;
    private EmailHandler emailHandler;
    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private boolean asynchronous;

    private final List<String> resultFields = Arrays.asList(SearchFieldKeys.ID.name());

    /**
     * Determines whether metadata for child objects should be exported, and then kicks off the job
     * @param username
     * @param group
     * @param request
     * @return
     * @throws ServiceException
     */
    public void exportXml(String username, AccessGroupSet group, XMLExportRequest request)
            throws ServiceException {
        if (username == null) {
            throw new AccessRestrictionException("User must have a username to export xml");
        }
        if (request.getPids() == null || request.getPids().isEmpty()) {
            throw new IllegalArgumentException("At least one PID is required for exporting");
        }
        if (request.getExportChildren()) {
            addChildPIDsToRequest(request);
        }

        XMLExportJob job = new XMLExportJob(username, group, request);
        job.setAclService(aclService);
        job.setEmailHandler(emailHandler);
        job.setRepoObjLoader(repoObjLoader);

        if (asynchronous) {
            Thread thread = new Thread(job);
            thread.start();
        } else {
            job.run();
        }

    }

    private void addChildPIDsToRequest(XMLExportRequest request) throws ServiceException {
        List<String> pids = new ArrayList<>();
        for (String pid : request.getPids()) {
            SearchState searchState = searchStateFactory.createSearchState();
            searchState.setResultFields(resultFields);
            searchState.setSortType("export");
            searchState.setRowsPerPage(Integer.MAX_VALUE);

            SearchRequest searchRequest = new SearchRequest(searchState, GroupsThreadStore.getGroups());
            SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);
            if (resultResponse == null) {
                throw new ServiceException("An error occurred while retrieving children of " + pid + " for export.");
            }

            // Add back in the parent pid
            pids.add(pid);

            List<BriefObjectMetadata> objects = resultResponse.getResultList();
            for (BriefObjectMetadata object : objects) {
                pids.add(object.getPid().toString());
            }
        }
        // update the list of pids in the request with all of the child pids found
        request.setPids(pids);
    }

    public SearchStateFactory getSearchStateFactory() {
        return searchStateFactory;
    }

    public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
        this.searchStateFactory = searchStateFactory;
    }

    public SolrQueryLayerService getQueryLayer() {
        return queryLayer;
    }

    public void setQueryLayer(SolrQueryLayerService queryLayer) {
        this.queryLayer = queryLayer;
    }

    public EmailHandler getEmailHandler() {
        return emailHandler;
    }

    public void setEmailHandler(EmailHandler emailHandler) {
        this.emailHandler = emailHandler;
    }

    public AccessControlService getAclService() {
        return aclService;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public RepositoryObjectLoader getRepoObjLoader() {
        return repoObjLoader;
    }

    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    /**
     * @param asynchronous the asynchronous to set
     */
    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }
}
