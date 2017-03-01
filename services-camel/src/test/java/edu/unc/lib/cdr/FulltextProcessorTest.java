package edu.unc.lib.cdr;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.Repository;

public class FulltextProcessorTest {
	private FulltextProcessor processor;
	private final String slug = "full_text";
	private final String fileSuffix = "-full_text.txt";
	private final String testText = "Test text, see if it can be extracted.";
	private Repository repository;
	private File file;
	

	@Mock
	private Exchange exchange;
	@Mock
	private Message message;

	@Before
	public void setup() throws Exception {
		initMocks(this);
		processor = new FulltextProcessor(this.repository, slug, fileSuffix);
		file = File.createTempFile("testFile", "txt");
		when(exchange.getIn()).thenReturn(message);
	}
	
	@Test
	public void validTest() throws Exception {
		BufferedWriter writeFile = new BufferedWriter(new FileWriter(this.file));
		writeFile.write(testText);
		writeFile.close();
		
		String output;
		
		BodyContentHandler handler = new BodyContentHandler();

		AutoDetectParser parser = new AutoDetectParser();
		Metadata metadata = new Metadata();

		try (InputStream stream = new FileInputStream(this.file)) {
			parser.parse(stream, handler, metadata);
			output = handler.toString();
		}
		
		this.file.deleteOnExit();
		
		assertEquals(testText + "\n", output);
	}
}
