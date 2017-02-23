package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

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

public class FulltextProcessor implements Processor {
	private static final Logger log = LoggerFactory.getLogger(AddDerivativeProcessor.class);
	
	private final Repository repository;
	private final String slug;
	private final String fileSuffix;

	protected FulltextProcessor(Repository repository, String slug, String fileSuffix) {
		this.repository = repository;
		this.slug = slug;
		this.fileSuffix = fileSuffix;
	}
			
	@Override
	public void process(Exchange exchange) throws Exception {
		final Message in = exchange.getIn();

		String binaryUri = (String) in.getHeader(FCREPO_URI);
		String binaryMimeType = (String) in.getHeader(CdrBinaryMimeType);
		String binaryPath = (String) in.getHeader(CdrBinaryPath);
		String derivativePath = binaryPath + fileSuffix;

		String text = extractText(binaryPath);
		writeFile(derivativePath, text);
		
		InputStream binaryStream = new FileInputStream(derivativePath);
		
		BinaryObject binary = repository.getBinary(PIDs.get(binaryUri));
		FileObject parent = (FileObject) binary.getParent();
		parent.addDerivative(slug, binaryStream, derivativePath, binaryMimeType, PcdmUse.ExtractedText);
		
		log.info("Adding derivative for {} from {}", binaryUri, derivativePath);
	}
	
	private void writeFile(String filepath, String text) throws FileNotFoundException {
		try (PrintWriter out = new PrintWriter(filepath)) {
			out.println(text);
		}
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

