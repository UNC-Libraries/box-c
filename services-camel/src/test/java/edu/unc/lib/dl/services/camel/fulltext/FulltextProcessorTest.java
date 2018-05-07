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
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import com.google.common.io.Files;

import edu.unc.lib.dl.test.TestHelper;

public class FulltextProcessorTest {
    private FulltextProcessor processor;
    private final String originalFileName = "full_text.txt";
    private final String testText = "Test text, see if it can be extracted.";
    private final String derivativeFinalPath = "de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";

    private File originalFile;
    private File finalDerivativeFile;

    private String derivPath;
    private static final String FEDORA_BASE = "http://example.com/rest/";

    private static final String RESC_ID = FEDORA_BASE + "content/de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Before
    public void init() throws Exception {
        initMocks(this);

        TestHelper.setContentBase(FEDORA_BASE);

        derivPath = tmpDir.newFolder().getAbsolutePath();
        processor = new FulltextProcessor(derivPath);


        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(eq(FCREPO_URI))).thenReturn(RESC_ID);

        finalDerivativeFile = new File(derivPath + "/" + derivativeFinalPath + ".txt");
        // Ensure that final path does not carry over between tests.
        finalDerivativeFile.delete();
    }

    @Test
    public void extractFulltextTest() throws Exception {
        originalFile = tmpDir.newFile(originalFileName);
        FileUtils.write(originalFile, testText, "UTF-8");

        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(originalFile.getAbsolutePath());

        processor.process(exchange);
        assertTrue(finalDerivativeFile.exists());
        assertEquals(testText, FileUtils.readFileToString(finalDerivativeFile, UTF_8).trim());
    }

    @Test
    public void extractFromInvalidPdfTest() throws Exception {
        originalFile = tmpDir.newFile("invalid.pdf");
        Files.copy(new File("src/test/resources/datastreams/invalid.pdf"), originalFile);

        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(originalFile.getAbsolutePath());

        processor.process(exchange);
        assertFalse(finalDerivativeFile.exists());
    }
}
