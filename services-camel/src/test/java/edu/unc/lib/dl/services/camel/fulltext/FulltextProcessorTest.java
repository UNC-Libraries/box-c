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
package edu.unc.lib.dl.services.camel.fulltext;

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.PcdmUse;
import edu.unc.lib.dl.services.camel.fulltext.FulltextProcessor;

public class FulltextProcessorTest {
    private FulltextProcessor processor;
    private final String slug = "full_text";
    private final String fileName = "full_text.txt";
    private final String testText = "Test text, see if it can be extracted.";
    private int maxRetries = 3;
    private long retryDelay = 10;
    private File file;
    private final static String MIMETYPE = "text/plain";
    private final static String BINARY_URI =
            "http://fedora/content/45/66/76/67/45667667-ed3f-41fc-94cc-7764fc266075/datafs/original_file";

    @Mock
    private BinaryObject binary;
    @Mock
    private FileObject parent;

    @Mock
    private RepositoryObjectLoader repoObjLoader;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Captor
    private ArgumentCaptor<InputStream> inputStreamCaptor;

    @Before
    public void init() throws Exception {
        initMocks(this);
        processor = new FulltextProcessor(repoObjLoader, slug, fileName, maxRetries, retryDelay);
        file = File.createTempFile(fileName, "txt");
        file.deleteOnExit();
        when(exchange.getIn()).thenReturn(message);

        when(repoObjLoader.getBinaryObject(any(PID.class))).thenReturn(binary);

        when(message.getHeader(eq(FCREPO_URI))).thenReturn(BINARY_URI);


        try (BufferedWriter writeFile = new BufferedWriter(new FileWriter(file))) {
            writeFile.write(testText);
        }
        String filePath = file.getAbsolutePath().toString();
        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(filePath);
    }

    @Test
    public void extractFulltextTest() throws Exception {

        when(binary.getParent()).thenReturn(parent);

        processor.process(exchange);

        verify(parent).addDerivative(eq(slug), inputStreamCaptor.capture(),
                eq(fileName), eq(MIMETYPE), eq(PcdmUse.ExtractedText));
        InputStream request = inputStreamCaptor.getValue();
        String extractedText = new BufferedReader(new InputStreamReader(request))
                .lines().collect(Collectors.joining("\n"));

        assertEquals(testText, extractedText);
    }

    @Test
    public void extractFulltextRetryTest() throws Exception {

        when(binary.getParent())
                .thenThrow(new RuntimeException())
                .thenReturn(parent);

        processor.process(exchange);

        verify(parent).addDerivative(eq(slug), inputStreamCaptor.capture(), eq(fileName),
                eq(MIMETYPE), eq(PcdmUse.ExtractedText));
        InputStream request = inputStreamCaptor.getValue();
        String extractedText = new BufferedReader(new InputStreamReader(request))
                .lines().collect(Collectors.joining("\n"));

        // Throws error on first pass and then retries.
        verify(binary, times(2)).getParent();
        assertEquals(testText, extractedText);
    }

    @Test(expected = RuntimeException.class)
    public void extractFulltextRetryFailTest() throws Exception {

        when(binary.getParent()).thenThrow(new RuntimeException());

        try {
            processor.process(exchange);
        } finally {
            verify(binary, times(maxRetries + 1)).getParent();
        }
    }
}
