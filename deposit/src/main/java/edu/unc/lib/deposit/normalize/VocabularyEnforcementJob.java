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

import static edu.unc.lib.deposit.work.DepositGraphUtils.walkChildrenDepthFirst;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.invalidAffiliationTerm;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.xml.DepartmentOntologyUtil;

/**
 * Examines the descriptive metadata for this ingest and determines if it is valid according to the given vocabularies.
 * If a preferred authoritative version for a term is found, it will replace the original value.
 *
 * @author bbpennel
 * @date Jun 26, 2014
 */
public class VocabularyEnforcementJob extends AbstractDepositJob {
	private static final Logger log = LoggerFactory.getLogger(VocabularyEnforcementJob.class);

	@Autowired
	private DepartmentOntologyUtil deptUtil;

	private XPath namePath;

	public VocabularyEnforcementJob() {
		initialize();
	}

	public VocabularyEnforcementJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
		initialize();
	}

	private void initialize() {
		try {
			namePath = XPath.newInstance("//mods:name");
			namePath.addNamespace("mods", MODS_V3_NS.getURI());
		} catch (JDOMException e) {
			log.error("Failed to initialize xpath", e);
		}
	}

	@Override
	public void runJob() {
		Model model = getModel();

		// Get the list of all objects being ingested in this job
		List<String> resourcePIDs = new ArrayList<>();
		Bag deposit = model.getBag(getDepositPID().getURI());
		walkChildrenDepthFirst(deposit, resourcePIDs, true);

		SAXBuilder sb = new SAXBuilder(false);

		for (String resourcePID : resourcePIDs) {
			PID pid = new PID(resourcePID);
			File modsFile = new File(getDescriptionDir(), pid.getUUID() + ".xml");

			// Check if the resource has a description
			if (modsFile.exists()) {
				try {
					Document modsDoc = sb.build(modsFile);

					boolean modified = filterAffiliation(modsDoc, pid, model);

					// Update the mods document if it was changed
					if (modified) {
						try (FileOutputStream fos = new FileOutputStream(modsFile)) {
							new XMLOutputter(Format.getPrettyFormat()).output(modsDoc.getDocument(), fos);
						}
					}
				} catch (JDOMException | IOException e) {
					log.error("Failed to parse description file {}", modsFile.getAbsolutePath(), e);
				}
			}
		}
	}

	/**
	 * Compares the affiliation values in the given MODS document against the ontology. If an affiliation term is not
	 * found, a invalid term warning is added. If a preferred term(s) is found, then it will replace the original.
	 *
	 * @param modsDoc
	 * @param pid
	 * @param model
	 * @return Returns true if the mods document was modified
	 * @throws JDOMException
	 */
	private boolean filterAffiliation(Document modsDoc, PID pid, Model model) throws JDOMException {
		List<?> nameObjs = namePath.selectNodes(modsDoc);
		boolean modified = false;

		Set<String> invalidTerms = new HashSet<>();
		Property invalidTerm = model.createProperty(invalidAffiliationTerm.getURI().toString());

		for (Object nameObj : nameObjs) {
			Element nameEl = (Element) nameObj;

			List<?> affiliationObjs = nameEl.getChildren("affiliation", MODS_V3_NS);
			if (affiliationObjs.size() == 0)
				continue;

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

				List<List<String>> departments = deptUtil.getAuthoritativeDepartment(affiliation);
				if (departments != null && departments.size() > 0) {

					Element parentEl = affiliationEl.getParentElement();
					int affilIndex = parentEl.indexOf(affiliationEl);

					boolean removeOriginal = true;
					// Add each path that matched the affiliation. There can be multiple if there were multiple parents
					for (List<String> deptPath : departments) {
						String baseDept = deptPath.size() > 1 ? deptPath.get(0) : null;
						String topDept = deptPath.get(deptPath.size() - 1);

						// No need to remove the original if it is in the set of departments being added
						if (affiliation.equals(topDept))
							removeOriginal = false;

						modified = addAffiliation(baseDept, parentEl, affilIndex, affiliationSet) || modified;
						modified = addAffiliation(topDept, parentEl, affilIndex, affiliationSet) || modified;
					}

					// Remove the old affiliation unless it was already in the vocabulary
					if (removeOriginal)
						parentEl.removeContent(affiliationEl);

				} else {
					// Affiliation was not found in the ontology, make a validation warning
					invalidTerms.add(affiliation);
				}
			}
		}

		// Persist the list of invalid vocab terms out to the model
		if (invalidTerms.size() > 0) {
			Resource resource = model.getResource(pid.getURI());
			for (String invalid : invalidTerms) {
				model.addLiteral(resource, invalidTerm, model.createLiteral(invalid));
			}
		}

		return modified;
	}

	/**
	 * Add the given department to the parent element as an affiliation if it is not already present
	 *
	 * @param dept
	 * @param parentEl
	 * @param affilIndex
	 * @param affiliationSet
	 * @return True if an affiliation was added
	 */
	private boolean addAffiliation(String dept, Element parentEl, int affilIndex,
			Set<String> affiliationSet) {

		// Prevent duplicate departments from being added
		if (dept != null && !affiliationSet.contains(dept)) {
			Element newAffilEl = new Element("affiliation", parentEl.getNamespace());
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

}
