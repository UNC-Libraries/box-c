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
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

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
    private static final org.jdom2.Namespace MODS_NS = JDOMNamespaceUtil.MODS_V3_NS;
    private static final XPathFactory XPATH_FACTORY = XPathFactory.instance();
    private static final XPathExpression<Element> XPATH_TITLE =
            XPATH_FACTORY.compile("mods:titleInfo/mods:title", Filters.element(), null, MODS_NS);
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
    private RepositoryObjectLoader repositoryObjectLoader;
    private SolrSearchService solrSearchService;
    private AccessGroupSet accessGroups;
    private final static List<String> FILE_REQUEST_FIELDS = List.of(
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

    public String generateContext(PID filePid) {
        Map<String, Object> contextMap = new LinkedHashMap<>();
        // Load file object, get its MODS
        FileObject fileObject = repositoryObjectLoader.getFileObject(filePid);
        Map<String, Object> fileContext = new HashMap<>();
        storeModsContext(fileObject, fileContext);

        SimpleIdRequest fileRequest = new SimpleIdRequest(filePid, FILE_REQUEST_FIELDS, accessGroups);
        ContentObjectRecord fileRecord = solrSearchService.getObjectById(fileRequest);
        String[] ancestorIds = fileRecord.getAncestorIds().split("/");

        storeSearchContext(fileRecord, fileContext);
        contextMap.put("item", fileContext);

        // Load Work object, get its MODS
        WorkObject workObject = repositoryObjectLoader.getWorkObject(PIDs.get(ancestorIds[ancestorIds.length - 1]));
        Map<String, Object> workContext = new HashMap<>();
        storeModsContext(workObject, workContext);

        SimpleIdRequest workRequest = new SimpleIdRequest(workObject.getPid(), FILE_REQUEST_FIELDS, accessGroups);
        ContentObjectRecord workRecord = solrSearchService.getObjectById(workRequest);
        storeSearchContext(workRecord, workContext);

        contextMap.put("work", workContext);

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
        try (InputStream modsStream = contentObject.getDescription().getBinaryStream()) {
            Document dsDoc = createSAXBuilder().build(modsStream);
            modsEl = dsDoc.detachRootElement();
        } catch (JDOMException | IOException | FedoraException e) {
            throw new RepositoryException("Failed to parse MODS stream for object " + contentObject.getPid(), e);
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
}
