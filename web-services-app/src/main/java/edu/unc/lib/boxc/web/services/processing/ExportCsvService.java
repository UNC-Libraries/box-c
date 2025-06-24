package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.search.api.FacetConstants;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Objects;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewOriginals;
import static edu.unc.lib.boxc.auth.api.UserRole.none;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_ROOT_ID;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.embargoUntil;
import static edu.unc.lib.boxc.search.api.FacetConstants.MARKED_FOR_DELETION;

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
    public static final String DEPTH_HEADER = "Depth";
    public static final String DELETED_HEADER = "Deleted";
    public static final String DATE_ADDED_HEADER = "Date Added";
    public static final String DATE_UPDATED_HEADER = "Date Updated";
    public static final String MIME_TYPE_HEADER = "MIME Type";
    public static final String CHECKSUM_HEADER = "Checksum";
    public static final String FILE_SIZE_HEADER = "File Size (bytes)";
    public static final String ACCESS_SURROGATE_HEADER = "Access Surrogate";
    public static final String NUM_CHILDREN_HEADER = "Number of Children";
    public static final String DESCRIBED_HEADER = "Description";
    public static final String PATRON_PERMISSIONS_HEADER = "Patron Permissions";
    public static final String EMBARGO_HEADER = "Embargo Date";
    public static final String VIEW_BEHAVIOR_HEADER = "View";
    public static final String PARENT_WORK_URL = "Parent Work URL";
    public static final String PARENT_WORK_TITLE = "Parent Work Title";

    public static final String[] CSV_HEADERS = new String[] {
            OBJ_TYPE_HEADER, PID_HEADER, TITLE_HEADER, PATH_HEADER,
            DEPTH_HEADER, DELETED_HEADER, DATE_ADDED_HEADER, DATE_UPDATED_HEADER,
            MIME_TYPE_HEADER, CHECKSUM_HEADER, FILE_SIZE_HEADER, ACCESS_SURROGATE_HEADER, NUM_CHILDREN_HEADER,
            DESCRIBED_HEADER, PATRON_PERMISSIONS_HEADER, EMBARGO_HEADER, VIEW_BEHAVIOR_HEADER,
            PARENT_WORK_URL, PARENT_WORK_TITLE
    };

    private static final List<String> SEARCH_FIELDS = Arrays.asList(SearchFieldKey.ID.name(),
            SearchFieldKey.TITLE.name(),
            SearchFieldKey.RESOURCE_TYPE.name(), SearchFieldKey.ANCESTOR_IDS.name(),
            SearchFieldKey.STATUS.name(), SearchFieldKey.DATASTREAM.name(),
            SearchFieldKey.ANCESTOR_PATH.name(),
            SearchFieldKey.DATE_ADDED.name(), SearchFieldKey.DATE_UPDATED.name(),
            SearchFieldKey.CONTENT_STATUS.name(),
            SearchFieldKey.ROLE_GROUP.name(),
            SearchFieldKey.VIEW_BEHAVIOR.name());

    private ChildrenCountService childrenCountService;
    private AccessControlService aclService;
    private SolrQueryLayerService queryLayer;
    private RepositoryObjectLoader repositoryObjectLoader;
    private String baseUrl;
    private int pageSize = DEFAULT_PAGE_SIZE;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

    public void streamCsv(List<PID> pids, AgentPrincipals agent, OutputStream out) {
        AccessGroupSet accessGroups = agent.getPrincipals();
        validate(pids, accessGroups);

        // Open the CSV
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        try (CSVPrinter printer = getPrinter(writer)) {
            for (PID pid : pids) {
                printObjectRows(pid, printer, agent.getUsername(), accessGroups);
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to stream CSV results: ", e);
        }
    }

    private void validate(List<PID> pids, AccessGroupSet accessGroups) {
        for (PID pid : pids) {
            validateObject(pid, accessGroups);
        }
    }

    private void validateObject(PID pid, AccessGroupSet accessGroups) {
        if (CONTENT_ROOT_ID.equals(pid.getId())) {
            throw new IllegalArgumentException("Error exporting CSV for " + pid.getId()
                    + ". Not allowed to export collection root");
        }

        aclService.assertHasAccess("Insufficient privileges to export CSV for " + pid.getUUID(),
                pid, accessGroups, viewHidden);
    }

    private void printObjectRows(PID pid, CSVPrinter printer, String username, AccessGroupSet accessGroups) throws IOException {
        log.debug("Streaming CSV export of {} for {}", pid, username);

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
        // Vitals: object type, pid, title, path, depth
        printer.print(object.getResourceType());
        printer.print(object.getId());
        printer.print(object.getTitle());
        printer.print(object.getAncestorNames());
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

        printAccessSurrogateField(printer, object);

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

        // Embargo expiration date, if one is set
        printer.print(getEmbargoDate(object));

        // View behavior
        var behavior = object.getViewBehavior();
        printer.print(Objects.requireNonNullElse(behavior, ""));

        // Parent info for FileObjects
        if (ResourceType.File.name().equals(object.getResourceType())) {
            // parentWorkUrl, parentWorkTitle
            var parentWorkId = object.getAncestorPathFacet().getHighestTierNode().getSearchKey();
            printer.print(getUrl(parentWorkId));
            var parentWorkTitle = getTitle(object.getAncestorNames());
            printer.print(parentWorkTitle);
        } else {
            printer.print("");
            printer.print("");
        }

        printer.println();
    }

    private void printAccessSurrogateField(CSVPrinter printer, ContentObjectRecord object) throws IOException {
        if (ResourceType.File.name().equals(object.getResourceType())
                && object.getContentStatus().contains(FacetConstants.HAS_ACCESS_SURROGATE)) {
            printer.print("Y");
        } else {
            printer.print("");
        }
    }

    private String getEmbargoDate(ContentObjectRecord object) {
        if (object.getStatus() == null || !object.getStatus().contains(FacetConstants.EMBARGOED)) {
            return "";
        }
        var repoObj = repositoryObjectLoader.getRepositoryObject(object.getPid());
        var embargoProperty = repoObj.getResource().getProperty(embargoUntil);
        if (embargoProperty == null) {
            return "";
        }
        // Return just the date part of the embargo timestamp, in YYYY-MM-DD format
        return StringUtils.substringBefore(embargoProperty.getString(), "T");
    }

    /**
     * Creating a record URL with just the ID
     * @param id
     * @return
     */
    private String getUrl(String id) {
        return URIUtil.join(baseUrl, id);
    }

    /**
     * Transforming ancestor path names to get the second to last one (for the work), but keep escaped slashes if necessary
     * @param input
     * @return
     */
    private String getTitle(String input) {
        String regex = "(?<!\\\\)/";
        String[] result = input.split(regex);
        // for the last one escape any backslashes in the title
        return result[result.length - 2].replace("\\/", "/");
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

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
