/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.deposit.validate;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.rdf.CdrDeposit;

/**
 * Verifies that files referenced by this deposit for ingest are present and
 * available
 * 
 * @author daines
 *
 */
public class ValidateFileAvailabilityJob extends AbstractDepositJob {

	@Override
	public void runJob() {

		Set<String> failures = new HashSet<String>();

		Model model = getReadOnlyModel();
		// Construct a map of objects to file paths to verify
		Set<String> hrefs = new HashSet<String>();
		addLocations(hrefs, model, CdrDeposit.stagingLocation);
		addLocations(hrefs, model, CdrDeposit.hasDatastream);

		setTotalClicks(hrefs.size());

		// Verify that the deposit is still running before preceeding with check
		verifyRunning();

		// Iterate through local file hrefs and verify that each one exists
		for (String href : hrefs) {
			URI manifestURI = null;
			try {
				manifestURI = new URI(href);
			} catch (URISyntaxException e) {
				failJob(e, "Unable to parse manifest URI: {0}", href);
			}

			if (manifestURI.getScheme() == null || manifestURI.getScheme().contains("file")) {
				if (!manifestURI.isAbsolute()) {
					manifestURI = getDepositDirectory().toURI().resolve(manifestURI);
				}

				File file = new File(manifestURI.getPath());

				if (!file.exists()) {
					failures.add(manifestURI.toString());
				}
			}

			addClicks(1);
		}

		// Generate failure message of all missing files and fail job
		if (failures.size() > 0) {
			StringBuilder sb = new StringBuilder("Some files referenced by the deposit could not be found:\n");
			for (String uri : failures) {
				sb.append(" - ").append(uri).append("\n");
			}

			if (failures.size() == 1) {
				failJob("1 file referenced by the deposit could not be found.", sb.toString());
			} else {
				failJob(failures.size() + " files referenced by the deposit could not be found.", sb.toString());
			}
		}
	}

	private void addLocations(Set<String> hrefs, Model model, Property property) {
		Property fileLocation = CdrDeposit.stagingLocation;
		StmtIterator i = model.listStatements(new SimpleSelector((Resource) null, fileLocation, (RDFNode) null));
		while (i.hasNext()) {
			Statement s = i.nextStatement();
			String href = s.getObject().asLiteral().getString();
			hrefs.add(href);
		}
	}
}
