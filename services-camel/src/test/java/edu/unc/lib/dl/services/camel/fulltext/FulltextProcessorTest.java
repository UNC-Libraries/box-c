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
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinarySubPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;

public class FulltextProcessorTest {
    private FulltextProcessor processor;
    private final String fileName = "full_text.txt";
    private final String testText = "Test text, see if it can be extracted.";
    private final String derivativeSubPath = "derivative";
    private File file;
    private final static String BINARY_URI =
            "http://fedora/content/45/66/76/67/45667667-ed3f-41fc-94cc-7764fc266075/datafs/original_file";

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Mock
    private BinaryObject binary;

    @Mock
    private FileObject parent;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Captor
    private ArgumentCaptor<InputStream> inputStreamCaptor;

    @Before
    public void init() throws Exception {
        initMocks(this);

        processor = new FulltextProcessor(tmpDir.newFolder().getAbsolutePath());
        file = tmpDir.newFile(fileName + ".txt");

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(eq(FCREPO_URI))).thenReturn(BINARY_URI);

        try (BufferedWriter writeFile = new BufferedWriter(new FileWriter(file))) {
            writeFile.write(testText);
        }

        String filePath = file.getAbsolutePath().toString();
        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(filePath);
        when(message.getHeader(eq(CdrBinarySubPath)))
                .thenReturn(derivativeSubPath);
    }

    @Test
    public void extractFulltextTest() throws Exception {
        processor.process(exchange);
        assertTrue(file.exists());
        assertEquals(testText, FileUtils.readFileToString(file, UTF_8));
    }
}
