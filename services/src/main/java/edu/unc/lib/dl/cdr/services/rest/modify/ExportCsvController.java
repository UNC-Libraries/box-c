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
package edu.unc.lib.dl.cdr.services.rest.modify;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.dl.acl.util.Permission.viewHidden;
import static edu.unc.lib.dl.acl.util.UserRole.canViewOriginals;
import static edu.unc.lib.dl.acl.util.UserRole.none;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_ROOT_ID;
import static edu.unc.lib.dl.model.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.dl.search.solr.util.FacetConstants.MARKED_FOR_DELETION;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.acl.fcrepo4.AccessControlServiceImpl;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.ChildrenCountService;
import edu.unc.lib.dl.search.solr.util.FacetConstants;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;

/**
 * Controller which generates a CSV listing of a repository object
 * and all of its children, recursively depth first.
 *
 * @author bbpennel
 */
@Controller
@RequestMapping("exportTree/csv")
public class ExportCsvController extends AbstractSolrSearchController {
    private static final Logger log = LoggerFactory.getLogger(ExportCsvController.class);

    private static final int MAX_PAGE_SIZE = 1000000;

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

    @Autowired
    private ChildrenCountService childrenCountService;
    @Autowired
    private AccessControlServiceImpl aclService;

    protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

    @RequestMapping(value = "{pid}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> export(@PathVariable("pid") String pidString, HttpServletRequest request,
                                                      HttpServletResponse response) {

        Map<String, Object> result = new HashMap<>();
        result.put("action", "exportCsv");
        result.put("username", request.getRemoteUser());

        if (CONTENT_ROOT_ID.equals(pidString)) {
            log.debug("Error exporting CSV for {}. Not allowed to export collection root", pidString);
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        PID pid = PIDs.get(pidString);

        AccessGroupSet accessGroups = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient privileges to export CSV for " + pid.getUUID(),
                pid, accessGroups, viewHidden);

        try {
            SearchRequest searchRequest = generateSearchRequest(request, searchStateFactory.createSearchState());
            searchRequest.setRootPid(pid);
            searchRequest.setApplyCutoffs(false);

            SearchState searchState = searchRequest.getSearchState();
            searchState.setResultFields(Arrays.asList(SearchFieldKeys.ID.name(), SearchFieldKeys.TITLE.name(),
                    SearchFieldKeys.RESOURCE_TYPE.name(), SearchFieldKeys.ANCESTOR_IDS.name(),
                    SearchFieldKeys.STATUS.name(), SearchFieldKeys.DATASTREAM.name(),
                    SearchFieldKeys.ANCESTOR_PATH.name(), SearchFieldKeys.CONTENT_MODEL.name(),
                    SearchFieldKeys.DATE_ADDED.name(), SearchFieldKeys.DATE_UPDATED.name(),
                    SearchFieldKeys.LABEL.name(), SearchFieldKeys.CONTENT_STATUS.name(),
                    SearchFieldKeys.ROLE_GROUP.name()));
            searchState.setSortType("export");
            searchState.setRowsPerPage(MAX_PAGE_SIZE);

            BriefObjectMetadata container = queryLayer.addSelectedContainer(pid, searchState, false,
                    searchRequest.getAccessGroups());

            if (container == null) {
                return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
            }

            SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);

            List<BriefObjectMetadata> objects = resultResponse.getResultList();
            objects.add(0, container);

            childrenCountService.addChildrenCounts(objects, searchRequest.getAccessGroups());

            String filename = pid.getId().replace(":", "_") + ".csv";
            response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.addHeader("Content-Type", "text/csv");

            try (ServletOutputStream out = response.getOutputStream()) {
                Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

                try (CSVPrinter printer = getPrinter(writer)) {
                    for (BriefObjectMetadata object : objects) {
                        printObject(printer, object);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error exporting CSV for {}, {}", pidString, e.getMessage());
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return null;
    }

    private String computePatronPermissions(List<String> roles) {
        if (roles == null) {
            return "Staff-only";
        }

        Map<String, String> roleList = new HashMap<>();
        for (String role : roles) {
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

    private void printObject(CSVPrinter printer, BriefObjectMetadata object) throws IOException {
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
        if (childCount > 0) {
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
}