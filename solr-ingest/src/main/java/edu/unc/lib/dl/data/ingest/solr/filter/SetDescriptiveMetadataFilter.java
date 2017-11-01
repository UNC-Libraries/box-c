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
package edu.unc.lib.dl.data.ingest.solr.filter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.Ebucore;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.VocabularyHelperManager;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.JDOMQueryUtil;

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

    private VocabularyHelperManager vocabManager;


    public SetDescriptiveMetadataFilter() {
        languageCodeMap = new Properties();
        try {
            languageCodeMap.load(new InputStreamReader(this.getClass().getResourceAsStream(
                    "iso639LangMappings.txt")));
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
            this.extractLanguages(mods, idb);
            this.extractSubjects(mods, idb);
            this.extractDateCreated(mods, idb);
            this.extractIdentifiers(mods, idb);
            this.extractCitation(mods, idb);
            this.extractKeywords(mods, idb);

        } else {
            // TODO basic DC mappings
        }

        if (dip.getDocument().getTitle() == null) {
            idb.setTitle(getAlternativeTitle(dip));
        }
        if (dip.getDocument().getDateCreated() == null) {
            idb.setDateCreated(idb.getDateAdded());
        }
        idb.getKeyword().add(dip.getPid().getId());
    }

    private String getAlternativeTitle(DocumentIndexingPackage dip) throws FedoraException, IndexingException {
        String title = null;
        Resource resc = dip.getContentObject().getResource();

        // Use dc:title as a default
        if (resc.hasProperty(DcElements.title)) {
            Statement dcTitle = resc.getProperty(DcElements.title);
            return dcTitle.getString();
        }

        // fall back to filename if one is present
        if (title == null && resc.hasProperty(Ebucore.filename)) {
            Statement filename = resc.getProperty(Ebucore.filename);
            return filename.getString();
        }

        // Use the object's id as the title as a final option
        return dip.getPid().getId();
    }

    private void extractTitles(Element mods, IndexDocumentBean idb) {
        List<?> titles = mods.getChildren("titleInfo", JDOMNamespaceUtil.MODS_V3_NS);
        String mainTitle = null;
        List<String> otherTitles = new ArrayList<>();
        for (Object titleInfoObj : titles) {
            Element titleInfoEl = (Element) titleInfoObj;
            for (Object titleObj : titleInfoEl.getChildren()) {
                Element titleEl = (Element) titleObj;
                if (mainTitle == null && "title".equalsIgnoreCase(titleEl.getName())) {
                    mainTitle = titleEl.getValue();
                } else {
                    otherTitles.add(titleEl.getValue());
                }
            }
        }
        idb.setTitle(mainTitle);
        if (otherTitles.size() > 0) {
            idb.setOtherTitle(otherTitles);
        } else {
            idb.setOtherTitle(null);
        }
    }

    private void extractNamesAndAffiliations(Element mods, IndexDocumentBean idb, boolean splitDepartments) {
        List<?> names = mods.getChildren("name", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> creators = new ArrayList<>();
        List<String> contributors = new ArrayList<>();

        Element nameEl;
        for (Object nameObj : names) {
            nameEl = (Element) nameObj;
            // First see if there is a display form
            String nameValue = nameEl.getChildText("displayForm", JDOMNamespaceUtil.MODS_V3_NS);
            if (nameValue == null) {
                // If there was no displayForm, then try to get the name parts.
                List<?> nameParts = nameEl.getChildren("namePart", JDOMNamespaceUtil.MODS_V3_NS);
                if (nameParts.size() == 1) {
                    nameValue = ((Element) nameParts.get(0)).getValue();
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
                            // Non-sensical name, just use the first available value.
                            nameValue = ((Element) nameParts.get(0)).getValue();
                        }
                    }
                }
            }
            if (nameValue != null) {
                contributors.add(nameValue);


                List<?> roles = nameEl.getChildren("role", JDOMNamespaceUtil.MODS_V3_NS);
                // Person is automatically a creator if no role is provided.
                boolean isCreator = roles.size() == 0;
                if (!isCreator) {
                    // If roles were provided, then check to see if any of them are creators.  If so, store as creator.
                    for (Object role: roles) {
                        List<?> roleTerms = ((Element)role).getChildren("roleTerm", JDOMNamespaceUtil.MODS_V3_NS);
                        for (Object roleTerm: roleTerms) {
                            if ("creator".equalsIgnoreCase(((Element)roleTerm).getValue())) {
                                isCreator = true;
                                break;
                            }
                            if ("author".equalsIgnoreCase(((Element)roleTerm).getValue())) {
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

        Map<String, List<List<String>>> authTerms = vocabManager.getAuthoritativeForms(idb.getPid(), mods);
        List<String> flattened = new ArrayList<>();
        if (authTerms != null) {
            List<List<String>> affiliationTerms = authTerms.get(AFFIL_URI);

            if (affiliationTerms != null) {
                // Make the departments for the whole document into a form solr can take
                for (List<String> path : affiliationTerms) {
                    flattened.addAll(path);
                }
                if (flattened.size() == 0) {
                    idb.setDepartment(null);
                } else {
                    idb.setDepartment(flattened);
                }
            }
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

    private void extractSubjects(Element mods, IndexDocumentBean idb) {
        List<?> subjectEls = mods.getChildren("subject", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> subjects = new ArrayList<>();
        if (subjectEls.size() > 0) {
            for (Object subjectObj: subjectEls) {
                List<?> subjectParts = ((Element)subjectObj).getChildren();
                for (Object subjectPart: subjectParts) {
                    Element subjectEl = (Element)subjectPart;
                    if (subjectEl.getChildren().size() == 0) {
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
        List<?> languageEls = mods.getChildren("language", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> languages = new ArrayList<>();
        if (languageEls.size() > 0) {
            String languageTerm = null;
            for (Object languageObj: languageEls) {
                // Our schema only allows for iso639-2b languages at this point.
                languageTerm = ((Element)languageObj).getChildText("languageTerm", JDOMNamespaceUtil.MODS_V3_NS);
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
        List<?> originInfoEls = mods.getChildren("originInfo", JDOMNamespaceUtil.MODS_V3_NS);
        Date dateCreated = null;
        Date dateIssued = null;
        Date dateCaptured = null;
        if (originInfoEls.size() > 0) {
            for (Object originInfoObj: originInfoEls) {
                Element originInfoEl = (Element) originInfoObj;
                dateCreated = JDOMQueryUtil
                        .parseISO6392bDateChild(originInfoEl, "dateCreated", JDOMNamespaceUtil.MODS_V3_NS);
                if (dateCreated != null) {
                    break;
                }

                if (dateIssued == null) {
                    dateIssued = JDOMQueryUtil
                            .parseISO6392bDateChild(originInfoEl, "dateIssued", JDOMNamespaceUtil.MODS_V3_NS);
                }

                if (dateIssued == null && dateCaptured == null) {
                    dateCaptured = JDOMQueryUtil
                            .parseISO6392bDateChild(originInfoEl, "dateCaptured", JDOMNamespaceUtil.MODS_V3_NS);
                }
            }

            if (dateCreated != null) {
                idb.setDateCreated(dateCreated);
            } else if (dateIssued != null) {
                idb.setDateCreated(dateIssued);
            } else if (dateCaptured != null) {
                idb.setDateCreated(dateCaptured);
            }
        }
    }

    private void extractIdentifiers(Element mods, IndexDocumentBean idb) {
        List<?> identifierEls = mods.getChildren("identifier", JDOMNamespaceUtil.MODS_V3_NS);
        List<String> identifiers = new ArrayList<>();
        for (Object identifierObj: identifierEls) {
            StringBuilder identifierBuilder = new StringBuilder();
            Element identifierEl = (Element) identifierObj;
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
        this.addValuesToList(idb.getKeyword(), mods.getChildren("genre", JDOMNamespaceUtil.MODS_V3_NS));
        this.addValuesToList(idb.getKeyword(), mods.getChildren("typeOfResource", JDOMNamespaceUtil.MODS_V3_NS));
        this.addValuesToList(idb.getKeyword(), mods.getChildren("note", JDOMNamespaceUtil.MODS_V3_NS));
        List<?> physicalDescription = mods.getChildren("physicalDescription", JDOMNamespaceUtil.MODS_V3_NS);
        for (Object childObj: physicalDescription) {
            this.addValuesToList(idb.getKeyword(), ((Element)childObj).getChildren(
                    "note", JDOMNamespaceUtil.MODS_V3_NS));
        }
        List<?> relatedItemEls = mods.getChildren("relatedItem", JDOMNamespaceUtil.MODS_V3_NS);
        for (Object childObj: relatedItemEls) {
            List<?> childChildren = ((Element)childObj).getChildren();
            for (Object childChildObj: childChildren) {
                this.addValuesToList(idb.getKeyword(), ((Element)childChildObj).getChildren());
            }
        }
    }

    private void addValuesToList(List<String> values, List<?> elements) {
        if (elements == null) {
            return;
        }
        for (Object elementObj: elements) {
            String value = ((Element)elementObj).getValue();
            if (value != null) {
                values.add(value);
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

    public void setVocabManager(VocabularyHelperManager vocabManager) {
        this.vocabManager = vocabManager;
    }
}
