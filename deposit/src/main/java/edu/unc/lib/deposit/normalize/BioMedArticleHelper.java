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
package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * 
 * @author bbpennel
 *
 */
public class BioMedArticleHelper {

    public static int MAX_SUPPL_TITLE_LENGTH = 250;

    private final XPathExpression<Element> articleXPath;

    public BioMedArticleHelper() {
        XPathFactory xFactory = XPathFactory.instance();
        articleXPath = xFactory.compile("/Publisher/Journal/Volume/Issue/Article", Filters.element());
    }

    public Document extractMODS(Document articleDocument, Document existingMods) throws JDOMException {

        // Start from a new MODS document if no existing
        Document result;
        Element modsContent;
        if (existingMods == null) {
            result = new Document();
            modsContent = new Element("mods", MODS_V3_NS);
            result.setRootElement(modsContent);
        } else {
            // Given an existing MODS document, start from it and strip out existing names
            result = existingMods;
            List<Element> preexistingModsNames = result.getRootElement().getChildren("name", MODS_V3_NS);
            if (preexistingModsNames != null) {
                Iterator<Element> it = preexistingModsNames.iterator();
                while (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
            modsContent = result.getRootElement();
        }

        Element articleRoot = articleXPath.evaluateFirst(articleDocument);

        // Add author information
        addAuthorsAndAffiliations(articleRoot, modsContent);

        // Add journal bibliographic info
        addBibliographicInfo(articleDocument, modsContent);

        return result;
    }

    private void addAuthorsAndAffiliations(Element article, Element modsContent) {
        Element articleHeader = article.getChild("ArticleHeader");
        Element authorGroup = articleHeader.getChild("AuthorGroup");

        //Extract affiliations
        List<Element> elements = authorGroup.getChildren("Affiliation");
        Map<String,String> affiliationMap = new HashMap<String,String>();
        if (elements != null) {
            for (Element element: elements) {
                String affId = element.getAttributeValue("ID");
                String affiliation = element.getChildTextTrim("OrgDivision");
                String orgName = element.getChildTextTrim("OrgName");
                if (affiliation == null) {
                    affiliation = orgName;
                } else if (orgName != null) {
                    affiliation += ", " + orgName;
                }
                affiliationMap.put(affId, affiliation);
            }
        }

        //Extract author names, then create name attributes with affiliations
        elements = authorGroup.getChildren("Author");
        if (elements != null) {
            for (Element element: elements) {
                Element nameElement = new Element("name", MODS_V3_NS);

                Element authorName = element.getChild("AuthorName");

                // Add the name parts
                String surname = authorName.getChildText("FamilyName");
                if (surname != null) {
                    Element namePartElement = new Element("namePart", MODS_V3_NS);
                    namePartElement.setAttribute("type", "family");
                    namePartElement.setText(surname);
                    nameElement.addContent(namePartElement);
                }

                StringBuilder givenName = new StringBuilder();
                List<Element> givenNames = authorName.getChildren("GivenName");
                if (givenNames != null) {
                    for (Element name : givenNames) {
                        if (givenName.length() > 0) {
                            givenName.append(' ');
                        }
                        givenName.append(name.getTextNormalize());
                    }

                    Element namePartElement = new Element("namePart", MODS_V3_NS);
                    namePartElement.setAttribute("type", "given");
                    namePartElement.setText(givenName.toString());
                    nameElement.addContent(namePartElement);
                }

                // Lookup the authors affiliation by id and assign
                String affiliationId = element.getAttributeValue("AffiliationIDS");
                if (affiliationId != null) {
                    String[] ids = affiliationId.split(" ");
                    for (String id : ids) {
                        String affiliation = affiliationMap.get(id);

                        if (affiliation != null) {
                            Element affiliationElement = new Element("affiliation", MODS_V3_NS);
                            affiliationElement.setText(affiliation);
                            nameElement.addContent(affiliationElement);
                        }
                    }
                }

                modsContent.addContent(nameElement);
            }
        }
    }

    private void addBibliographicInfo(Document articleDoc, Element modsContent) {
        Element journalEl = articleDoc.getRootElement().getChild("Journal");

        Element sourceElement = new Element("relatedItem", MODS_V3_NS);
        sourceElement.setAttribute("type", "otherFormat");
        sourceElement.setAttribute("displayLabel", "Source");
        modsContent.addContent(sourceElement);

        Element journalInfo = journalEl.getChild("JournalInfo");

        String jTitle = journalInfo.getChildTextNormalize("JournalTitle");
        Element titleInfoElement = new Element("titleInfo", MODS_V3_NS);
        Element titleElement = new Element("title", MODS_V3_NS);
        sourceElement.addContent(titleInfoElement);
        titleInfoElement.addContent(titleElement);
        titleElement.setText(jTitle);

        String issn = journalInfo.getChildTextNormalize("JournalElectronicISSN");
        if (issn != null) {
            Element element = new Element("identifier", MODS_V3_NS);
            element.setText(issn);
            element.setAttribute("type", "issn");
            sourceElement.addContent(element);
        }

        // Add journal issue and volume info
        Element volumeEl = journalEl.getChild("Volume");
        String volume = volumeEl.getChild("VolumeInfo").getChildText("VolumeIDStart");

        Element issue = volumeEl.getChild("Issue");
        Element issueInfo = issue.getChild("IssueInfo");

        if (volume != null || issueInfo != null) {
            Element element = new Element("part", MODS_V3_NS);
            addDetailElement("volume", volume, "vol.", element);
            addDetailElement("issue", issueInfo.getChildText("IssueIDStart"), "issue", element);
            sourceElement.addContent(element);

            String pubYear = issueInfo.getChild("IssueHistory").getChild("CoverDate").getChildText("Year");
            Element pubEl = new Element("originInfo", MODS_V3_NS);
            Element dateIssued = new Element("dateIssued", MODS_V3_NS);
            dateIssued.setText(pubYear);
            pubEl.setContent(dateIssued);
            sourceElement.addContent(pubEl);
        }

        Element articleInfo = issue.getChild("Article").getChild("ArticleInfo");

        // Add article identifiers
        String doi = articleInfo.getChildTextNormalize("ArticleDOI");
        if (doi != null) {
            Element doiEl = new Element("identifier", MODS_V3_NS);
            doiEl.setAttribute("type", "doi");
            doiEl.setText(doi);
            modsContent.addContent(doiEl);
        }

        String biomedId = articleInfo.getAttributeValue("ID");
        if (biomedId != null) {
            Element idEl = new Element("identifier", MODS_V3_NS);
            idEl.setAttribute("type", "BMID");
            idEl.setText(biomedId.trim());
            modsContent.addContent(idEl);
        }

        // Add first/last page references to journal info
        String fpage = articleInfo.getChildTextNormalize("ArticleFirstPage");
        String lpage = articleInfo.getChildTextNormalize("ArticleLastPage");
        if (fpage != null || lpage != null) {
            Element extentPart = new Element("part", MODS_V3_NS);
            Element element = new Element("extent", MODS_V3_NS);
            element.setAttribute("unit", "page");
            if (fpage != null) {
                Element pageElement = new Element("start", MODS_V3_NS);
                pageElement.setText(fpage);
                element.addContent(pageElement);
            }
            if (lpage != null) {
                Element pageElement = new Element("end", MODS_V3_NS);
                pageElement.setText(lpage);
                element.addContent(pageElement);
            }
            extentPart.addContent(element);
            sourceElement.addContent(extentPart);
        }
    }

    private void addDetailElement(String type, String text, String caption, Element parentElement) {
        if (text == null) {
            return;
        }
        Element detailElement = new Element("detail", MODS_V3_NS);
        detailElement.setAttribute("type", type);
        Element numberElement = new Element("number", MODS_V3_NS);
        numberElement.setText(text);
        Element captionElement = new Element("caption", MODS_V3_NS);
        captionElement.setText(caption);
        detailElement.addContent(numberElement);
        detailElement.addContent(captionElement);
        parentElement.addContent(detailElement);
    }

}
