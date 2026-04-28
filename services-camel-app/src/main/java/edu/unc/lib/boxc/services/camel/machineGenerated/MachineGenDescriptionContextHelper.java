package edu.unc.lib.boxc.services.camel.machineGenerated;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
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
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper service which generates context information in a prompt format to be provided to the description
 * generation service to assist its understanding of the content.
 *
 * @author bbpennel
 */
public class MachineGenDescriptionContextHelper {
    private static final Logger log = LoggerFactory.getLogger(MachineGenDescriptionContextHelper.class);
    private static final ObjectMapper MAPPER = YAMLMapper.builder().build();
    private static final org.jdom2.Namespace MODS_NS = JDOMNamespaceUtil.MODS_V3_NS;
    private static final XPathFactory XPATH_FACTORY = XPathFactory.instance();
    private static final XPathExpression<Element> XPATH_NOTE =
            XPATH_FACTORY.compile("mods:note", Filters.element(), null, MODS_NS);
    private static final XPathExpression<Element> XPATH_SUBJECT_TOPIC =
            XPATH_FACTORY.compile("mods:subject/mods:topic", Filters.element(), null, MODS_NS);
    private static final XPathExpression<Element> XPATH_SUBJECT_NAME_PART =
            XPATH_FACTORY.compile("mods:subject/mods:name/mods:namePart", Filters.element(), null, MODS_NS);
    private static final XPathExpression<Element> XPATH_SUBJECT_GEO =
            XPATH_FACTORY.compile("mods:subject/mods:geographic", Filters.element(), null, MODS_NS);
    private static final XPathExpression<Element> XPATH_DATE_CREATED =
            XPATH_FACTORY.compile("mods:originInfo/mods:dateCreated", Filters.element(), null, MODS_NS);
    private static final XPathExpression<Element> XPATH_DATE_ISSUED =
            XPATH_FACTORY.compile("mods:originInfo/mods:dateIssued", Filters.element(), null, MODS_NS);
    private final static List<String> ITEM_REQUEST_FIELDS = List.of(
            SearchFieldKey.TITLE.name(),
            SearchFieldKey.LANGUAGE.name(),
            SearchFieldKey.GENRE.name(),
            SearchFieldKey.CREATOR.name(),
            SearchFieldKey.PARENT_COLLECTION.name(),
            SearchFieldKey.ANCESTOR_IDS.name()
    );
    private final static List<String> FOLDER_REQUEST_FIELDS = List.of(
            SearchFieldKey.TITLE.name()
    );
    private final static String CONTEXT_PROMPT = "Use the reference information below to identify specific named " +
            "subjects, locations, or creators where you can do so with reasonable confidence. Where context " +
            "is ambiguous or the item may not be closely related to its parent collection, hedge appropriately.";

    private RepositoryObjectLoader repositoryObjectLoader;
    private SolrSearchService solrSearchService;
    private AccessGroupSet accessGroups;

    /**
     * Generate context reference information prompt segment for the file identified by the filePid.
     * @param filePid
     * @return
     */
    public String generateContext(PID filePid) {
        Map<String, Object> contextMap = new LinkedHashMap<>();
        // Load file object, get its MODS
        FileObject fileObject = repositoryObjectLoader.getFileObject(filePid);
        Map<String, Object> fileContext = new HashMap<>();
        storeModsContext(fileObject, fileContext);

        SimpleIdRequest fileRequest = new SimpleIdRequest(filePid, ITEM_REQUEST_FIELDS, accessGroups);
        ContentObjectRecord fileRecord = solrSearchService.getObjectById(fileRequest);
        storeSearchContext(fileRecord, fileContext);

        contextMap.put("item", fileContext);

        if (fileRecord.getAncestorIds() == null) {
            throw new ServiceException("File object " + filePid + " is missing ancestor information");
        }
        String[] ancestorIds = fileRecord.getAncestorIds().split("/");


        // Load Work object, get its MODS
        WorkObject workObject = repositoryObjectLoader.getWorkObject(PIDs.get(ancestorIds[ancestorIds.length - 1]));
        Map<String, Object> workContext = new HashMap<>();
        storeModsContext(workObject, workContext);

        SimpleIdRequest workRequest = new SimpleIdRequest(workObject.getPid(), ITEM_REQUEST_FIELDS, accessGroups);
        ContentObjectRecord workRecord = solrSearchService.getObjectById(workRequest);
        storeSearchContext(workRecord, workContext);

        contextMap.put("work", workContext);

        // Retrieve parent collection title
        String collectionTitle = fileRecord.getParentCollectionName();
        if (collectionTitle != null) {
            contextMap.put("collection_title", collectionTitle);
        }

        // Retrieve parent folder titles
        storeFolderTitles(contextMap, ancestorIds);

        String result = CONTEXT_PROMPT + "\n" + serializeContext(contextMap);
        log.debug("Generated context for file {}: {}", filePid, result);
        return result;
    }

    /**
     * Serialize context to YAML
     * @param contextMap
     * @return
     */
    private String serializeContext(Map<String, Object> contextMap) {
        try {
            return MAPPER.writeValueAsString(contextMap);
        } catch (IOException e) {
            throw new ServiceException("Failed to serialize context to YAML", e);
        }
    }

    private void storeFolderTitles(Map<String, Object> contextMap, String[] ancestorIds) {
        // Must be more than 3 ancestors (admin unit, coll, work) to have any additional parent folders
        if (ancestorIds == null || ancestorIds.length <= 3) {
            return;
        }
        String[] folderIds = Arrays.copyOfRange(ancestorIds, 2, ancestorIds.length - 1);
        List<String> folderTitles = new ArrayList<>(folderIds.length);
        for (String folderId : folderIds) {
            SimpleIdRequest folderRequest = new SimpleIdRequest(PIDs.get(folderId), FOLDER_REQUEST_FIELDS, accessGroups);
            ContentObjectRecord folderRecord = solrSearchService.getObjectById(folderRequest);
            folderTitles.add(folderRecord.getTitle());
        }
        contextMap.put("folder_titles", folderTitles);
    }

    private void storeSearchContext(ContentObjectRecord contentObjectRecord, Map<String, Object> context) {
        context.put("title", contentObjectRecord.getTitle());
        if (contentObjectRecord.getLanguage() != null) {
            context.put("language", contentObjectRecord.getLanguage());
        }
        if (contentObjectRecord.getCreator() != null) {
            context.put("creator", contentObjectRecord.getCreator());
        }
        if (contentObjectRecord.getGenre() != null) {
            context.put("genre", contentObjectRecord.getGenre());
        }
    }

    private void storeModsContext(ContentObject contentObject, Map<String, Object> context) {
        Element modsEl;
        if (contentObject.getDescription() == null) {
            return;
        }
        try (InputStream modsStream = contentObject.getDescription().getBinaryStream()) {
            Document dsDoc = createSAXBuilder().build(modsStream);
            modsEl = dsDoc.detachRootElement();
        } catch (JDOMException | IOException | FedoraException e) {
            throw new ServiceException("Failed to parse MODS stream for object " + contentObject.getPid(), e);
        }

        // Extract mods fields and store them into the map
        // Add all notes
        List<String> notes = XPATH_NOTE.evaluate(modsEl).stream()
                .map(Element::getTextTrim)
                .filter(v -> !v.isBlank())
                .collect(java.util.stream.Collectors.toList());
        if (!notes.isEmpty()) {
            context.put("note", notes);
        }

        // Add top, namePart, and geographic subjects
        List<String> subjects = new ArrayList<>();
        for (XPathExpression<Element> xp : List.of(XPATH_SUBJECT_TOPIC, XPATH_SUBJECT_NAME_PART, XPATH_SUBJECT_GEO)) {
            xp.evaluate(modsEl).stream()
                    .map(Element::getTextTrim)
                    .filter(v -> !v.isBlank())
                    .forEach(subjects::add);
        }
        if (!subjects.isEmpty()) {
            context.put("subjects", subjects);
        }

        // add dateCreated
        XPATH_DATE_CREATED.evaluate(modsEl).stream()
                .map(Element::getTextTrim)
                .filter(v -> !v.isBlank())
                .findFirst()
                .ifPresent(v -> context.put("date_created", v));

        // add dateIssued
        XPATH_DATE_ISSUED.evaluate(modsEl).stream()
                .map(Element::getTextTrim)
                .filter(v -> !v.isBlank())
                .findFirst()
                .ifPresent(v -> context.put("date_issued", v));
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    public void setAccessGroups(AccessGroupSet accessGroups) {
        this.accessGroups = accessGroups;
    }
}
