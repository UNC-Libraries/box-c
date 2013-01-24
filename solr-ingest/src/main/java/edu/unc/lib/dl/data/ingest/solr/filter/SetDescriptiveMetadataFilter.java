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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.util.JDOMQueryUtil;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Filter which sets descriptive metadata information, generally pulled from MODS 
 * 
 * @author bbpennel
 *
 */
public class SetDescriptiveMetadataFilter extends AbstractIndexDocumentFilter {
	private static final Logger log = LoggerFactory.getLogger(SetDescriptiveMetadataFilter.class);

	private Properties languageCodeMap;
	private Set<String> departmentVocab;
	
	private Pattern addressPattern;
	private Pattern addressTrailingPattern;
	private Pattern deptTrimLeading;
	private Pattern deptTrimTrailing;
	private Pattern deptTrimLeadingColon;
	private Pattern deptSplitPlural;
	private Pattern deptSplit;
	private Pattern deptStripUnitPrefix;
	private Pattern deptHasDeptPrefix;
	
	public SetDescriptiveMetadataFilter() {
		addressPattern = Pattern.compile("([^,]+,)+\\s*[a-zA-Z ]*\\d+[a-zA-Z]*\\s*[^\\n]*");
		addressTrailingPattern = Pattern.compile("([^,]+,){2,}\\s*([a-zA-Z]+ ?){1,2}\\s*");
		deptTrimLeading = Pattern.compile("^([.,?;:*&^%$#@!\\-]|[tT]he |at |[aA]nd |\\s)+");
		deptTrimTrailing = Pattern.compile("([.,?;:*&^%$#@!\\-]|[tT]he |at |\\s)+$");
		deptTrimLeadingColon = Pattern.compile("^[^:]*:");
		deptSplitPlural = Pattern.compile("(and the |and (the )?(?=[dD]ep(t\\.?|artment(s)?)|[sS]chool|[dD]ivision|[sS]ection(s)?|[pP]rogram in|[cC]enter for)( of)?|and )");
		deptSplit = Pattern.compile("(and the |and (the )?(?=[dD]ep(t\\.?|artment(s)?)|[sS]chool|[dD]ivision|[sS]ection(s)?|[pP]rogram in|[cC]enter for)(?= of)?)");
		deptStripUnitPrefix = Pattern.compile("^\\s*([^,]+,)?\\s*([dD]ep(t\\.?|artment(s)?)|[sS]chools?|[dD]ivision|[sS]ections?|[pP]rogram in)( of)?\\s*");
		deptHasDeptPrefix =  Pattern.compile("^\\s*[dD]ep(t\\.?|artment(s)?).+");
		
		languageCodeMap = new Properties();
		try {
			languageCodeMap.load(new InputStreamReader(this.getClass().getResourceAsStream(
					"iso639LangMappings.txt")));
			BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(
					"department_vocab.txt")));
			departmentVocab = new HashSet<String>();
			String line;
			while ((line = br.readLine()) != null) {
				departmentVocab.add(line);
			}
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
			try {
				this.extractTitles(mods, idb);
				this.extractNamesAndAffiliations(mods, idb, true);
				this.extractAbstract(mods, idb);
				this.extractLanguages(mods, idb);
				this.extractSubjects(mods, idb);
				this.extractDateCreated(mods, idb);
				this.extractIdentifiers(mods, idb);
				this.extractCitation(mods, idb);
				this.extractKeywords(mods, idb);
				
			} catch (JDOMException e) {
				throw new IndexingException("Failed to extract MODS data", e);
			}
		} else {
			// TODO basic DC mappings
		}
		
		if (dip.getDocument().getTitle() == null) {
			idb.setTitle(dip.getLabel());
		}
		idb.getKeyword().add(dip.getPid().getPid());
	}

	private void extractTitles(Element mods, IndexDocumentBean idb) throws JDOMException {
		List<?> titles = mods.getChildren("titleInfo", JDOMNamespaceUtil.MODS_V3_NS);
		String mainTitle = null;
		List<String> otherTitles = new ArrayList<String>();
		for (Object titleInfoObj : titles) {
			Element titleInfoEl = (Element) titleInfoObj;
			for (Object titleObj : titleInfoEl.getChildren()) {
				Element titleEl = (Element) titleObj;
				if (mainTitle == null && "title".equals(titleEl.getName())) {
					mainTitle = titleEl.getValue();
				}
				otherTitles.add(titleEl.getValue());
			}
		}
		idb.setTitle(mainTitle);
		if (otherTitles.size() > 0)
			idb.setOtherTitle(otherTitles);
	}

	private void extractNamesAndAffiliations(Element mods, IndexDocumentBean idb, boolean splitDepartments)
			throws JDOMException {
		List<?> names = mods.getChildren("name", JDOMNamespaceUtil.MODS_V3_NS);
		List<String> creators = new ArrayList<String>();
		List<String> contributors = new ArrayList<String>();
		Set<String> departments = new HashSet<String>();
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
							if (givenPart != null)
								nameBuilder.append(',').append(' ');
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
							if ("creator".equals(((Element)roleTerm).getValue())){
								isCreator = true;
								break;
							}
							if (isCreator)
								break;
						}
					}
				}
				
				if (isCreator) {
					creators.add(nameValue);
				}
				
				List<?> affiliations = nameEl.getChildren("affiliation", JDOMNamespaceUtil.MODS_V3_NS);

				for (Object affilObj : affiliations) {
					String affiliation = ((Element) affilObj).getValue();
					if (affiliation != null && affiliation.trim().length() > 0) {
						List<String> individualDepartments = this.splitDepartment(affiliation);
						if (individualDepartments != null && individualDepartments.size() > 0)
							departments.addAll(individualDepartments);
					}
				}
			}
		}
		
		if (contributors.size() > 0)
			idb.setContributor(contributors);
		if (creators.size() > 0) {
			idb.setCreator(creators);
			idb.setCreatorSort(creators.get(0));
		}
		if (departments.size() > 0)
			idb.setDepartment(new ArrayList<String>(departments));
	}
	
	private void extractAbstract(Element mods, IndexDocumentBean idb) throws JDOMException {
		String abstractText = mods.getChildText("abstract", JDOMNamespaceUtil.MODS_V3_NS);
		if (abstractText != null)
			idb.setAbstractText(abstractText.trim());
	}
	
	private void extractSubjects(Element mods, IndexDocumentBean idb) {
		List<?> subjectEls = mods.getChildren("subject", JDOMNamespaceUtil.MODS_V3_NS);
		if (subjectEls.size() > 0) {
			List<String> subjects = new ArrayList<String>();
			for (Object subjectObj: subjectEls) {
				List<?> subjectParts = ((Element)subjectObj).getChildren();
				for (Object subjectPart: subjectParts) {
					Element subjectEl = (Element)subjectPart;
					if (subjectEl.getChildren().size() == 0) {
						subjects.add(subjectEl.getValue());
					}
				}
			}
			if (subjects.size() > 0)
				idb.setSubject(subjects);
		}
		
	}
	
	private void extractLanguages(Element mods, IndexDocumentBean idb){
		List<?> languageEls = mods.getChildren("language", JDOMNamespaceUtil.MODS_V3_NS);
		if (languageEls.size() > 0) {
			List<String> languages = new ArrayList<String>();
			String languageTerm = null;
			for (Object languageObj: languageEls) {
				// Our schema only allows for iso639-2b languages at this point.
				languageTerm = ((Element)languageObj).getChildText("languageTerm", JDOMNamespaceUtil.MODS_V3_NS);
				if (languageTerm != null) {
					languageTerm = languageCodeMap.getProperty(languageTerm.trim());
					if (languageTerm != null)
						languages.add(languageTerm);
				}
			}
			if (languages.size() > 0)
				idb.setLanguage(languages);
		}
	}
	
	private void extractDateCreated(Element mods, IndexDocumentBean idb){
		List<?> originInfoEls = mods.getChildren("originInfo", JDOMNamespaceUtil.MODS_V3_NS);
		Date dateCreated = null;
		if (originInfoEls.size() > 0) {
			for (Object originInfoObj: originInfoEls) {
				Element originInfoEl = (Element) originInfoObj;
				dateCreated = JDOMQueryUtil.parseISO6392bDateChild(originInfoEl, "dateCreated", JDOMNamespaceUtil.MODS_V3_NS);
				if (dateCreated == null) {
					dateCreated = JDOMQueryUtil.parseISO6392bDateChild(originInfoEl, "dateIssued", JDOMNamespaceUtil.MODS_V3_NS);
				}
				if (dateCreated == null) {
					dateCreated = JDOMQueryUtil.parseISO6392bDateChild(originInfoEl, "dateCaptured", JDOMNamespaceUtil.MODS_V3_NS);
				}
				if (dateCreated != null) {
					idb.setDateCreated(dateCreated);
					return;
				}
			}
		}
	}
	

	
	private void extractIdentifiers(Element mods, IndexDocumentBean idb){
		List<?> identifierEls = mods.getChildren("identifier", JDOMNamespaceUtil.MODS_V3_NS);
		List<String> identifiers = new ArrayList<String>();
		for (Object identifierObj: identifierEls) {
			StringBuilder identifierBuilder = new StringBuilder();
			Element identifierEl = (Element) identifierObj;
			String idType = identifierEl.getAttributeValue("type");
			if (idType != null) {
				if (idType.equals("uri")) {
					continue;
				}
				identifierBuilder.append(idType);
			}
			String idValue = identifierEl.getValue();
			identifierBuilder.append('|').append(idValue);
			
			identifiers.add(identifierBuilder.toString());
			idb.getKeyword().add(idValue);
		}
		idb.setIdentifier(identifiers);
	}
	
	private void extractKeywords(Element mods, IndexDocumentBean idb) {
		this.addValuesToList(idb.getKeyword(), mods.getChildren("genre", JDOMNamespaceUtil.MODS_V3_NS));
		this.addValuesToList(idb.getKeyword(), mods.getChildren("typeOfResource", JDOMNamespaceUtil.MODS_V3_NS));
		this.addValuesToList(idb.getKeyword(), mods.getChildren("note", JDOMNamespaceUtil.MODS_V3_NS));
		List<?> physicalDescription = mods.getChildren("physicalDescription", JDOMNamespaceUtil.MODS_V3_NS);
		for (Object childObj: physicalDescription) {
			this.addValuesToList(idb.getKeyword(), ((Element)childObj).getChildren("note", JDOMNamespaceUtil.MODS_V3_NS));
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
		if (elements == null)
			return;
		for (Object elementObj: elements) {
			String value = ((Element)elementObj).getValue();
			if (value != null)
				values.add(value);
		}
	}
	
	private void extractCitation(Element mods, IndexDocumentBean idb) {
		Element citationEl = JDOMQueryUtil.getChildByAttribute(mods, "note", JDOMNamespaceUtil.MODS_V3_NS, "type", "citation/reference");
		if (citationEl != null) {
			idb.setCitation(citationEl.getValue().trim());
		}
	}
	
	private final String UNC_NAME = "University of North Carolina";
	
	public boolean departmentInVocabulary(String department) {
		return this.departmentVocab.contains(department);
	}
	
	public List<String> splitDepartment(String department) {
		department = department.trim();
		int indexUNC = department.indexOf(UNC_NAME);
		boolean isUNC = indexUNC != -1;
		
		if (isUNC) {
			String afterUNC = department.substring(indexUNC + UNC_NAME.length());
			if (afterUNC.trim().length() > 0 && !afterUNC.contains("Chapel Hill")){
				// Skip, university is not Chapel Hill
				return null;
			}
			// Strip off UNC
			if (indexUNC == 0)
				department = afterUNC;
			else department = department.substring(0, indexUNC);
			department = deptTrimTrailing.matcher(deptTrimLeading.matcher(department).replaceAll("")).replaceAll("");
		} else {
			if (department.contains("University")){
				// From another University, skip
				return null;
			}
			// Does this look like an address?  If so, and its not a UNC address, toss it
			if (addressPattern.matcher(department).matches() || addressTrailingPattern.matcher(department).matches()){
				return null;
			}
		}
		
		List<String> deptList = new ArrayList<String>();
		
		// Remove extraneous UNC's
		department = deptTrimLeadingColon.matcher(department.replace("UNC", "").replace("Chapel Hill", "").replace("&", "and")).replaceAll("");
		
		String[] departments;
		if (department.startsWith("Departments")) {
			departments = deptSplitPlural.split(department);
		} else {
			departments = deptSplit.split(department);
		}
		for (String dept: departments) {
			String[] deptSegments = dept.split(",");
			String preferredDept = null;
			for (String deptSegment: deptSegments) {
				if (deptSegment.trim().length() > 0) {
					String strippedDept = deptStripUnitPrefix.matcher(deptSegment).replaceAll("");
					// Department actually begins with the prefix "Department", so use this as the preferred value for this entry.
					if (deptHasDeptPrefix.matcher(deptSegment).matches()) {
						preferredDept = strippedDept;
						break;
					} else {
						preferredDept = strippedDept;
					}
				}
			}
			if (preferredDept != null) {
				preferredDept = deptTrimTrailing.matcher(deptTrimLeading.matcher(preferredDept).replaceAll("")).replaceAll("");
				deptList.add(preferredDept.trim());
			}
				
			
		}
		return deptList;
	}
}
