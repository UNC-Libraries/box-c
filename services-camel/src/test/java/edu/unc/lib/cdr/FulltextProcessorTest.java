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
import java.io.File;
import java.io.FileWriter;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Test;
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

	@Before
	public void init() throws Exception {
		initMocks(this);
		processor = new FulltextProcessor(this.repository, slug, fileSuffix);
		file = File.createTempFile("testFile", "txt");
		when(exchange.getIn()).thenReturn(message);
		PIDs.setRepository(repository);
		when(repository.getBaseUri()).thenReturn("http://fedora");
	}
	
	@Test
	public void validTest() throws Exception {
		BufferedWriter writeFile = new BufferedWriter(new FileWriter(this.file));
		writeFile.write(testText);
		writeFile.close();
		
		String filePath = this.file.getAbsolutePath().toString();
		
		when(message.getHeader(eq(FCREPO_URI)))
		.thenReturn("http://fedora/test/original_file");
		
		when(message.getHeader(eq(CdrBinaryPath)))
				.thenReturn(filePath);
		
		when(message.getHeader(eq(CdrBinaryMimeType)))
		.thenReturn("plain/text");
		
		binary = mock(BinaryObject.class);
		parent = mock(FileObject.class);

		when(repository.getBinary(any(PID.class))).thenReturn(binary);
		when(binary.getParent()).thenReturn(parent);

		processor.process(exchange);

		verify(message).equals(FileObject.class);

		this.file.deleteOnExit();
	}
}
