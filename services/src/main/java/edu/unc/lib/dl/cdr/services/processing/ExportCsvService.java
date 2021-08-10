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

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewOriginals;
import static edu.unc.lib.boxc.auth.api.UserRole.none;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_ROOT_ID;
import static edu.unc.lib.dl.search.solr.util.FacetConstants.MARKED_FOR_DELETION;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.ChildrenCountService;
import edu.unc.lib.dl.search.solr.util.FacetConstants;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

/**
 * Service which outputs a CSV listing of a repository object and all of its children,
 * recursively depth first
 *
 * @author bbpennel
 */
public class ExportCsvService {
    private static final Logger log = LoggerFactory.getLogger(ExportCsvService.class);

    private static final int DEFAULT_PAGE_SIZE = 10000;

    public static final String OBJ_TYPE_HEADER = "Object Type";
    public static final String PID_HEADER = "PID";
    public static final String TITLE_HEADER = "Title";
    public static final String PATH_HEADER = "Path";
    public static final String LABEL_HEADER = "Label";
    public static final String DEPTH_HEADER = "Depth";
    public static final String DELETED_HEADER = "Deleted";
    public static final String DATE_ADDED_HEADER = "Date Added";
    public static final String DATE_UPDATED_HEADER = "Date Updated";
    public static final String MIME_TYPE_HEADER = "MIME Type";
    public static final String CHECKSUM_HEADER = "Checksum";
    public static final String FILE_SIZE_HEADER = "File Size (bytes)";
    public static final String NUM_CHILDREN_HEADER = "Number of Children";
    public static final String DESCRIBED_HEADER = "Description";
    public static final String PATRON_PERMISSIONS_HEADER = "Patron Permissions";
    public static final String EMBARGO_HEADER = "Embargoed";

    private static final String[] CSV_HEADERS = new String[] {
            OBJ_TYPE_HEADER, PID_HEADER, TITLE_HEADER, PATH_HEADER, LABEL_HEADER,
            DEPTH_HEADER, DELETED_HEADER, DATE_ADDED_HEADER, DATE_UPDATED_HEADER,
            MIME_TYPE_HEADER, CHECKSUM_HEADER, FILE_SIZE_HEADER, NUM_CHILDREN_HEADER,
            DESCRIBED_HEADER, PATRON_PERMISSIONS_HEADER, EMBARGO_HEADER};

    private static final List<String> SEARCH_FIELDS = Arrays.asList(SearchFieldKeys.ID.name(),
            SearchFieldKeys.TITLE.name(),
            SearchFieldKeys.RESOURCE_TYPE.name(), SearchFieldKeys.ANCESTOR_IDS.name(),
            SearchFieldKeys.STATUS.name(), SearchFieldKeys.DATASTREAM.name(),
            SearchFieldKeys.ANCESTOR_PATH.name(), SearchFieldKeys.CONTENT_MODEL.name(),
            SearchFieldKeys.DATE_ADDED.name(), SearchFieldKeys.DATE_UPDATED.name(),
            SearchFieldKeys.LABEL.name(), SearchFieldKeys.CONTENT_STATUS.name(),
            SearchFieldKeys.ROLE_GROUP.name());

    private ChildrenCountService childrenCountService;
    private AccessControlService aclService;
    private SolrQueryLayerService queryLayer;
    private int pageSize = DEFAULT_PAGE_SIZE;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

    public void streamCsv(PID pid, AgentPrincipals agent, OutputStream out) {
        if (CONTENT_ROOT_ID.equals(pid.getId())) {
            throw new IllegalArgumentException("Error exporting CSV for " + pid.getId()
                    + ". Not allowed to export collection root");
        }

        AccessGroupSet accessGroups = agent.getPrincipals();
        aclService.assertHasAccess("Insufficient privileges to export CSV for " + pid.getUUID(),
                pid, accessGroups, viewHidden);

        log.debug("Streaming CSV export of {} for {}", pid, agent.getUsername());

        SearchState searchState = new SearchState();
        searchState.setResultFields(SEARCH_FIELDS);
        searchState.setSortType("export");
        searchState.setRowsPerPage(pageSize);

        ContentObjectRecord container = queryLayer.addSelectedContainer(pid, searchState, false,
                accessGroups);
        if (container == null) {
            throw new NotFoundException("Object " + pid.getId() + " not found while streaming CSV export");
        }

        SearchRequest searchRequest = new SearchRequest(searchState, accessGroups);
        searchRequest.setRootPid(pid);
        searchRequest.setApplyCutoffs(false);

        // Open the CSV
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        try (CSVPrinter printer = getPrinter(writer)) {
            int pageStart = 0;
            long totalResults = -1;
            do {
                searchState.setStartRow(pageStart);

                SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);

                List<ContentObjectRecord> objects = resultResponse.getResultList();
                // Insert the parent container if on the first page of results
                if (pageStart == 0) {
                    objects.add(0, container);
                    totalResults = resultResponse.getResultCount();
                } else {
                    log.debug("Streaming results {}-{} of {} in multi-page result for {}",
                            pageStart, pageStart + pageSize, totalResults, pid.getId());
                }

                childrenCountService.addChildrenCounts(objects, searchRequest.getAccessGroups());

                // Stream the current page of results
                for (ContentObjectRecord object : objects) {
                    printObject(printer, object);
                }

                pageStart += pageSize;
            } while (pageStart < totalResults);
        } catch (IOException e) {
            throw new RepositoryException("Failed to stream CSV results for " + pid, e);
        }
    }

    private String computePatronPermissions(List<String> roles) {
        if (roles == null) {
            return "Staff-only";
        }

        Map<String, String> roleList = new HashMap<>();
        for (String role : roles) {
            if (StringUtils.isBlank(role)) {
                continue;
            }
            String[] principalRole = role.split("\\|");
            roleList.put(principalRole[1], principalRole[0]);
        }

        String everyoneRole = roleList.get(PUBLIC_PRINC);
        String authenticatedRole = roleList.get(AUTHENTICATED_PRINC);
        String permission;

        if (canViewOriginals(everyoneRole)) {
            permission = "Public";
        } else if (canViewOriginals(authenticatedRole) && hasNoAccess(everyoneRole)) {
            permission = "Authenticated";
        } else if (hasNoAccess(everyoneRole) && hasNoAccess(authenticatedRole)) {
            permission = "Staff-only";
        } else {
            permission = "Restricted";
        }

        return permission;
    }

    private boolean canViewOriginals(String role) {
        return role != null && role.equals(canViewOriginals.getPredicate());
    }

    private boolean hasNoAccess(String role) {
        return role == null || role.equals(none.getPredicate());
    }

    private CSVPrinter getPrinter(Writer writer) throws IOException {
        return CSVFormat.EXCEL.withHeader(CSV_HEADERS).print(writer);
    }

    private void printObject(CSVPrinter printer, ContentObjectRecord object) throws IOException {
        // Vitals: object type, pid, title, path, label, depth
        printer.print(object.getResourceType());
        printer.print(object.getId());
        printer.print(object.getTitle());
        printer.print(object.getAncestorNames());

        String label = object.getLabel();

        if (label != null) {
            printer.print(label);
        } else {
            printer.print("");
        }

        printer.print(object.getAncestorPathFacet().getHighestTier());

        // Status: deleted

        if (object.getStatus() != null) {
            printer.print(object.getStatus().contains(MARKED_FOR_DELETION));
        } else {
            printer.print("");
        }


        // Dates: added, updated

        Date added = object.getDateAdded();

        if (added != null) {
            printer.print(dateFormat.format(added));
        } else {
            printer.print("");
        }

        Date updated = object.getDateUpdated();

        if (updated != null) {
            printer.print(dateFormat.format(updated));
        } else {
            printer.print("");
        }

        // DATA_FILE info: mime type, checksum, file size
        Datastream dataFileDatastream = object.getDatastreamObject(ORIGINAL_FILE.getId());

        if (dataFileDatastream != null) {
            printer.print(dataFileDatastream.getMimetype());
            printer.print(dataFileDatastream.getChecksum().replace("urn:", ""));

            Long filesize = dataFileDatastream.getFilesize();

            // If we don't have a filesize for whatever reason, print a blank
            if (filesize != null && filesize >= 0) {
                printer.print(filesize);
            } else {
                printer.print("");
            }
        } else {
            printer.print("");
            printer.print("");
            printer.print("");
        }

        // Container info: child count
        Long childCount = object.getCountMap().get("child");
        if (childCount != null && childCount > 0) {
            printer.print(childCount.toString());
        } else {
            printer.print("");
        }

        // Description: does object have a MODS description?
        List<String> contentStatus = object.getContentStatus();
        if (contentStatus != null && contentStatus.contains(FacetConstants.CONTENT_NOT_DESCRIBED)) {
            printer.print(FacetConstants.CONTENT_NOT_DESCRIBED);
        } else if (contentStatus != null && contentStatus.contains(FacetConstants.CONTENT_DESCRIBED)) {
            printer.print(FacetConstants.CONTENT_DESCRIBED);
        } else {
            printer.print("");
        }

        // Patron permissions
        String computedPermissions = computePatronPermissions(object.getRoleGroup());
        printer.print(computedPermissions);

        // Is object embargoed
        List<String> objStatus = object.getStatus();
        printer.print(objStatus != null && objStatus.contains(FacetConstants.EMBARGOED));

        printer.println();
    }

    public void setChildrenCountService(ChildrenCountService childrenCountService) {
        this.childrenCountService = childrenCountService;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setQueryLayer(SolrQueryLayerService queryLayer) {
        this.queryLayer = queryLayer;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
