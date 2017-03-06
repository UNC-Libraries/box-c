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

package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryChecksum;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.rdf.PcdmUse;

/**
 * Extracts fulltext from documents and adds it as a derivative file on existing file object
 * 
 * @author lfarrell
 *
 */
public class FulltextProcessor implements Processor {
	private static final Logger log = LoggerFactory.getLogger(AddDerivativeProcessor.class);
	
	private final Repository repository;
	private final String slug;
	private final String fileSuffix;

	public FulltextProcessor(Repository repository, String slug, String fileSuffix) {
		this.repository = repository;
		this.slug = slug;
		this.fileSuffix = fileSuffix;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		final Message in = exchange.getIn();

		String binaryUri = (String) in.getHeader(FCREPO_URI);
		String binaryChecksum = (String) in.getHeader(CdrBinaryChecksum);
		String binaryMimeType = (String) in.getHeader(CdrBinaryMimeType);
		String binaryPath = (String) in.getHeader(CdrBinaryPath);
		String derivativeFilename = binaryChecksum + fileSuffix;

		String text = extractText(binaryPath);
		File tempFile = writeFile(derivativeFilename, text);
		
		InputStream binaryStream = new FileInputStream(tempFile);
		
		BinaryObject binary = repository.getBinary(PIDs.get(binaryUri));
		FileObject parent = (FileObject) binary.getParent();
		String derivative = tempFile.getName();
		
		parent.addDerivative(slug, binaryStream, derivative, binaryMimeType, PcdmUse.ExtractedText);
		
		log.info("Adding derivative for {} from {}", binaryUri, tempFile);
	}
	
	private File writeFile(String filename, String text) throws FileNotFoundException {
		try {
			File fulltext = File.createTempFile(filename, ".txt");

			BufferedWriter writeFile = new BufferedWriter(new FileWriter(fulltext));
			writeFile.write(text);
			writeFile.close();
			
			return fulltext;
		} catch (IOException e) {
			log.warn("Unable to write out fulltext for {}", filename);
		}
		
		return null;
	}
	
	private String extractText(String filepath) throws IOException, SAXException, TikaException {
		BodyContentHandler handler = new BodyContentHandler();

		AutoDetectParser parser = new AutoDetectParser();
		Metadata metadata = new Metadata();

		try (InputStream stream = new FileInputStream(new File(filepath))) {
			parser.parse(stream, handler, metadata);
			return handler.toString();
		}
	}
}

