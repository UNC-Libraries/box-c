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
package edu.unc.lib.cdr;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static edu.unc.lib.dl.rdf.Ebucore.hasMimeType;
import static edu.unc.lib.dl.rdf.Premis.hasMessageDigest;

import java.io.InputStream;
import java.util.StringJoiner;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

import com.google.common.base.Splitter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.rdf.Fcrepo4Repository;

/**
 * 
 * @author lfarrell
 *
 */
public class BinaryMetadataProcessor implements Processor {
	private final String baseBinaryPath;
	
	protected BinaryMetadataProcessor(String baseBinaryPath) {
		this.baseBinaryPath = baseBinaryPath;
	}

	@Override
	public void process(final Exchange exchange) throws Exception {
		final Message in = exchange.getIn();
		final Model model = createDefaultModel();

		Model values = model.read(in.getBody(InputStream.class),  null, "Turtle");
		ResIterator resources = values.listResourcesWithProperty(RDF.type, Fcrepo4Repository.Binary);
		
		if (resources.hasNext()) {
			Resource resource = resources.next();
			String mimeType = resource.getProperty(hasMimeType).getObject().toString();
			String fcrepoChecksum = resource.getProperty(hasMessageDigest).getObject().toString();
			String[] fcrepoChecksumSplit = fcrepoChecksum.split(":");
			String fcrepoChecksumStart = fcrepoChecksumSplit[2].substring(0, 6);
				
			String binaryPath = "";
			for (String substring : Splitter.fixedLength(2).split(fcrepoChecksumStart)) {
				binaryPath += substring + "/";
			}
				
			String fullPath = new StringJoiner("")
				.add(baseBinaryPath)
				.add(binaryPath)
				.add(fcrepoChecksumSplit[2])
				.toString();

		//	String fcrepoBaseUrl = in.getHeader("org.fcrepo.jms.baseURL").toString().split(",")[0].trim();

		//	in.setHeader("FcrepoBaseUrl", fcrepoBaseUrl);
			in.setHeader("Checksum", fcrepoChecksumSplit[2]);
			in.setHeader("MimeType", mimeType);
			in.setHeader("BinaryPath", fullPath);
		}
	}
}
