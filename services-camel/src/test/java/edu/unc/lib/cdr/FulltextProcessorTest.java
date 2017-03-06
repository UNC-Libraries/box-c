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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;

public class FulltextProcessorTest {
	private FulltextProcessor processor;
	private final String slug = "full_text";
	private final String fileSuffix = "-full_text.txt";
	private final String testText = "Test text, see if it can be extracted.";
	private File file;
	private BinaryObject binary;
	private FileObject parent;

	@Mock
	private Repository repository;
	
	@Mock
	private Exchange exchange;
	
	@Mock
	private Message message;
	
	@Mock
	private FulltextProcessor fulltext;

	@Before
	public void init() throws Exception {
		initMocks(this);
		processor = new FulltextProcessor(this.repository, slug, fileSuffix);
		file = File.createTempFile("testFile", "txt");
		file.deleteOnExit();
		when(exchange.getIn()).thenReturn(message);
		PIDs.setRepository(repository);
		when(repository.getBaseUri()).thenReturn("http://fedora");
	}
	
	@Test
	public void validTest() throws Exception {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			BufferedWriter writeFile = new BufferedWriter(new FileWriter(this.file));
			writeFile.write(testText);
			writeFile.close();
			
			when(message.getBody(eq(InputStream.class)))
					.thenReturn(new ByteArrayInputStream(bos.toByteArray()));
		}
		
		String filePath = this.file.getAbsolutePath().toString();
		
		when(message.getHeader(eq(FCREPO_URI)))
				.thenReturn("http://fedora/test/original_file");
		
		when(message.getHeader(eq(CdrBinaryChecksum)))
		.thenReturn("1234");
		
		when(message.getHeader(eq(CdrBinaryPath)))
				.thenReturn(filePath);
		
		when(message.getHeader(eq(CdrBinaryMimeType)))
				.thenReturn("plain/text");
		
	//	when(message.get)
		
		binary = mock(BinaryObject.class);
		parent = mock(FileObject.class);

		when(repository.getBinary(any(PID.class))).thenReturn(binary);
		when(binary.getParent()).thenReturn(parent);
		
		

		processor.process(exchange);
		
		ArgumentCaptor<FulltextProcessor> requestCaptor = ArgumentCaptor.forClass(FulltextProcessor.class);
		verify(fulltext).process((Exchange) requestCaptor.capture());
		List<FulltextProcessor> request = requestCaptor.getAllValues();
		
	//	when(message.getHeader(eq(CdrBinaryPath))), eq())).thenReturn(true); 
		
	//	assertEquals("", requestCaptor.getValue().)
//

		verify(message).equals(FileObject.class);
	}
}
