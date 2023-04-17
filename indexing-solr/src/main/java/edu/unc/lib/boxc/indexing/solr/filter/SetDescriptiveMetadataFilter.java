package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.utils.JDOMQueryUtil;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.search.solr.services.TitleRetrievalService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static edu.unc.lib.boxc.model.api.xml.DescriptionConstants.COLLECTION_NUMBER_EL;
import static edu.unc.lib.boxc.model.api.xml.DescriptionConstants.COLLECTION_NUMBER_LABEL;
import static edu.unc.lib.boxc.model.api.xml.DescriptionConstants.COLLECTION_NUMBER_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Filter which sets descriptive metadata information, generally pulled from MODS
 *
 * @author bbpennel
 *
 */
public class SetDescriptiveMetadataFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetDescriptiveMetadataFilter.class);

    private final Properties languageCodeMap;
    private final Map<String, String> rightsUriMap;
    private final List<String> CREATOR_LIST = Arrays.asList("creator", "author", "interviewer", "interviewee");
    private final List<String> GENRE_ATTRIBUTES = Arrays.asList("authority", "authorityURI", "valueURI");
    private TitleRetrievalService titleRetrievalService;

    public SetDescriptiveMetadataFilter() throws IOException {
        languageCodeMap = new Properties();
        languageCodeMap.load(new InputStreamReader(getClass().getResourceAsStream(
                    "/iso639LangMappings.txt")));


        // URIS aren't great for property keys
        rightsUriMap = new HashMap<>();
        InputStream rightsStream = getClass().getClassLoader().getResourceAsStream(
                "rightsUriMappings.txt");
        if (rightsStream == null) {
            throw new IOException();
        }
        List<String> rights = IOUtils.readLines(rightsStream, UTF_8);
        for (String right : rights) {
            String[] rightsParts = right.split("=", 2);
            rightsUriMap.put(rightsParts[0], rightsParts[1]);
        }
    }

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        IndexDocumentBean idb = dip.getDocument();
        Element mods = dip.getMods();

        idb.setKeyword(new ArrayList<String>());
        if (mods != null) {
            this.extractTitles(mods, idb);
            this.extractNames(mods, idb);
            this.extractAbstract(mods, idb);
            this.extractCollectionId(mods, idb);
            this.extractLanguages(mods, idb);
            this.extractSubjects(mods, idb);
            this.extractOtherSubjects(mods, idb);
            this.extractLocations(mods, idb);
            this.extractPublisher(mods, idb);
            this.extractDateCreated(mods, idb);
            this.extractRights(mods, idb);
            this.extractIdentifiers(mods, idb);
            this.extractCitation(mods, idb);
            this.extractKeywords(mods, idb);
            this.extractGenre(mods, idb);
            this.extractExhibit(mods, idb);
        }

        if (idb.getTitle() == null) {
            idb.setTitle(getAlternativeTitle(dip));
        }
        // Store title to cache for units/collections, in case their children are updated in the same batch
        if (dip.getContentObject() instanceof AdminUnit || dip.getContentObject() instanceof CollectionObject) {
            titleRetrievalService.storeTitle(idb.getPid(), idb.getTitle());
        }
        idb.getKeyword().add(dip.getPid().getId());
    }

    private String getAlternativeTitle(DocumentIndexingPackage dip) throws FedoraException, IndexingException {
        Resource resc = dip.getContentObject().getResource();
        String dcTitle = titleText(resc, DcElements.title);
        String ebucoreTitle = titleText(resc, Ebucore.filename);
        if (isBlank(dcTitle) && isBlank(ebucoreTitle) && dip.getContentObject() instanceof FileObject) {
            ebucoreTitle = ((FileObject) dip.getContentObject()).getOriginalFile().getFilename();
        }

        // Use dc:title as a default
        if (!isBlank(dcTitle)) {
            return dcTitle;
        } else if (!isBlank(ebucoreTitle)) { // fall back to filename if one is present
            return ebucoreTitle;
        } else { // Use the object's id as the title as a final option
            return dip.getPid().getId();
        }
    }

    private String titleText(Resource resc, Property field) {
        if (resc.hasProperty(field)) {
            Statement title = resc.getProperty(field);
            return title.getString();
        }

        return "";
    }

    private void extractTitles(Element mods, IndexDocumentBean idb) {
        List<Element> titles = mods.getChildren("titleInfo", JDOMNamespaceUtil.MODS_V3_NS);
        String mainTitle = null;
        List<String> otherTitles = new ArrayList<>();
        for (Element titleInfoEl : titles) {
            for (Object titleObj : titleInfoEl.getChildren()) {
                Element titleEl = (Element) titleObj;
                if (mainTitle == null && "title".equalsIgnoreCase(titleEl.getName())) {
                    mainTitle = titleEl.getValue();
                } else {
                    otherTitles.add(titleEl.getValue());
                }
            }
        }

        if (!isBlank(mainTitle)) {
            idb.setTitle(mainTitle);
        }

        if (otherTitles.size() > 0) {
            idb.setOtherTitle(otherTitles);
        } else {
            idb.setOtherTitle(null);
        }
    }

    private void extractNames(Element mods, IndexDocumentBean idb) {
        List<Element> names = mods.getChildren("name", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> creators = new ArrayList<>();
        List<String> contributors = new ArrayList<>();
        List<String> creatorsContributors = new ArrayList<>();

        for (Element nameEl : names) {
            // First see if there is a display form
            String nameValue = formatName(nameEl);

            if (StringUtils.isBlank(nameValue)) {
                continue;
            }

            List<Element> roles = nameEl.getChildren("role", JDOMNamespaceUtil.MODS_V3_NS);
            // Person is automatically a contributor if no role is provided.
            boolean isContributor = roles.isEmpty();
            boolean isCreator = false;
            if (!isContributor) {
                // If roles were provided, then check to see if any of them are creators.  If so, store as creator.
                for (Element role : roles) {
                    List<Element> roleTerms = role.getChildren("roleTerm", JDOMNamespaceUtil.MODS_V3_NS);
                    if (roleTerms.isEmpty()) {
                        isContributor = true;
                    }  else {
                        for (Element roleTerm : roleTerms) {
                            String roleType = roleTerm.getTextTrim();
                            if (CREATOR_LIST.contains(roleType.toLowerCase())) {
                                isCreator = true;
                            } else {
                                isContributor = true;
                            }
                        }
                    }
                }
            }

            if (isCreator) {
                creators.add(nameValue);
                creatorsContributors.add(nameValue);
            }

            if (isContributor) {
                contributors.add(nameValue);
                creatorsContributors.add(nameValue);
            }
        }

        if (contributors.size() > 0) {
            idb.setContributor(contributors);
        } else {
            idb.setContributor(null);
        }
        if (creators.size() > 0) {
            idb.setCreator(creators);
        } else {
            idb.setCreator(null);
        }
        if (creatorsContributors.size() > 0) {
            idb.setCreatorContributor(creatorsContributors);
        } else {
            idb.setCreatorContributor(null);
        }
    }

    private void extractAbstract(Element mods, IndexDocumentBean idb) {
        String abstractText = mods.getChildText("abstract", JDOMNamespaceUtil.MODS_V3_NS);
        if (abstractText != null) {
            idb.setAbstractText(abstractText.trim());
        } else {
            idb.setAbstractText(null);
        }
    }

    private void extractCollectionId(Element mods, IndexDocumentBean idb) {
        List<Element> identifiers = mods.getChildren(COLLECTION_NUMBER_EL, JDOMNamespaceUtil.MODS_V3_NS);
        String collectionId = null;

        if (!identifiers.isEmpty()) {
            for (Element aid : identifiers) {
                Attribute type = aid.getAttribute("type");
                Attribute collection = aid.getAttribute("displayLabel");

                if (type == null || collection == null) {
                    continue;
                }

                if (collection.getValue().equals(COLLECTION_NUMBER_LABEL)
                        && type.getValue().equals(COLLECTION_NUMBER_TYPE)) {
                    collectionId = aid.getValue();
                    break;
                }
            }
        }

        idb.setCollectionId(collectionId);
    }

    private void extractSubjects(Element mods, IndexDocumentBean idb) {
        List<Element> subjectEls = mods.getChildren("subject", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> subjects = new ArrayList<>();
        if (subjectEls.size() > 0) {
            for (Element subjectObj : subjectEls) {
                List<Element> subjectParts = subjectObj.getChildren();
                for (Element subjectEl : subjectParts) {
                    String subjectName = subjectEl.getName();

                    if (subjectName.equals("name")) {
                        addIfNotBlank(subjects, formatName(subjectEl));
                    }

                    if (subjectEl.getChildren().isEmpty() && subjectName.equals("topic")) {
                        addIfNotBlank(subjects, subjectEl.getValue());
                    }
                }
            }
        }
        if (subjects.size() > 0) {
            idb.setSubject(subjects);
        } else {
            idb.setSubject(null);
        }

    }

    private void extractOtherSubjects(Element mods, IndexDocumentBean idb) {
        List<Element> otherSubjectEls = mods.getChildren("subject", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> otherSubjects = new ArrayList<>();
        extractNestedSubjects(otherSubjectEls, otherSubjects);
        if (otherSubjects.size() > 0) {
            idb.setOtherSubject(otherSubjects);
        } else {
            idb.setOtherSubject(null);
        }
    }

    private void extractNestedSubjects(List<Element> otherSubjectEls, List<String> otherSubjects) {
        if (otherSubjectEls.isEmpty()) {
            return;
        }

        for (Element otherSubjectEl : otherSubjectEls) {
            List<Element> otherSubjectParts = otherSubjectEl.getChildren();

            // No further levels of recursive nesting
            if (otherSubjectParts.isEmpty()) {
                if (!invalidOtherSubject(otherSubjectEl)) {
                    addIfNotBlank(otherSubjects, otherSubjectEl.getValue());
                }
                continue;
            }

            for (Element otherSubjectPart : otherSubjectParts) {
                if (invalidOtherSubject(otherSubjectPart)) {
                    continue;
                }

                List<Element> childrenEls = otherSubjectPart.getChildren();
                if (childrenEls.isEmpty()) {
                    addIfNotBlank(otherSubjects, otherSubjectPart.getValue());
                } else {
                    extractNestedSubjects(childrenEls, otherSubjects);
                }
            }
        }
    }

    private boolean invalidOtherSubject(Element otherSubject) {
        String otherSubjectName = otherSubject.getName();
        return otherSubjectName.equals("name") || otherSubjectName.equals("topic");
    }

    private void extractLocations(Element mods, IndexDocumentBean idb) {
        List<Element> locationEls = mods.getChildren("subject", JDOMNamespaceUtil.MODS_V3_NS);
        if (locationEls.isEmpty()) {
            idb.setLocation(null);
            return;
        }

        List<String> locations = new ArrayList<>();
        for (Element locationEl : locationEls) {
            String authority = locationEl.getAttributeValue("authority");
            if (authority != null && authority.equals("lcsh")) {
                List<Element> locationParts = locationEl.getChildren("geographic",
                        JDOMNamespaceUtil.MODS_V3_NS);
                for (Element locEl : locationParts) {
                    addIfNotBlank(locations, locEl.getValue());
                }
            }
        }

        if (!locations.isEmpty()) {
            idb.setLocation(locations);
        } else {
            idb.setLocation(null);
        }
    }

    private void extractLanguages(Element mods, IndexDocumentBean idb) {
        List<Element> languageEls = mods.getChildren("language", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> languages = new ArrayList<>();
        if (languageEls.size() > 0) {
            String languageTerm = null;
            for (Element languageObj : languageEls) {
                // Our schema only allows for iso639-2b languages at this point.
                languageTerm = languageObj.getChildText("languageTerm", JDOMNamespaceUtil.MODS_V3_NS);
                if (languageTerm != null) {
                    languageTerm = languageCodeMap.getProperty(languageTerm.trim());
                    if (languageTerm != null) {
                        languages.add(languageTerm);
                    }
                }
            }
        }
        if (languages.size() > 0) {
            idb.setLanguage(languages);
        } else {
            idb.setLanguage(null);
        }
    }

    private void extractPublisher(Element mods, IndexDocumentBean idb) {
        List<Element> originInfoEls = mods.getChildren("originInfo", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> publishers = new ArrayList<>();

        for (Element originInfoEl : originInfoEls) {
            List<Element> publisherEls = originInfoEl.getChildren("publisher", JDOMNamespaceUtil.MODS_V3_NS);
            for (Element publisher : publisherEls) {
                String publisherText = publisher.getTextTrim();
                if (StringUtils.isBlank(publisherText)) {
                    continue;
                }
                publishers.add(publisherText);
            }
        }

        if (publishers.size() > 0) {
            idb.setPublisher(publishers);
        } else {
            idb.setPublisher(null);
        }
    }

    /**
     * Get the preferred date to use as the date created. Order of preference is
     * the first date created found, then the first date issued, then the first
     * date captured.
     *
     * @param mods
     * @param idb
     */
    private void extractDateCreated(Element mods, IndexDocumentBean idb) {
        List<Element> originInfoEls = mods.getChildren("originInfo", JDOMNamespaceUtil.MODS_V3_NS);
        Date dateCreated = null;
        Date dateIssued = null;
        Date dateCaptured = null;
        if (originInfoEls.size() > 0) {
            for (Element originInfoEl : originInfoEls) {
                dateCreated = JDOMQueryUtil
                        .parseISO6392bDateChild(originInfoEl, "dateCreated", JDOMNamespaceUtil.MODS_V3_NS);
                if (dateCreated != null) {
                    break;
                }

                if (dateIssued == null) {
                    dateIssued = JDOMQueryUtil
                            .parseISO6392bDateChild(originInfoEl, "dateIssued", JDOMNamespaceUtil.MODS_V3_NS);
                }

                if (dateCaptured == null) {
                    dateCaptured = JDOMQueryUtil
                            .parseISO6392bDateChild(originInfoEl, "dateCaptured", JDOMNamespaceUtil.MODS_V3_NS);
                }
            }

            if (dateCreated != null) {
                idb.setDateCreated(dateCreated);
                idb.setDateCreatedYear(extractDateYear(dateCreated));
            } else if (dateIssued != null) {
                idb.setDateCreated(dateIssued);
                idb.setDateCreatedYear(extractDateYear(dateIssued));
            } else if (dateCaptured != null) {
                idb.setDateCreated(dateCaptured);
            }
        }
    }

    private void extractRights(Element mods, IndexDocumentBean idb) {
        List<String> rights = new ArrayList<>();
        List<String> rightsOaiPmh = new ArrayList<>();
        List<String> rightsUri = new ArrayList<>();

        List<Element> rightsEls = mods.getChildren("accessCondition", JDOMNamespaceUtil.MODS_V3_NS);
        for (Element rightsEl : rightsEls) {
            String accessType = rightsEl.getAttributeValue("type");
            if (accessType != null && accessType.trim().equalsIgnoreCase("use and reproduction")) {
                String href = rightsEl.getAttributeValue("href", JDOMNamespaceUtil.XLINK_NS);
                String modsRightsStatement = rightsEl.getTextTrim();
                boolean hasUrl = !StringUtils.isBlank(href);
                boolean hasRightsText = !StringUtils.isBlank(modsRightsStatement);
                var displayLabel = rightsEl.getAttributeValue("displayLabel");
                var disallowedLabel = displayLabel == null || displayLabel.equalsIgnoreCase("CONTENTdm Usage Rights");

                if ((!hasRightsText && !hasUrl) || disallowedLabel) {
                    continue;
                }

                // No vocabulary. Add MODS text
                if (hasRightsText && !hasUrl) {
                    rights.add(modsRightsStatement);
                    rightsOaiPmh.add(modsRightsStatement);
                    continue;
                }

                String trimmedHref = href.trim();
                if (rightsUriMap.containsKey(trimmedHref)) {
                    String mappedRightsText = rightsUriMap.get(trimmedHref);
                    if (hasRightsText && !mappedRightsText.equals(modsRightsStatement)) {
                        log.error("MODS rights text, {}, does not match the rights vocabulary for, {}.",
                                modsRightsStatement, href);
                    }
                    rightsOaiPmh.add(convertUrlProtocol(trimmedHref));
                    rightsOaiPmh.add(mappedRightsText);
                    rights.add(mappedRightsText);
                    rightsUri.add(trimmedHref);
                } else {
                    log.warn("URI, {} wasn't found in the rights uri mappings.", trimmedHref);
                }
            }
        }

        if (!rights.isEmpty()) {
            idb.setRights(rights);
        } else {
            idb.setRights(null);
        }

        if (!rightsOaiPmh.isEmpty()) {
            idb.setRightsOaiPmh(rightsOaiPmh);
        } else {
            idb.setRightsOaiPmh(null);
        }

        if (!rightsUri.isEmpty()) {
            idb.setRightsUri(rightsUri);
        } else {
            idb.setRightsUri(null);
        }
    }

    private String convertUrlProtocol(String uri) {
        if (uri.startsWith("http:")) {
            return uri;
        }
        return uri.replace("https", "http");
    }

    private void extractIdentifiers(Element mods, IndexDocumentBean idb) {
        List<Element> identifierEls = mods.getChildren("identifier", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> identifiers = new ArrayList<>();
        for (Element identifierEl : identifierEls) {
            StringBuilder identifierBuilder = new StringBuilder();
            String idType = identifierEl.getAttributeValue("type");
            if (idType != null) {
                if (idType.equalsIgnoreCase("uri")) {
                    continue;
                }
                identifierBuilder.append(idType);
            }
            String idValue = identifierEl.getValue();
            identifierBuilder.append('|').append(idValue);

            identifiers.add(identifierBuilder.toString());
            idb.getKeyword().add(idValue);
        }
        if (identifiers.size() > 0) {
            idb.setIdentifier(identifiers);
        } else {
            idb.setIdentifier(null);
        }

    }

    private void extractKeywords(Element mods, IndexDocumentBean idb) {
        this.addValuesToList(idb.getKeyword(), mods.getChildren("typeOfResource", JDOMNamespaceUtil.MODS_V3_NS));
        this.addValuesToList(idb.getKeyword(), mods.getChildren("note", JDOMNamespaceUtil.MODS_V3_NS));
        List<Element> physicalDescription = mods.getChildren("physicalDescription", JDOMNamespaceUtil.MODS_V3_NS);
        for (Element childObj : physicalDescription) {
            this.addValuesToList(idb.getKeyword(), childObj.getChildren(
                    "note", JDOMNamespaceUtil.MODS_V3_NS));
        }
        List<Element> relatedItemEls = mods.getChildren("relatedItem", JDOMNamespaceUtil.MODS_V3_NS);
        for (Element childObj : relatedItemEls) {
            List<Element> childChildren = childObj.getChildren();
            for (Element childChildObj : childChildren) {
                this.addValuesToList(idb.getKeyword(), childChildObj.getChildren());
            }
        }
    }

    /**
     * Extract exhibit name and url
     * @param mods
     * @param idb
     */
    private void extractExhibit(Element mods, IndexDocumentBean idb) {
        var exhibits = new ArrayList<String>();
        List<Element> relatedItems = mods.getChildren("relatedItem", JDOMNamespaceUtil.MODS_V3_NS);
        for (Element relatedItem : relatedItems) {
            String itemType = relatedItem.getAttributeValue("type");
            String displayLabel = relatedItem.getAttributeValue("displayLabel");

            if ("isReferencedBy".equals(itemType) && "Digital Exhibit".equals(displayLabel)) {
                List<Element> locations = relatedItem.getChildren("location", JDOMNamespaceUtil.MODS_V3_NS);
                for (Element location : locations) {
                    List<Element> locationLinks = location.getChildren("url", JDOMNamespaceUtil.MODS_V3_NS);
                    for (Element locationLink : locationLinks) {
                        String exhibitLabel = locationLink.getAttributeValue("displayLabel");
                        String exhibitLink = locationLink.getTextTrim();

                        if (!StringUtils.isBlank(exhibitLink)) {
                            String label = !StringUtils.isBlank(exhibitLabel) ? exhibitLabel : exhibitLink;
                            exhibits.add(label + "|" + exhibitLink);
                        }
                    }
                }
            }
        }

        if (!exhibits.isEmpty()) {
            idb.setExhibit(exhibits);
        } else {
            idb.setExhibit(null);
        }
    }

    private void extractGenre(Element mods, IndexDocumentBean idb) {
        var genres = new ArrayList<String>();
        List<Element> genreList = mods.getChildren("genre", JDOMNamespaceUtil.MODS_V3_NS);

        for (Element genre : genreList) {
            if (hasAttribute(genre, GENRE_ATTRIBUTES)) {
                String genreValue = genre.getTextTrim();
                if (!StringUtils.isBlank(genreValue)) {
                    genres.add(genreValue);
                }
            }
        }

        if (!genres.isEmpty()) {
            idb.setGenre(genres);
        } else {
            idb.setGenre(null);
        }
    }

    private boolean hasAttribute(Element el, List<String> attributes) {
        for (String attr : attributes) {
            String attrValue = el.getAttributeValue(attr);
            if (!StringUtils.isBlank(attrValue)) {
                return true;
            }
        }
        return false;
    }

    private void addIfNotBlank(List<String> values, String newValue) {
        if (StringUtils.isBlank(newValue)) {
            return;
        }
        values.add(newValue);
    }

    private void addValuesToList(List<String> values, List<Element> elements) {
        if (elements == null) {
            return;
        }
        for (Element elementObj : elements) {
            addIfNotBlank(values, elementObj.getValue());
        }
    }

    private void extractCitation(Element mods, IndexDocumentBean idb) {
        Element citationEl = JDOMQueryUtil.getChildByAttribute(
                mods, "note", JDOMNamespaceUtil.MODS_V3_NS, "type", "citation/reference");
        if (citationEl != null) {
            idb.setCitation(citationEl.getValue().trim());
        } else {
            idb.setCitation(null);
        }
    }

    private String formatName(Element nameEl) {
        String nameValue = null;

        List<Element> nameParts = nameEl.getChildren("namePart", JDOMNamespaceUtil.MODS_V3_NS);
        if (nameParts.size() == 1) {
            String nameTypeValue = nameParts.get(0).getAttributeValue("type");
            if (nameTypeValue == null || (nameTypeValue.equals("family") || nameTypeValue.equals("given"))) {
                nameValue = nameParts.get(0).getTextTrim();
            }
        } else if (nameParts.size() > 1) {
            Element givenPart = JDOMQueryUtil.getElementByAttribute(nameParts, "type", null);
            if (!hasNodeValue(givenPart)) {
                givenPart = JDOMQueryUtil.getElementByAttribute(nameParts, "type", "given");
            }
            // If there were multiple non-generic name parts, then try to piece them together
            Element familyPart = JDOMQueryUtil.getElementByAttribute(nameParts, "type", "family");
            Element termsOfAddressPart = JDOMQueryUtil.getElementByAttribute(nameParts, "type", "termsOfAddress");
            Element datePart = JDOMQueryUtil.getElementByAttribute(nameParts, "type", "date");
            StringBuilder nameBuilder = new StringBuilder();

            boolean hasFamilyPart = hasNodeValue(familyPart);
            boolean hasGivenPart = hasNodeValue(givenPart);
            if (hasFamilyPart) {
                nameBuilder.append(familyPart.getTextTrim());
                if (hasGivenPart) {
                    nameBuilder.append(',').append(' ');
                }
            }
            if (hasGivenPart) {
                nameBuilder.append(givenPart.getTextTrim());
            }

            if (hasFamilyPart || hasGivenPart) {
                if (hasNodeValue(termsOfAddressPart)) {
                    nameBuilder.append(", ").append(termsOfAddressPart.getTextTrim());
                }
                if (hasNodeValue(datePart)) {
                    nameBuilder.append(", ").append(datePart.getTextTrim());
                }
            }

            if (nameBuilder.length() > 0) {
                nameValue = nameBuilder.toString();
            }
        }

        return nameValue;
    }

    private String extractDateYear(Date date) {
        return Integer.toString(date.toInstant().atZone(ZoneOffset.UTC).getYear());
    }

    private boolean hasNodeValue(Element node) {
        return node != null && !StringUtils.isBlank(node.getTextTrim());
    }

    public void setTitleRetrievalService(TitleRetrievalService titleRetrievalService) {
        this.titleRetrievalService = titleRetrievalService;
    }
}