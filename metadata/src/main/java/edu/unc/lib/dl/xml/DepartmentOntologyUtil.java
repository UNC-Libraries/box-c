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

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.RDF_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.SKOS_NS;

import java.io.ByteArrayInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderSAX2Factory;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for looking up department name hierarchies using an index constructed
 * from a SKOS ontology
 *
 * @author bbpennel
 * @date Jun 23, 2014
 */
public class DepartmentOntologyUtil implements VocabularyHelper {
    private static final Logger log = LoggerFactory
            .getLogger(DepartmentOntologyUtil.class);

    // Index of terms and labels mapped to authoritative department names pull
    // from the ontology
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

    private XPathExpression<Element> namePath;

    private String vocabularyURI;

    private static final String locationTermPattern =
            "dep(t\\.?|artment(s)?)|school|division|section(s)?|program in|center for|university";

    public DepartmentOntologyUtil() {
        addressPattern = Pattern
                .compile("([^,]+,)+\\s*[a-zA-Z ]*\\d+[a-zA-Z]*\\s*[^\\n]*");
        addressTrailingPattern = Pattern
                .compile("([^,]+,){2,}\\s*([a-zA-Z]+ ?){1,2}\\s*");
        addressSplit = Pattern.compile("(,? *(and *)?(?=" + locationTermPattern
                + ")(?= of)?)", Pattern.CASE_INSENSITIVE);
        trimLeading = Pattern
                .compile("^([.?;:*&^%$#@!\\-]|the |at |and |\\s)+");
        trimTrailing = Pattern.compile("([.?;:*&^%$#@!\\-]|the |at |\\s)+$");
        deptSplitPlural = Pattern.compile("(and the |and (the )?("
                + locationTermPattern + ")( of)?|and )");
        trimSuffix = Pattern.compile("\\s*(" + locationTermPattern
                + ")( of| for| in)?$");
        trimUNC = Pattern.compile("\\b(unc|carolina)\\s+");
        splitSimple = Pattern.compile("(\\s*[:()]\\s*)+");

        try {
            XPathFactory xFactory = XPathFactory.instance();
            namePath = xFactory.compile("mods:name",
                    Filters.element(MODS_V3_NS), null, MODS_V3_NS);
        } catch (Exception e) {
            log.error("Failed to construct xpath", e);
        }
    }

    /**
     * Returns the authoritative department name that best matches the
     * affiliation provided using the ontology. Since there can be multiple
     * hierarchies for a single term, the outer list of the result separates
     * between multiple paths, while the inner list contains the individual
     * steps within the same hierarchy
     */
    @Override
    public List<List<String>> getAuthoritativeForm(final String affiliation) {

        String cleanAffil = cleanLabel(affiliation);

        // First, check to see if the department matches verbatim.
        DepartmentConcept dept = departments.get(cleanAffil);
        if (dept != null) {
            return buildHierarchy(dept);
        }

        AffiliationStyle style = this.determineStyle(cleanAffil);
        switch (style) {
        case notApplicable:
            // log.debug("Affiliation {} was determined to not be applicable",
            // affiliation);
            return null;

        case address:
            // Affiliation is in address format, so split it into components by
            // commas
            List<List<String>> resultDepts = getAddressDepartment(addressSplit
                    .split(cleanAffil));

            if (resultDepts != null) {
                return resultDepts;
            }

            return getAddressDepartment(cleanAffil.split("\\s*,\\s*"));

        case simple:
            // Clean it up and start
            String[] affilParts = splitSimple.split(cleanAffil);
            for (int i = affilParts.length - 1; i >= 0; i--) {
                String affilPart = affilParts[i];

                List<List<String>> result = getDepartment(affilPart);
                if (result != null) {
                    return result;
                }
            }
            break;
        }

        return null;
    }

    /**
     * Returns a list of authoritative terms for the affiliation field in the
     * given document.
     */
    @Override
    public List<List<String>> getAuthoritativeForms(final Element docElement)
            throws JDOMException {

        Set<String> terms = new HashSet<String>();

        List<?> names = docElement.getChildren("name",
                JDOMNamespaceUtil.MODS_V3_NS);
        Element nameEl;
        for (Object nameObj : names) {
            nameEl = (Element) nameObj;
            List<?> affiliations = nameEl.getChildren("affiliation",
                    JDOMNamespaceUtil.MODS_V3_NS);

            for (Object affilObj : affiliations) {
                String affiliation = ((Element) affilObj).getValue();

                if (affiliation != null && affiliation.trim().length() > 0) {
                    terms.add(affiliation);
                }
            }
        }

        List<List<String>> expandedDepts = new ArrayList<List<String>>(
                terms.size());
        for (String affiliation : terms) {
            List<List<String>> results = getAuthoritativeForm(affiliation);
            if (results != null) {
                expandedDepts.addAll(results);
            }
        }

        // Remove any duplication between paths
        collapsePaths(expandedDepts);

        return expandedDepts;
    }

    /**
     * Compares the affiliation values in the given MODS document against the
     * ontology. If a preferred term(s) is found, then it will replace the
     * original. Only the first and last terms in a single hierarchy are kept if
     * there are more than two levels
     *
     * @param modsDoc
     * @return Returns true if the mods document was modified by adding or
     *         changing affiliations
     * @throws JDOMException
     */
    @Override
    public boolean updateDocumentTerms(final Element docElement) throws JDOMException {
        List<?> nameObjs = namePath.evaluate(docElement);
        boolean modified = false;

        for (Object nameObj : nameObjs) {
            Element nameEl = (Element) nameObj;

            List<?> affiliationObjs = nameEl.getChildren("affiliation", MODS_V3_NS);
            if (affiliationObjs.size() == 0) {
                continue;
            }

            // Collect the set of all affiliations for this name so that it can be used to detect duplicates
            Set<String> affiliationSet = new HashSet<>();
            for (Object affiliationObj : affiliationObjs) {
                Element affiliationEl = (Element) affiliationObj;

                affiliationSet.add(affiliationEl.getTextNormalize());
            }

            // Make a snapshot of the list of affiliations so that the original can be modified
            List<?> affilList = new ArrayList<>(affiliationObjs);
            // Get the authoritative department path for each affiliation and overwrite the original
            for (Object affiliationObj : affilList) {
                Element affiliationEl = (Element) affiliationObj;
                String affiliation = affiliationEl.getTextNormalize();

                List<List<String>> departments = getAuthoritativeForm(affiliation);
                if (departments != null && departments.size() > 0) {

                    Element parentEl = affiliationEl.getParentElement();
                    int affilIndex = parentEl.indexOf(affiliationEl);

                    boolean removeOriginal = true;
                    // Add each path that matched the affiliation. There can be multiple if there were multiple parents
                    for (List<String> deptPath : departments) {
                        String baseDept = deptPath.size() > 1 ? deptPath.get(0) : null;
                        String topDept = deptPath.get(deptPath.size() - 1);

                        // No need to remove the original if it is in the set of departments being added
                        if (affiliation.equals(topDept)) {
                            removeOriginal = false;
                        }

                        modified = addAffiliation(baseDept, parentEl, affilIndex, affiliationSet) || modified;
                        modified = addAffiliation(topDept, parentEl, affilIndex, affiliationSet) || modified;
                    }

                    // Remove the old affiliation unless it was already in the vocabulary
                    if (removeOriginal) {
                        parentEl.removeContent(affiliationEl);
                    }

                }
            }
        }

        return modified;
    }

    /**
     * Add the given department to the parent element as an affiliation if it is
     * not already present
     *
     * @param dept
     * @param parentEl
     * @param affilIndex
     * @param affiliationSet
     * @return True if an affiliation was added
     */
    private boolean addAffiliation(final String dept, final Element parentEl,
            final int affilIndex, final Set<String> affiliationSet) {

        // Prevent duplicate departments from being added
        if (dept != null && !affiliationSet.contains(dept)) {
            Element newAffilEl = new Element("affiliation",
                    parentEl.getNamespace());
            newAffilEl.setText(dept);
            // Insert the new element near where the original was
            try {
                parentEl.addContent(affilIndex, newAffilEl);
            } catch (IndexOutOfBoundsException e) {
                parentEl.addContent(newAffilEl);
            }
            affiliationSet.add(dept);

            return true;
        }

        return false;
    }

    private List<List<String>> getAddressDepartment(final String[] addressParts) {
        List<List<String>> allResults = new ArrayList<List<String>>();

        for (int i = 0; i < addressParts.length; i++) {
            String addressPart = addressParts[i];

            List<List<String>> result = getDepartment(addressPart);
            if (result != null) {
                allResults.addAll(result);
            }
        }

        // Deduplicate the path and remove other entries which are subsets are a
        // more exact path
        if (allResults.size() > 0) {
            collapsePaths(allResults);

            return allResults;
        }

        return null;
    }

    /**
     * Attempt to normalize and generate variations on the given affiliation,
     * and return the first matching dept hierarchy
     *
     * @param affiliation
     * @return
     */
    private List<List<String>> getDepartment(final String affiliation) {
        if (affiliation == null || affiliation.length() == 0) {
            return null;
        }

        String affilPart = affiliation;
        affilPart = affilPart.replaceAll("&amp;", "and")
                .replaceAll(" & ", " and ").replace("&", "");

        int index = affilPart.indexOf(UNC_NAME);
        if (index > 0) {
            affilPart = affilPart.substring(0, index);
        }

        // Trim off trailing punctuation and articles
        affilPart = trimTrailing.matcher(
                trimLeading.matcher(affilPart).replaceAll("")).replaceAll("");
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
        affilPart = trimSuffix.matcher(affilPart).replaceAll("")
                .replaceAll("\\s*/\\s*", " and ").trim();
        if (affilPart.endsWith(",")) {
            affilPart = affilPart.substring(0, affilPart.length() - 1);
        }
        dept = departments.get(affilPart);
        if (dept != null) {
            return buildHierarchy(dept);
        }

        // Try to uninvert the name
        int commaIndex = affilPart.indexOf(',');
        if (commaIndex != -1) {
            String uninverted = affilPart.substring(commaIndex + 1).trim()
                    + ' ' + affilPart.substring(0, commaIndex).trim();
            dept = departments.get(uninverted);
            if (dept != null) {
                return buildHierarchy(dept);
            }
        }

        // Check if there are multiple departments in this affiliation
        String[] multipleDepts = deptSplitPlural.split(affiliation);
        if (multipleDepts.length > 1) {
            List<List<String>> allPaths = new ArrayList<List<String>>();
            // Split the departments up, to lookup and add separately
            for (String part : multipleDepts) {
                part = part.trim().replace("departments", "department");
                List<List<String>> result = getDepartment(part);
                if (result != null) {
                    allPaths.addAll(result);
                }
            }
            if (allPaths.size() > 0) {
                return allPaths;
            }
        }

        return null;
    }

    /**
     * Builds a list containing all departments in the hierarchy chain leading
     * up to and including the given department concept
     *
     * @param dept
     * @param hierarchy
     * @return
     */
    private List<List<String>> buildHierarchy(final DepartmentConcept dept) {
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
    private void walkHierarchy(final DepartmentConcept dept,
            final ArrayDeque<String> deptStack, final List<List<String>> deptPaths) {
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
     * Determines what style of affiliation the given text adheres to, or if it
     * should not be processed
     *
     * @param affiliation
     * @return
     */
    private AffiliationStyle determineStyle(final String affiliation) {
        String department = affiliation.trim();
        int indexUNC = department.indexOf(UNC_NAME);
        if (indexUNC != -1) {
            String afterUNC = department.substring(indexUNC);
            // make sure it is UNC chapel hill
            if (afterUNC.trim().length() > 0
                    && !afterUNC.contains("chapel hill")) {
                return AffiliationStyle.notApplicable;
            } else {
                // since it contains the university name, it is most likely an
                // address
                return AffiliationStyle.address;
            }
        }

        if (department.contains("university")) {
            // From another University, skip
            return AffiliationStyle.notApplicable;
        }

        if (addressPattern.matcher(department).matches()
                || addressTrailingPattern.matcher(department).matches()) {
            // If the address is located in chapel hill, it is worth further
            // processing
            if (department.contains("chapel hill")) {
                return AffiliationStyle.address;
            }
            return AffiliationStyle.notApplicable;
        } else {
            return AffiliationStyle.simple;
        }
    }

    /**
     * Parses a SKOS XML vocabulary located at filePath and populates a lookup
     * index labels and alternative labels referencing the authoritative
     * version.
     *
     * @param ontologyURL
     * @throws Exception
     */
    private void parseVocabulary(final byte[] content) throws Exception {
        departments = new HashMap<String, DepartmentConcept>();

        log.debug("Parsing and building Department vocabulary from {}",
                getVocabularyURI());

        SAXBuilder sb = new SAXBuilder(new XMLReaderSAX2Factory(false));
        Document skosDoc = sb.build(new ByteArrayInputStream(content));

        // Extract all of the concepts and store them to an index
        List<?> concepts = skosDoc.getRootElement().getChildren("Concept",
                SKOS_NS);
        Map<String, DepartmentConcept> tempDepts = new HashMap<String, DepartmentConcept>(
                concepts.size());
        for (Object conceptObj : concepts) {
            DepartmentConcept dept = new DepartmentConcept((Element) conceptObj);
            tempDepts.put(cleanLabel(dept.getIdentifier()), dept);
        }

        // Expand out all the alternative labels into an index and resolve
        // references
        for (Iterator<Entry<String, DepartmentConcept>> deptIt = tempDepts
                .entrySet().iterator(); deptIt.hasNext();) {
            Entry<String, DepartmentConcept> deptEntry = deptIt.next();

            DepartmentConcept dept = deptEntry.getValue();

            // Check if this concept should be ignored in favor of a preferred
            // concept
            if (dept.prefLabel != null) {
                if (departments.containsKey(dept.prefLabel)) {
                    // The preferred concept has already been indexed, grab
                    // extra labels from this concept and reindex pref
                    DepartmentConcept prefDept = departments
                            .get(dept.prefLabel);
                    prefDept.merge(dept);
                    addLabels(prefDept);
                } else {
                    // Since the preferred concept isn't indexed yet, just need
                    // to merge labels into it
                    DepartmentConcept prefDept = tempDepts.get(dept.prefLabel);
                    if (prefDept == null) {
                        log.warn(
                                "Preferred label {} referencing a concept which is not present",
                                dept.prefLabel);
                    } else {
                        prefDept.merge(dept);
                    }
                }
                continue;
            }

            String identifier = cleanLabel(dept.identifier);
            if (departments.containsKey(identifier)
                    && dept.identifier
                            .equals(departments.get(identifier).identifier)) {
                log.error(
                        "Illegal state, multiple concepts share the identifier {}, ignoring duplicate",
                        identifier);
            } else {
                departments.put(identifier, dept);
            }

            addLabels(dept);
        }
    }

    /**
     * Deduplicate the given set of paths and remove entries which are subsets
     * of more exact paths
     *
     * @param paths
     */
    public static void collapsePaths(final List<List<String>> paths) {
        Iterator<List<String>> resultsIt = paths.iterator();
        while (resultsIt.hasNext()) {
            List<String> result = resultsIt.next();
            boolean removePath = false;

            for (List<String> otherResult : paths) {
                if (otherResult != result
                        && result.size() <= otherResult.size()) {
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

            if (removePath) {
                resultsIt.remove();
            }
        }
    }

    private static String cleanLabel(String label) {
        return label.toLowerCase().replaceAll("[.']+", "");
    }

    /**
     * Adds all the alternative labels for a department into the index. Logs a
     * warning if more than one department has the same label
     *
     * @param dept
     */
    private void addLabels(final DepartmentConcept dept) {

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

    /**
     * Returns a set of invalid department affiliation names found in the given
     * MODS document
     */
    @Override
    public Set<String> getInvalidTerms(final Element modsRoot) throws JDOMException {
        return getInvalidTerms(modsRoot, false);
    }

    @Override
    public Set<String> getInvalidTermsWithPrefix(final Element modsRoot)
            throws JDOMException {
        return getInvalidTerms(modsRoot, true);
    }

    public Set<String> getInvalidTerms(final Element modsRoot, final boolean includePrefix)
            throws JDOMException {
        List<?> nameObjs = namePath.evaluate(modsRoot);

        Set<String> invalidTerms = new HashSet<String>();

        for (Object nameObj : nameObjs) {
            Element nameEl = (Element) nameObj;

            List<?> affiliationObjs = nameEl.getChildren("affiliation",
                    MODS_V3_NS);
            if (affiliationObjs.size() == 0) {
                continue;
            }

            // Make a snapshot of the list of affiliations so that the original
            // can be modified
            List<?> affilList = new ArrayList<Object>(affiliationObjs);
            // Get the authoritative department path for each affiliation and
            // overwrite the original
            for (Object affiliationObj : affilList) {
                Element affiliationEl = (Element) affiliationObj;
                String affiliation = affiliationEl.getTextNormalize();

                List<List<String>> departments = getAuthoritativeForm(affiliation);
                if (departments == null || departments.size() == 0) {
                    // Affiliation was not found in the ontology, add it to
                    // result set
                    if (includePrefix) {
                        affiliation = getInvalidTermPrefix() + "|"
                                + affiliation;
                    }
                    invalidTerms.add(affiliation);
                }
            }
        }

        return invalidTerms;
    }

    public Map<String, DepartmentConcept> getDepartments() {
        return departments;
    }

    /**
     * Selectors are not currently used for this helper
     */
    @Override
    public void setSelector(final String selector) {
    }

    public XPathExpression<Element> getNamePath() {
        return namePath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.unc.lib.dl.xml.VocabularyHelper#getVocabularyTerms()
     */
    @Override
    public Collection<String> getVocabularyTerms() {
        Set<String> deptNames = new HashSet<>();
        for (DepartmentConcept dept : departments.values()) {
            deptNames.add(dept.getIdentifier());
        }
        return deptNames;
    }

    @Override
    public String getInvalidTermPrefix() {
        return "affiliation";
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.unc.lib.dl.xml.VocabularyHelper#setContent(byte[])
     */
    @Override
    public void setContent(final byte[] content) throws Exception {
        parseVocabulary(content);
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

        public DepartmentConcept(final Element conceptEl)
                throws IllegalArgumentException {
            String deptLabel = conceptEl.getAttributeValue("about", RDF_NS);
            if (deptLabel == null) {
                throw new IllegalArgumentException(
                        "Invalid concept without a rdf:about attribute found");
            }

            this.identifier = deptLabel;

            setBroader(conceptEl.getChildren("broader", SKOS_NS));

            this.prefLabel = conceptEl.getChildText("prefLabel", SKOS_NS);
            if (this.prefLabel != null) {
                this.prefLabel = cleanLabel(this.prefLabel);
            }

            this.otherLabels = new ArrayList<String>();
            addLabelsFromElements(conceptEl.getChildren("altLabel", SKOS_NS));
            addLabelsFromElements(conceptEl.getChildren("hiddenLabel", SKOS_NS));
        }

        public void merge(final DepartmentConcept incoming) {
            if (incoming.broader != null) {
                for (String newBroader : incoming.broader) {
                    String lower = cleanLabel(newBroader);
                    if (!this.broader.contains(lower)) {
                        this.broader.add(lower);
                    }
                }
            }

            if (incoming.otherLabels != null) {
                for (String newLabel : incoming.otherLabels) {
                    String lower = cleanLabel(newLabel);
                    if (!this.otherLabels.contains(lower)) {
                        this.otherLabels.add(lower);
                    }
                }
            }
        }

        public void addLabelsFromElements(final List<?> labelEls) {
            if (labelEls == null) {
                return;
            }

            for (Object labelEl : labelEls) {
                otherLabels.add(cleanLabel(((Element) labelEl)
                        .getTextNormalize()));
            }
        }

        public void setBroader(final List<?> broaderEls) {
            broader = new ArrayList<String>(broaderEls.size());
            for (Object broaderEl : broaderEls) {
                broader.add(((Element) broaderEl).getAttributeValue("resource",
                        RDF_NS).toLowerCase());
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

    /*
     * (non-Javadoc)
     * 
     * @see edu.unc.lib.dl.xml.VocabularyHelper#getVocabularyURI()
     */
    @Override
    public String getVocabularyURI() {
        return vocabularyURI;
    }

    @Override
    public void setVocabularyURI(final String vocabularyURI) {
        this.vocabularyURI = vocabularyURI;
    }

    @Override
    public void setSelectorNamespaces(final Namespace[] namespaces) {
    }

    @Override
    public String getSelector() {
        return "//mods:name/mods:affiliation";
    }

}
