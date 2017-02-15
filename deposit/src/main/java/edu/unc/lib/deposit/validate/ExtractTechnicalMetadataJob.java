/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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

import static edu.unc.lib.dl.rdf.CdrDeposit.stagingLocation;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

/**
 * 
 * @author bbpennel
 *
 */
public class ExtractTechnicalMetadataJob extends AbstractDepositJob {
	
	@Autowired
	private CloseableHttpClient httpClient;
	
	private String baseFitsAddress;

	public ExtractTechnicalMetadataJob() {
		// TODO Auto-generated constructor stub
	}

	public ExtractTechnicalMetadataJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void runJob() {
		// TODO Auto-generated method stub
		Model model = getReadOnlyModel();
		
		List<Entry<PID, String>> stagingList = getPropertyPairList(model, stagingLocation);
		
		for (Entry<PID, String> stagedPair : stagingList) {
			PID objPid = stagedPair.getKey();
			String stagedPath = stagedPair.getValue();
			
			// Make request to fits to get back report
		}
	}

	protected List<Entry<PID, String>> getPropertyPairList(Model model, Property property) {
		List<Entry<PID, String>> results = new ArrayList<>();
		
		Selector stageSelector = new SimpleSelector((Resource) null, property, (RDFNode) null);
		StmtIterator i = model.listStatements(stageSelector);
		while (i.hasNext()) {
			Statement s = i.nextStatement();
			PID p = PIDs.get(s.getSubject().getURI());
			String href = s.getObject().asLiteral().getString();
			Entry<PID, String> entry = new SimpleEntry<>(p, href);
			results.add(entry);
		}
		
		return results;
	}
}
