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
package edu.unc.lib.dl.xml;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.RDF_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.SKOS_NS;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for looking up department name hierarchies using an index constructed from a SKOS ontology
 *
 * @author bbpennel
 * @date Jun 23, 2014
 */
public class DepartmentOntologyUtil {
	private static final Logger log = LoggerFactory.getLogger(DepartmentOntologyUtil.class);

	private String ontologyURL;

	// Index of terms and labels mapped to authoritative department names pull from the ontology
	private Map<String, DepartmentConcept> departments;

	private final String UNC_NAME = "university of north carolina";

	private final Pattern addressPattern;
	private final Pattern addressTrailingPattern;
	private final Pattern addressSplit;
	private final Pattern trimLeading;
	private final Pattern trimTrailing;
	private final Pattern deptSplitPlural;
	private final Pattern trimSuffix;
	private final Pattern trimUNC;
	private final Pattern splitSimple;

	public DepartmentOntologyUtil() {
		addressPattern = Pattern.compile("([^,]+,)+\\s*[a-zA-Z ]*\\d+[a-zA-Z]*\\s*[^\\n]*");
		addressTrailingPattern = Pattern.compile("([^,]+,){2,}\\s*([a-zA-Z]+ ?){1,2}\\s*");
		addressSplit = Pattern.compile(
						"(,? *(and *)?(?=dep(t\\.?|artment(s)?)|school|division|section(s)?|program in|center for|university)(?= of)?)",
				Pattern.CASE_INSENSITIVE);
		trimLeading = Pattern.compile("^([.?;:*&^%$#@!\\-]|the |at |and |\\s)+");
		trimTrailing = Pattern.compile("([.?;:*&^%$#@!\\-]|the |at |\\s)+$");
		deptSplitPlural = Pattern
				.compile("(and the |and (the )?(dep(t\\.?|artment(s)?)|school|division|section(s)?|program in|center for)( of)?|and )");
		trimSuffix = Pattern.compile("\\s*(department|doctoral|masters)$");
		trimUNC = Pattern.compile("\\b(unc|carolina)\\s+");
		splitSimple = Pattern.compile("(\\s*[:()]\\s*)+");
	}

	public void init() {
		try {
			parseVocabulary(ontologyURL);
		} catch (Exception e) {
			log.error("Failed to parse department ontology at path {}", e, ontologyURL);
		}
	}

	/**
	 * Returns the authoritative department name that best matches the affiliation provided using the ontology
	 *
	 * @param affiliation
	 * @return
	 */
	public List<List<String>> getAuthoritativeDepartment(String affiliation) {

		String cleanAffil = cleanLabel(affiliation).replaceAll("&amp;", "and").replace("&", "");

		// First, check to see if the department matches verbatim.
		DepartmentConcept dept = departments.get(cleanAffil);
		if (dept != null) {
			return buildHierarchy(dept);
		}

		AffiliationStyle style = this.determineStyle(cleanAffil);
		switch (style) {
			case notApplicable:
				// log.debug("Affiliation {} was determined to not be applicable", affiliation);
				return null;

			case address:
				// Affiliation is in address format, so split it into components by commas
				List<List<String>> resultDepts = getAddressDepartment(addressSplit.split(cleanAffil));

				if (resultDepts != null)
					return resultDepts;

				return getAddressDepartment(cleanAffil.split("\\s*,\\s*"));

			case simple:
				cleanAffil = cleanAffil.replace(",", "");

				// Clean it up and start
				String[] affilParts = splitSimple.split(cleanAffil);
				for (int i = affilParts.length - 1; i >= 0; i--) {
					String affilPart = affilParts[i];

					List<List<String>> result = getDepartment(affilPart);
					if (result != null)
						return result;
				}
				break;
		}

		return null;
	}

	private List<List<String>> getAddressDepartment(String[] addressParts) {
		List<List<String>> allResults = new ArrayList<List<String>>();

		for (int i = 0; i < addressParts.length; i++) {
			String addressPart = addressParts[i];

			List<List<String>> result = getDepartment(addressPart);
			if (result != null) {
				allResults.addAll(result);
			}
		}

		// Deduplicate the path and remove other entries which are subsets are a more exact path
		if (allResults.size() > 0) {
			collapsePaths(allResults);

			return allResults;
		}

		return null;
	}

	/**
	 * Attempt to normalize and generate variations on the given affiliation, and return the first matching dept
	 * hierarchy
	 *
	 * @param affiliation
	 * @return
	 */
	private List<List<String>> getDepartment(String affiliation) {
		if (affiliation == null || affiliation.length() == 0)
			return null;

		String affilPart = affiliation;

		int index = affilPart.indexOf(UNC_NAME);
		if (index > 0) {
			affilPart = affilPart.substring(0, index);
		}

		// Trim off trailing punctuation and articles
		affilPart = trimTrailing.matcher(trimLeading.matcher(affilPart).replaceAll("")).replaceAll("");
		// Expand abbreviation
		affilPart = affilPart.replaceAll("\\bdept\\b", "department");

		// Give it another try
		DepartmentConcept dept = departments.get(affilPart);
		if (dept != null) {
			return buildHierarchy(dept);
		}

		// Attempt without superfluous UNC's
		affilPart = trimUNC.matcher(affilPart).replaceFirst("");
		dept = departments.get(affilPart);
		if (dept != null) {
			return buildHierarchy(dept);
		}

		// Handle inverted departments and slash/and mixups
		affilPart = trimSuffix.matcher(affilPart).replaceAll("").replaceAll("\\s*/\\s*", " and ");
		dept = departments.get(affilPart);
		if (dept != null) {
			return buildHierarchy(dept);
		}

		// Check if there are multiple departments in this affiliation
		String[] multipleDepts = deptSplitPlural.split(affiliation);
		if (multipleDepts.length > 1) {
			List<List<String>> allPaths = new ArrayList<List<String>>();
			// Split the departments up, to lookup and add separately
			for (String part : multipleDepts) {
				part = part.trim().replace("departments", "department");
				List<List<String>> result = getDepartment(part);
				if (result != null)
					allPaths.addAll(result);
			}
			if (allPaths.size() > 0)
				return allPaths;
		}

		return null;
	}

	/**
	 * Builds a list containing all departments in the hierarchy chain leading up to and including the given department
	 * concept
	 *
	 * @param dept
	 * @param hierarchy
	 * @return
	 */
	private List<List<String>> buildHierarchy(DepartmentConcept dept) {
		List<List<String>> hierarchy = new ArrayList<List<String>>();

		walkHierarchy(dept, new ArrayDeque<String>(), hierarchy);

		return hierarchy;
	}

	/**
	 * Constructs a list of all departments in the hierarchy leading up to dept
	 *
	 * @param dept
	 * @param deptStack
	 * @param deptPaths
	 */
	private void walkHierarchy(DepartmentConcept dept, ArrayDeque<String> deptStack, List<List<String>> deptPaths) {
		deptStack.addFirst(dept.identifier);

		if (dept.broader != null && dept.broader.size() > 0) {
			// Seek the first parent department that has a real concept
			DepartmentConcept parentDept = null;
			for (String broader : dept.broader) {
				parentDept = departments.get(broader);
				if (parentDept == null) {
					deptPaths.add(new ArrayList<String>(deptStack));
				} else {
					walkHierarchy(parentDept, deptStack, deptPaths);
				}
			}

		} else {
			deptPaths.add(new ArrayList<String>(deptStack));
		}

		deptStack.removeFirst();
	}

	/**
	 * Determines what style of affiliation the given text adheres to, or if it should not be processed
	 *
	 * @param affiliation
	 * @return
	 */
	private AffiliationStyle determineStyle(String affiliation) {
		String department = affiliation.trim();
		int indexUNC = department.indexOf(UNC_NAME);
		if (indexUNC != -1) {
			String afterUNC = department.substring(indexUNC);
			// make sure it is UNC chapel hill
			if (afterUNC.trim().length() > 0 && !afterUNC.contains("chapel hill")) {
				return AffiliationStyle.notApplicable;
			} else {
				// since it contains the university name, it is most likely an address
				return AffiliationStyle.address;
			}
		}

		if (department.contains("university")) {
			// From another University, skip
			return AffiliationStyle.notApplicable;
		}

		if (addressPattern.matcher(department).matches() || addressTrailingPattern.matcher(department).matches()) {
			// If the address is located in chapel hill, it is worth further processing
			if (department.contains("chapel hill")) {
				return AffiliationStyle.address;
			}
			return AffiliationStyle.notApplicable;
		} else {
			return AffiliationStyle.simple;
		}
	}

	public void setOntologyURL(String ontologyURL) {
		this.ontologyURL = ontologyURL;
	}

	/**
	 * Parses a SKOS XML vocabulary located at filePath and populates a lookup index labels and alternative labels
	 * referencing the authoritative version.
	 *
	 * @param ontologyURL
	 * @throws Exception
	 */
	private void parseVocabulary(String ontologyURL) throws Exception {
		departments = new HashMap<String, DepartmentConcept>();

		SAXBuilder sb = new SAXBuilder(false);
		Document skosDoc = sb.build(new URL(ontologyURL));

		// Extract all of the concepts and store them to an index
		List<?> concepts = skosDoc.getRootElement().getChildren("Concept", SKOS_NS);
		Map<String, DepartmentConcept> tempDepts = new HashMap<String, DepartmentConcept>(concepts.size());
		for (Object conceptObj : concepts) {
			DepartmentConcept dept = new DepartmentConcept((Element) conceptObj);
			tempDepts.put(cleanLabel(dept.getIdentifier()), dept);
		}

		// Expand out all the alternative labels into an index and resolve references
		for (Iterator<Entry<String, DepartmentConcept>> deptIt = tempDepts.entrySet().iterator(); deptIt.hasNext();) {
			Entry<String, DepartmentConcept> deptEntry = deptIt.next();

			DepartmentConcept dept = deptEntry.getValue();

			// Check if this concept should be ignored in favor of a preferred concept
			if (dept.prefLabel != null) {
				if (departments.containsKey(dept.prefLabel)) {
					// The preferred concept has already been indexed, grab extra labels from this concept and reindex pref
					DepartmentConcept prefDept = departments.get(dept.prefLabel);
					prefDept.merge(dept);
					addLabels(prefDept);
				} else {
					// Since the preferred concept isn't indexed yet, just need to merge labels into it
					DepartmentConcept prefDept = tempDepts.get(dept.prefLabel);
					if (prefDept == null) {
						log.warn("Preferred label {} referencing a concept which is not present", dept.prefLabel);
					} else {
						prefDept.merge(dept);
					}
				}
				continue;
			}

			String identifier = cleanLabel(dept.identifier);
			if (departments.containsKey(identifier) && dept.identifier.equals(departments.get(identifier).identifier)) {
				log.error("Illegal state, multiple concepts share the identifier {}, ignoring duplicate", identifier);
			} else {
				departments.put(identifier, dept);
			}

			addLabels(dept);
		}
	}

	/**
	 * Deduplicate the given set of paths and remove entries which are subsets of more exact paths
	 *
	 * @param paths
	 */
	public static void collapsePaths(List<List<String>> paths) {
		Iterator<List<String>> resultsIt = paths.iterator();
		while (resultsIt.hasNext()) {
			List<String> result = resultsIt.next();
			boolean removePath = false;

			for (List<String> otherResult : paths) {
				if (otherResult != result && result.size() <= otherResult.size()) {
					boolean containsPath = true;
					for (String dept : result) {
						if (!otherResult.contains(dept)) {
							containsPath = false;
							break;
						}
					}
					if (containsPath) {
						removePath = true;
						break;
					}
				}
			}

			if (removePath)
				resultsIt.remove();
		}
	}

	private static String cleanLabel(String label) {
		return label.toLowerCase().replaceAll("[.']+", "");
	}

	/**
	 * Adds all the alternative labels for a department into the index. Logs a warning if more than one department has
	 * the same label
	 *
	 * @param dept
	 */
	private void addLabels(DepartmentConcept dept) {

		if (dept.otherLabels != null) {
			for (String label : dept.otherLabels) {
				// Check to see if this label has already been indexed
				if (departments.containsKey(label)) {
					DepartmentConcept collidingDept = departments.get(label);
					if (collidingDept != dept) {
						log.warn("Label collision for key {}", label);
					}
				} else {
					departments.put(label, dept);
				}
			}
		}
	}

	public Map<String, DepartmentConcept> getDepartments() {
		return departments;
	}

	public static enum AffiliationStyle {
		simple, address, notApplicable;
	}

	/**
	 * Stores ontology information for one department concept
	 *
	 * @author bbpennel
	 * @date Jun 30, 2014
	 */
	public static class DepartmentConcept {
		private final String identifier;
		private String prefLabel;
		private List<String> broader;
		private final List<String> otherLabels;

		public DepartmentConcept(Element conceptEl) throws IllegalArgumentException {
			String deptLabel = conceptEl.getAttributeValue("about", RDF_NS);
			if (deptLabel == null) {
				throw new IllegalArgumentException("Invalid concept without a rdf:about attribute found");
			}

			this.identifier = deptLabel;

			setBroader(conceptEl.getChildren("broader", SKOS_NS));

			this.prefLabel = conceptEl.getChildText("prefLabel", SKOS_NS);
			if (this.prefLabel != null)
				this.prefLabel = cleanLabel(this.prefLabel);

			this.otherLabels = new ArrayList<String>();
			addLabelsFromElements(conceptEl.getChildren("altLabel", SKOS_NS));
			addLabelsFromElements(conceptEl.getChildren("hiddenLabel", SKOS_NS));
		}

		public void merge(DepartmentConcept incoming) {
			if (incoming.broader != null) {
				for (String newBroader : incoming.broader) {
					String lower = cleanLabel(newBroader);
					if (!this.broader.contains(lower))
						this.broader.add(lower);
				}
			}

			if (incoming.otherLabels != null) {
				for (String newLabel : incoming.otherLabels) {
					String lower = cleanLabel(newLabel);
					if (!this.otherLabels.contains(lower))
						this.otherLabels.add(lower);
				}
			}
		}

		public void addLabelsFromElements(List<?> labelEls) {
			if (labelEls == null) {
				return;
			}

			for (Object labelEl : labelEls) {
				otherLabels.add(cleanLabel(((Element) labelEl).getTextNormalize()));
			}
		}

		public void setBroader(List<?> broaderEls) {
			broader = new ArrayList<String>(broaderEls.size());
			for (Object broaderEl : broaderEls) {
				broader.add(((Element) broaderEl).getAttributeValue("resource", RDF_NS).toLowerCase());
			}
		}

		public String getIdentifier() {
			return identifier;
		}

		public String getPrefLabel() {
			return prefLabel;
		}

		public List<String> getBroader() {
			return broader;
		}

	}
}
