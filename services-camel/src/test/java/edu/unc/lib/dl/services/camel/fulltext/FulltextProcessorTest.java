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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

public class FulltextProcessorTest {
    private FulltextProcessor processor;
    private final String originalFileName = "full_text.txt";
    private final String testText = "Test text, see if it can be extracted.";
    private final String derivativeFinalPath = "de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private File originalFile;
    private String derivPath;
    private final static String BINARY_URI =
            "http://fedora/content/45/66/76/67/45667667-ed3f-41fc-94cc-7764fc266075/datafs/original_file";
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

        derivPath = tmpDir.newFolder().getAbsolutePath();
        processor = new FulltextProcessor(derivPath);
        originalFile = tmpDir.newFile(originalFileName);

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(eq(FCREPO_URI))).thenReturn(RESC_ID);

        try (BufferedWriter writeFile = new BufferedWriter(new FileWriter(originalFile))) {
            writeFile.write(testText);
        }

        String filePath = originalFile.getAbsolutePath();

        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(filePath);
    }

    @Test
    public void extractFulltextTest() throws Exception {
        processor.process(exchange);
        File finalPath = new File(derivPath + "/" + derivativeFinalPath + ".txt");
        assertTrue(finalPath.exists());
        assertEquals(testText, FileUtils.readFileToString(finalPath, UTF_8).trim());
    }
}
