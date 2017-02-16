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

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryMimeType;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.exec.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.rdf.PcdmUse;

/**
 * Adds a derivative file to an existing file object
 * 
 * @author bbpennel
 *
 */
public class AddDerivativeProcessor implements Processor {
	private static final Logger log = LoggerFactory.getLogger(AddDerivativeProcessor.class);

	private final Repository repository;
	private final String slug;
	private final String fileExtension;
	
	public AddDerivativeProcessor(Repository repository, String slug, String fileExtension) {
		this.repository = repository;
		this.slug = slug;
		this.fileExtension = fileExtension;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		Message in = exchange.getIn();
		String binaryUri = (String) in.getHeader(FCREPO_URI);
		String binaryMimeType = (String) in.getHeader(CdrBinaryMimeType); 
		
		final ExecResult result = (ExecResult) in.getBody();
		String derivativePath = new BufferedReader(new InputStreamReader(result.getStdout()))
				.lines().collect(Collectors.joining("\n"));
		
		InputStream binaryStream = new FileInputStream(derivativePath + "." + fileExtension);
		
		BinaryObject binary = repository.getBinary(PIDs.get(binaryUri));
		FileObject parent = (FileObject) binary.getParent();
		parent.addDerivative(slug, binaryStream, derivativePath, binaryMimeType, PcdmUse.ThumbnailImage);

		log.info("Adding derivative for {} from {}", binaryUri, derivativePath);
	}
}
