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
package edu.unc.lib.boxc.indexing.solr.filter;

import static edu.unc.lib.boxc.model.api.xml.DescriptionConstants.COLLECTION_NUMBER_EL;
import static edu.unc.lib.boxc.model.api.xml.DescriptionConstants.COLLECTION_NUMBER_LABEL;
import static edu.unc.lib.boxc.model.api.xml.DescriptionConstants.COLLECTION_NUMBER_TYPE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.utils.JDOMQueryUtil;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;

/**
 * Filter which sets descriptive metadata information, generally pulled from MODS
 *
 * @author bbpennel
 *
 */
public class SetDescriptiveMetadataFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetDescriptiveMetadataFilter.class);

    private final Properties languageCodeMap;
    public final static String AFFIL_URI = "http://cdr.unc.edu/vocabulary/Affiliation";
    private final SimpleDateFormat getYear = new SimpleDateFormat("yyyy");

    public SetDescriptiveMetadataFilter() {
        languageCodeMap = new Properties();
        try {
            languageCodeMap.load(new InputStreamReader(getClass().getResourceAsStream(
                    "/iso639LangMappings.txt")));
        } catch (IOException e) {
            log.error("Failed to load code language mappings", e);
        }
    }
    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        IndexDocumentBean idb = dip.getDocument();
        Element mods = dip.getMods();

        idb.setKeyword(new ArrayList<String>());
        if (mods != null) {
            this.extractTitles(mods, idb);
            this.extractNamesAndAffiliations(mods, idb, true);
            this.extractAbstract(mods, idb);
            this.extractCollectionId(mods, idb);
            this.extractLanguages(mods, idb);
            this.extractSubjects(mods, idb);
            this.extractDateCreated(mods, idb);
            this.extractIdentifiers(mods, idb);
            this.extractCitation(mods, idb);
            this.extractKeywords(mods, idb);
            this.extractGenre(mods, idb);
        }

        if (idb.getTitle() == null) {
            idb.setTitle(getAlternativeTitle(dip));
        }
        if (idb.getDateCreated() == null) {
            idb.setDateCreated(idb.getDateAdded());
            idb.setDateCreatedYear(getYear.format(idb.getDateAdded()));
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

    private void extractNamesAndAffiliations(Element mods, IndexDocumentBean idb, boolean splitDepartments) {
        List<Element> names = mods.getChildren("name", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> creators = new ArrayList<>();
        List<String> contributors = new ArrayList<>();

        for (Element nameEl : names) {
            // First see if there is a display form
            String nameValue = formatName(nameEl);

            if (nameValue != null) {
                contributors.add(nameValue);

                List<Element> roles = nameEl.getChildren("role", JDOMNamespaceUtil.MODS_V3_NS);
                // Person is automatically a creator if no role is provided.
                boolean isCreator = roles.size() == 0;
                if (!isCreator) {
                    // If roles were provided, then check to see if any of them are creators.  If so, store as creator.
                    for (Element role: roles) {
                        List<Element> roleTerms = role.getChildren("roleTerm", JDOMNamespaceUtil.MODS_V3_NS);
                        for (Element roleTerm: roleTerms) {
                            if ("creator".equalsIgnoreCase(roleTerm.getValue())) {
                                isCreator = true;
                                break;
                            }
                            if ("author".equalsIgnoreCase(roleTerm.getValue())) {
                                isCreator = true;
                                break;
                            }
                            if (isCreator) {
                                break;
                            }
                        }
                    }
                }

                if (isCreator) {
                    creators.add(nameValue);
                }
            }
        }

        if (contributors.size() > 0) {
            idb.setContributor(contributors);
        } else {
            idb.setContributor(null);
        }
        if (creators.size() > 0) {
            idb.setCreator(creators);
            idb.setCreatorSort(creators.get(0));
        } else {
            idb.setCreator(null);
            idb.setCreatorSort(null);
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
            for (Element aid: identifiers) {
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
            for (Element subjectObj: subjectEls) {
                List<Element> subjectParts = subjectObj.getChildren();
                for (Element subjectEl: subjectParts) {
                    String subjectName = subjectEl.getName();

                    if (subjectName.equals("name")) {
                        subjects.add(formatName(subjectEl));
                    }

                    if (subjectEl.getChildren().isEmpty() && subjectName.equals("topic")) {
                            subjects.add(subjectEl.getValue());
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

    private void extractLanguages(Element mods, IndexDocumentBean idb) {
        List<Element> languageEls = mods.getChildren("language", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> languages = new ArrayList<>();
        if (languageEls.size() > 0) {
            String languageTerm = null;
            for (Element languageObj: languageEls) {
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
            for (Element originInfoEl: originInfoEls) {
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
                idb.setDateCreatedYear(getYear.format(dateCreated));
            } else if (dateIssued != null) {
                idb.setDateCreated(dateIssued);
                idb.setDateCreatedYear(getYear.format(dateIssued));
            } else if (dateCaptured != null) {
                idb.setDateCreated(dateCaptured);
                idb.setDateCreatedYear(getYear.format(dateCaptured));
            }
        }
    }

    private void extractIdentifiers(Element mods, IndexDocumentBean idb) {
        List<Element> identifierEls = mods.getChildren("identifier", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> identifiers = new ArrayList<>();
        for (Element identifierEl: identifierEls) {
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
        for (Element childObj: physicalDescription) {
            this.addValuesToList(idb.getKeyword(), childObj.getChildren(
                    "note", JDOMNamespaceUtil.MODS_V3_NS));
        }
        List<Element> relatedItemEls = mods.getChildren("relatedItem", JDOMNamespaceUtil.MODS_V3_NS);
        for (Element childObj: relatedItemEls) {
            List<Element> childChildren = childObj.getChildren();
            for (Element childChildObj: childChildren) {
                this.addValuesToList(idb.getKeyword(), childChildObj.getChildren());
            }
        }
    }

    private void extractGenre(Element mods, IndexDocumentBean idb) {
        idb.setGenre(new ArrayList<>());
        this.addValuesToList(idb.getGenre(), mods.getChildren("genre", JDOMNamespaceUtil.MODS_V3_NS));
    }

    private void addValuesToList(List<String> values, List<Element> elements) {
        if (elements == null) {
            return;
        }
        for (Element elementObj: elements) {
            String value = elementObj.getValue();
            if (value != null) {
                values.add(value.trim());
            }
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
        String nameValue = nameEl.getChildText("displayForm", JDOMNamespaceUtil.MODS_V3_NS);
        if (nameValue == null) {
            // If there was no displayForm, then try to get the name parts.
            List<Element> nameParts = nameEl.getChildren("namePart", JDOMNamespaceUtil.MODS_V3_NS);
            if (nameParts.size() == 1) {
                nameValue = nameParts.get(0).getValue();
            } else if (nameParts.size() > 1) {
                Element genericPart = JDOMQueryUtil.getElementByAttribute(nameParts, "type", null);
                if (genericPart != null) {
                    nameValue = genericPart.getValue();
                } else {
                    // If there were multiple non-generic name parts, then try to piece them together
                    Element givenPart = JDOMQueryUtil.getElementByAttribute(nameParts, "type", "given");
                    Element familyPart = JDOMQueryUtil.getElementByAttribute(nameParts, "type", "family");
                    StringBuilder nameBuilder = new StringBuilder();
                    if (familyPart != null) {
                        nameBuilder.append(familyPart.getValue());
                        if (givenPart != null) {
                            nameBuilder.append(',').append(' ');
                        }
                    }
                    if (givenPart != null) {
                        nameBuilder.append(givenPart.getValue());
                    }
                    if (nameBuilder.length() > 0) {
                        nameValue = nameBuilder.toString();
                    } else {
                        // Nonsensical name, just use the first available value.
                        nameValue = nameParts.get(0).getValue();
                    }
                }
            }
        }
        return nameValue;
    }
}
