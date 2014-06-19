package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

public class BioMedArticleHelper {

	public static int MAX_SUPPL_TITLE_LENGTH = 250;

	private XPath supplementXPath;
	private XPath supplementFileNameXPath;
	private XPath supplementTitleXPath;
	private XPath identifierXPath;
	private XPath affiliationXPath;
	private XPath authorXPath;
	private XPath bibRootXPath;

	public BioMedArticleHelper() {
		try {
			supplementXPath = XPath.newInstance("//suppl");
			supplementFileNameXPath = XPath.newInstance("file/@name");
			supplementTitleXPath = XPath.newInstance("text/p");
			identifierXPath = XPath.newInstance("xrefbib/pubidlist/pubid");
			affiliationXPath = XPath.newInstance("insg/ins");
			authorXPath = XPath.newInstance("aug/au");
			bibRootXPath = XPath.newInstance("/art/fm/bibl");
		} catch (JDOMException e) {
			throw new Error("Error initializing", e);
		}
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
			List<?> preexistingModsNames = result.getRootElement().getChildren("name", MODS_V3_NS);
			if (preexistingModsNames != null) {
				Iterator<?> it = preexistingModsNames.iterator();
				while (it.hasNext()) {
					it.next();
					it.remove();
				}
			}
			modsContent = result.getRootElement();
		}

		Element bibRoot = (Element)this.bibRootXPath.selectSingleNode(articleDocument);

		//Add identifiers
		@SuppressWarnings("unchecked")
		List<Element> elements = this.identifierXPath.selectNodes(bibRoot);
		if (elements != null){
			for (Element identifier: elements){
				String idType = identifier.getAttributeValue("idtype");
				if (idType != null) {
					Element modsIdentifier = new Element("identifier", MODS_V3_NS);
					modsIdentifier.setAttribute("type", idType);
					if (idType.equals("pmpid")){
						modsIdentifier.setAttribute("displayLabel", "PMID");
					}
					modsIdentifier.setText(identifier.getTextTrim());
					modsContent.addContent(modsIdentifier);
				}
			}
		}

		this.addAuthorsAndAffiliations(bibRoot, modsContent);

		// Add in the containing journal
		String source = bibRoot.getChildText("source");
		if (source != null){
			Element sourceElement = new Element("relatedItem", MODS_V3_NS);
			Element titleInfoElement = new Element("titleInfo", MODS_V3_NS);
			Element titleElement = new Element("title", MODS_V3_NS);
			sourceElement.addContent(titleInfoElement);
			titleInfoElement.addContent(titleElement);
			sourceElement.setAttribute("type", "otherFormat");
			sourceElement.setAttribute("displayLabel", "Source");
			titleElement.setText(source);
			modsContent.addContent(sourceElement);

			//Extract the rest of the bibliographic fields
			String issn = bibRoot.getChildText("issn");
			if (issn != null){
				Element element = new Element("identifier", MODS_V3_NS);
				element.setText(issn);
				element.setAttribute("type", "issn");
				sourceElement.addContent(element);
			}

			String pubdate = bibRoot.getChildText("pubdate");
			if (pubdate != null){
				Element element = new Element("originInfo", MODS_V3_NS);
				Element dateIssued = new Element("dateIssued", MODS_V3_NS);
				dateIssued.setText(pubdate);
				element.setContent(dateIssued);
				sourceElement.addContent(element);
			}

			String volume = bibRoot.getChildText("volume");
			String issue = bibRoot.getChildText("issue");
			if (volume != null || issue != null){
				Element element = new Element("part", MODS_V3_NS);
				addDetailElement("volume", volume, "vol.", element);
				addDetailElement("issue", issue, "issue", element);
				sourceElement.addContent(element);
			}

			String fpage = bibRoot.getChildText("fpage");
			String lpage = bibRoot.getChildText("lpage");
			if (fpage != null || lpage != null){
				Element extentPart = new Element("part", MODS_V3_NS);
				Element element = new Element("extent", MODS_V3_NS);
				element.setAttribute("unit", "page");
				if (fpage != null){
					Element pageElement = new Element("start", MODS_V3_NS);
					pageElement.setText(fpage);
					element.addContent(pageElement);
				}
				if (lpage != null){
					Element pageElement = new Element("end", MODS_V3_NS);
					pageElement.setText(lpage);
					element.addContent(pageElement);
				}
				extentPart.addContent(element);
				sourceElement.addContent(extentPart);
			}

		}
		return result;
	}

	public Map<String, String> getFilesLC2SupplementLabels(Document articleDocument) throws JDOMException {
		Map<String, String> result = new HashMap<String, String>();
		//Set titles for supplements
		@SuppressWarnings("unchecked")
		List<Element> elements = this.supplementXPath.selectNodes(articleDocument);
		if (elements != null){
			for (Element supplement: elements){
				String supplementFileName = ((Attribute)this.supplementFileNameXPath.selectSingleNode(supplement)).getValue();
				Element supplementTitleElement = (Element)this.supplementTitleXPath.selectSingleNode(supplement);
				String supplementTitle = null;
				if (supplementTitleElement != null && supplementTitleElement.getValue() != null) {
					supplementTitle = supplementTitleElement.getValue().trim();
					//If the title is too long for the label field, then limit to just the main title
					if (supplementTitle.length() >= MAX_SUPPL_TITLE_LENGTH){
						String shortenedTitle = supplementTitleElement.getChildTextTrim("b");
						if (shortenedTitle != null) {
							supplementTitle = shortenedTitle;
						}
						//If still too long, then truncate.
						if (supplementTitle.length() >= MAX_SUPPL_TITLE_LENGTH){
							supplementTitle = supplementTitle.substring(0, MAX_SUPPL_TITLE_LENGTH);
						}
					}
				}

				if (supplementTitle == null || supplementTitle.trim().length() == 0)
					supplementTitle = supplementFileName;
				result.put(supplementFileName.toLowerCase(), supplementTitle);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private void addAuthorsAndAffiliations(Element bibRoot, Element modsContent) throws JDOMException{
		//Extract affiliations
		List<Element> elements = this.affiliationXPath.selectNodes(bibRoot);
		Map<String,String> affiliationMap = new HashMap<String,String>();
		if (elements != null){
			for (Element element: elements){
				String affiliation = element.getChildTextTrim("p");
				affiliationMap.put(element.getAttributeValue("id"), affiliation);
			}
		}

		//Extract author names, then create name attributes with affiliations
		elements = this.authorXPath.selectNodes(bibRoot);
		if (elements != null){
			for (Element element: elements){
				String surname = element.getChildText("snm");
				String givenName = element.getChildText("fnm");
				String middle = element.getChildText("mi");
				String affiliationID = null;
				List<?> affiliationRefList = element.getChildren("insr");

				Element nameElement = new Element("name", MODS_V3_NS);
				Element namePartElement = new Element("namePart", MODS_V3_NS);

				StringBuilder nameBuilder = new StringBuilder();
				if (surname != null){
					nameBuilder.append(surname);
					if (givenName != null || middle != null)
						nameBuilder.append(", ");
				}
				if (givenName != null)
					nameBuilder.append(givenName);
				if (middle != null)
					nameBuilder.append(' ').append(middle);
				namePartElement.setText(nameBuilder.toString());

				nameElement.addContent(namePartElement);

				//Add in the list of affiliations for each affil reference
				for (Object affiliationObject: affiliationRefList){
					Element affiliationRef = (Element)affiliationObject;
					affiliationID = affiliationRef.getAttributeValue("iid");
					if (affiliationID != null){
						String affiliation = affiliationMap.get(affiliationID);
						Element affiliationElement = new Element("affiliation", MODS_V3_NS);
						affiliationElement.setText(affiliation);
						nameElement.addContent(affiliationElement);
					}
				}

				modsContent.addContent(nameElement);
			}
		}
	}

	private void addDetailElement(String type, String text, String caption, Element parentElement){
		if (text == null)
			return;
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
