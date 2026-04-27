package edu.unc.lib.boxc.services.camel.machineGenerated;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.jena.sparql.function.library.leviathan.log;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bbpennel
 */
public class MachineGenDescriptionContextHelper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private RepositoryObjectLoader repositoryObjectLoader;
    private SolrSearchService solrSearchService;
    private AccessGroupSet accessGroups;
    private final static List<String> FILE_REQUEST_FIELDS = List.of(
            SearchFieldKey.PARENT_COLLECTION.name(),
            SearchFieldKey.ANCESTOR_IDS.name()
    );
    private final static List<String> FOLDER_REQUEST_FIELDS = List.of(
            SearchFieldKey.TITLE.name()
    );

    public String generateContext(PID filePid) {
        Map<String, Object> contextMap = new LinkedHashMap<>();
        // Load file object, get its MODS
        FileObject fileObject = repositoryObjectLoader.getFileObject(filePid);
        var fileModsContext = extractModsContext(fileObject);
        contextMap.put("item", fileModsContext);

        SimpleIdRequest fileRequest = new SimpleIdRequest(filePid, FILE_REQUEST_FIELDS, accessGroups);
        ContentObjectRecord fileRecord = solrSearchService.getObjectById(fileRequest);
        String[] ancestorIds = fileRecord.getAncestorIds().split("/");

        // Load Work object, get its MODS
        WorkObject workObject = repositoryObjectLoader.getWorkObject(PIDs.get(ancestorIds[ancestorIds.length - 1]));
        var workModsContext = extractModsContext(workObject);
        contextMap.put("work", workModsContext);

        // Retrieve parent collection title
        String collectionTitle = fileRecord.getParentCollectionName();
        if (collectionTitle != null) {
            contextMap.put("collection_title", fileRecord.getParentCollectionName());
        }

        // Retrieve parent folder titles
        if (fileRecord.getAncestorIds() != null) {
            // Must be more than 3 ancestors (admin unit, coll, work) to have any additional parent folders
            if (ancestorIds.length > 3) {
                String[] folderIds = Arrays.copyOfRange(ancestorIds, 2, ancestorIds.length - 2);
                List<String> folderTitles = new ArrayList<>();
                for (String folderId : folderIds) {
                    SimpleIdRequest folderRequest = new SimpleIdRequest(PIDs.get(folderId), FOLDER_REQUEST_FIELDS, accessGroups);
                    ContentObjectRecord folderRecord = solrSearchService.getObjectById(folderRequest);
                    folderTitles.add(folderRecord.getTitle());
                }
                contextMap.put("folder_titles", folderTitles);
            }
        }

        // Serialize the map to YAML

        return "";
    }

    private static Map<String, Object> extractModsContext(ContentObject contentObject) {
        Element modsEl;
        try (InputStream modsStream = contentObject.getDescription().getBinaryStream()) {
            Document dsDoc = createSAXBuilder().build(modsStream);
            modsEl = dsDoc.detachRootElement();
        } catch (JDOMException | IOException | FedoraException e) {
            throw new RepositoryException("Failed to parse MODS stream for object " + contentObject.getPid(), e);
        }

        // Extract mods fields and store them into the map
        Map<String, Object> modsContext = new HashMap<>();

        return modsContext;
    }
}
